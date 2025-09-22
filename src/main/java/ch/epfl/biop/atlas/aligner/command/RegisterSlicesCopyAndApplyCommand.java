package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.LockAndRunOnceSliceAction;
import ch.epfl.biop.atlas.aligner.MoveSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.adapter.AlignerState;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.scijava.command.source.register.ElastixHelper;
import com.google.gson.Gson;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.epfl.biop.atlas.aligner.adapter.AlignerState.filterSerializedActions;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Copy and Apply Registration",
        description = "Copy the registration sequence of a slice and apply it to selected slices")
public class RegisterSlicesCopyAndApplyCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Index of the slice registrations you'd like to copy")
    int model_slice_index;

    @Parameter(label = "Tick if you want to skip the pre-transform (probably not)")
    boolean skip_pre_transform = false;

    private Gson serializer;

    public void run() {
        List<SliceSources> selectedSlices = mp.getSelectedSlices();

        if (selectedSlices.size() == 0) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
            return;
        }

        if ((model_slice_index < 0) || (model_slice_index >= mp.getSlices().size())) {
            mp.warningMessageForUser.accept("Invalid model slice index", "Please enter a valid slice index.");
            return;
        }

        SliceSources modelSlice = mp.getSlices().get(model_slice_index);

        if (selectedSlices.contains(modelSlice)) {
            selectedSlices.remove(modelSlice);
            mp.warningMessageForUser.accept("Model slice selected!", "The registration sequence will not be applied to the model slice.");
        }

        // Initialize serializer
        serializer = mp.getGsonStateSerializer(new ArrayList<>());

        // Apply registration to all selected slices
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean result = new AtomicBoolean();

        new LockAndRunOnceSliceAction(mp, modelSlice, counter, 1, () -> {
            // This block will be executed when the model slice has finished all jobs
            for (SliceSources slice : selectedSlices) {
                copyRegistration(mp, modelSlice, mp, slice, skip_pre_transform);
            }
            return true;
        }, result).runRequest();
    }

    private RegisterSliceAction copyReg(RegisterSliceAction action) {
         // Clone through serialisation and deserialisation
        String regString = serializer.toJson(action, RegisterSliceAction.class);
        return serializer.fromJson(regString, RegisterSliceAction.class);
    }

    /**
     * Static utility method for copying registration between slices in other contexts.
     * This version initializes its own serializer and doesn't depend on the command's state.
     *
     * @param mpModel The MultiSlicePositioner instance of the model slice
     * @param modelSlice The slice to copy the registration from
     * @param mpModel The MultiSlicePositioner instance of the target slice
     * @param targetSlice The slice to apply the registration to
     * @param skipPreTransform Whether to skip applying the pre-transform
     */
    public static void copyRegistration(MultiSlicePositioner mpModel, SliceSources modelSlice,
                                        MultiSlicePositioner mpTarget, SliceSources targetSlice, boolean skipPreTransform) {
        Gson localSerializer = mpModel.getGsonStateSerializer(new ArrayList<>());

        // Extract all actions from the model slice
        AlignerState.SliceSourcesState slice_state = new AlignerState.SliceSourcesState();
        slice_state.actions.addAll(filterSerializedActions(mpModel.getActionsFromSlice(modelSlice)));
        slice_state.preTransform = modelSlice.getTransformSourceOrigin();

        // Apply all actions to the target slice
        for (CancelableAction action : slice_state.actions) {
            if (action instanceof RegisterSliceAction) {
                // Clone through serialisation and deserialisation
                String regString = localSerializer.toJson(action, RegisterSliceAction.class);
                RegisterSliceAction regActionModel = localSerializer.fromJson(regString, RegisterSliceAction.class);

                RegisterSliceAction registerSlice = new RegisterSliceAction(
                        mpTarget,
                        targetSlice,
                        regActionModel.getRegistration(),
                        regActionModel.getFixedSourcesProcessor(),
                        regActionModel.getMovingSourcesProcessor()
                );
                registerSlice.setRegistration(registerSlice.getRegistration());
                registerSlice.runRequest();
            }
        }

        // Apply pre-transform if not skipped
        if (!skipPreTransform) {
            targetSlice.transformSourceOrigin(modelSlice.getTransformSourceOrigin().copy());
        }
    }

}
