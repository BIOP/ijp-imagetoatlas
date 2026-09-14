package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import ch.epfl.biop.atlas.aligner.LockAndRunOnceSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.SourcesZOffset;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.plugin.IRegistrationPlugin;
import ch.epfl.biop.registration.source.affine.AffineEditor;
import ch.epfl.biop.registration.source.affine.AffineRegistration;
import ch.epfl.biop.source.processor.SourcesProcessor;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static ch.epfl.biop.atlas.aligner.ABBAHelper.getResource;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Manual Affine Registration",
        description = "Manual in-plane affine registration: moves, rotates, scales and shears the selected slices with a gizmo, "
                + "in a single dedicated window. Selected channels are only used for display. The result can be edited later.",
        iconPath = "/graphics/InteractiveTransform.png")
public class RegisterSlicesManualAffineCommand extends RegistrationMultiChannelCommand {

    private static final Logger logger = LoggerFactory.getLogger(RegisterSlicesManualAffineCommand.class);

    /** Name of the registrations added, shown in the timeline and saved in the state */
    public static final String REGISTRATION_NAME = "Manual Affine";

    public void runValidated() {
        registerSlices(mp, mp.getSelectedSlices(), getFixedFilter(), getMovingFilter(),
                pairs -> AffineEditor.edit(pairs, 0, "Manual Affine Registration"));
    }

    /**
     * Adds a new affine registration on top of each slice, edited in a single {@link AffineEditor} window, in a
     * single undo step. Cancelling the edition adds nothing.
     * <p>
     * The edition starts once the action queues of all slices have reached it, and nothing else runs on these slices
     * until it ends. It also waits for the manual lock, so that it does not overlap another manual edition.
     * @param mp the aligner
     * @param slices the slices to register, in slicing axis order
     * @param fixedFilter preprocessing of the atlas sources displayed, the z offset of each slice is added
     * @param movingFilter preprocessing of the slice sources displayed, the z offset of each slice is added
     * @param editor opens the edition on one pair per slice and returns one transform per pair, or null if it was
     *               cancelled. Called from a thread which is not the event dispatch thread
     */
    public static void registerSlices(MultiSlicePositioner mp, List<SliceSources> slices,
                                      SourcesProcessor fixedFilter, SourcesProcessor movingFilter,
                                      Function<List<AffineEditor.Pair>, List<AffineTransform3D>> editor) {
        if (slices.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
            return;
        }

        List<DeepSliceHelper.Holder<Registration<SourceAndConverter<?>[]>>> holders = new ArrayList<>();
        for (int i = 0; i < slices.size(); i++) holders.add(new DeepSliceHelper.Holder<>());

        // Always returns true: a failure or a cancellation leaves the holders empty, and the registrations are skipped
        Supplier<Boolean> editorRunner = () -> {
            synchronized (MultiSlicePositioner.manualActionLock) {
                try {
                    double[] roi = mp.getROI();
                    List<AffineEditor.Pair> pairs = new ArrayList<>();
                    for (SliceSources slice : slices) {
                        // Read now, so that the actions which ran before the edition are taken into account
                        SourceAndConverter<?>[] fixed = SourcesProcessorHelper.compose(new SourcesZOffset(slice), fixedFilter)
                                .apply(mp.getReslicedAtlas().nonExtendedSlicedSources);
                        SourceAndConverter<?>[] moving = SourcesProcessorHelper.compose(new SourcesZOffset(slice), movingFilter)
                                .apply(slice.getRegisteredSources());
                        pairs.add(new AffineEditor.Pair(fixed, moving, new AffineTransform3D(), roi, slice.getName()));
                    }
                    List<AffineTransform3D> transforms = editor.apply(pairs);
                    if (transforms == null) return true;
                    for (int i = 0; i < slices.size(); i++) {
                        holders.get(i).accept(affineRegistration(mp, transforms.get(i), roi));
                    }
                } catch (Exception e) {
                    logger.error("Manual affine registration failed", e);
                    mp.errorMessageForUser.accept("Manual affine registration failed", String.valueOf(e.getMessage()));
                    holders.forEach(holder -> holder.accept(null));
                }
            }
            return true;
        };

        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean result = new AtomicBoolean();
        new MarkActionSequenceBatchAction(mp).runRequest();
        for (int i = 0; i < slices.size(); i++) {
            SliceSources slice = slices.get(i);
            new LockAndRunOnceSliceAction(mp, slice, counter, slices.size(), editorRunner, result).runRequest(true);
            new RegisterSliceAction(mp, slice, holders.get(i),
                    SourcesProcessorHelper.Identity(), SourcesProcessorHelper.Identity(), REGISTRATION_NAME).runRequest(true);
        }
        new MarkActionSequenceBatchAction(mp).runRequest();
    }

    /**
     * @return a done affine registration applying the transform, whose parameters hold the region of interest:
     * editing it later places the gizmo at the same place
     */
    private static IRegistrationPlugin affineRegistration(MultiSlicePositioner mp,
                                                  AffineTransform3D transform, double[] roi) throws Exception {
        IRegistrationPlugin registration = (IRegistrationPlugin)
                mp.getContext().getService(PluginService.class).getPlugin(AffineRegistration.class).createInstance();
        registration.setScijavaContext(mp.getContext());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AffineRegistration.TRANSFORM_KEY, AffineRegistration.affineTransform3DToString(transform));
        parameters.put("px", roi[0]);
        parameters.put("py", roi[1]);
        parameters.put("sx", roi[2]);
        parameters.put("sy", roi[3]);
        parameters.put("pz", 0);
        registration.setRegistrationParameters(MultiSlicePositioner.convertToString(mp.getContext(), parameters));
        registration.register();
        return registration;
    }

    @Override
    protected String getMessage() {
        return "<html>" +
               "    <p><img src='"+getResource("graphics/InteractiveTransform.png")+"' width='80' height='80'></img></p>" +
               "    <p>The selected slices are edited in a single window, one at a time.</p>" +
               "    <p>Selected channels are displayed in the editor window.</p>" +
               "</html>\n";
    }

}
