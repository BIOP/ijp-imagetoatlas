package ch.epfl.biop.atlas.aligner.command;

import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.transforms.BigWarpTransform;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import ch.epfl.biop.registration.source.spline.PrecomputedSplineRegistration;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;
import net.imglib2.realtransform.RealTransform;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.persist.ScijavaGsonHelper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Landmark Registration",
        description = "Adds a thin-plate spline registration to the slices, computed from landmark pairs read from a BigWarp "
                + "landmarks file (.csv), as if they had been placed in 'ABBA - BigWarp Registration', but without opening BigWarp. "
                + "The registration is saved with the state and can be edited in BigWarp with 'ABBA - Edit Last Registration'. One undo step.")
public class RegisterSlicesLandmarksCommand implements Command {

    /** Name of the registrations added, shown in the timeline and saved in the state */
    public static final String REGISTRATION_NAME = "Landmarks";

    /** Fewer landmarks do not define a thin-plate spline: BigWarp then gives a transform which does not follow them */
    static final int MIN_LANDMARKS = 3;

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slices", required = false, description = SliceIndices.DESCRIPTION)
    String slices_csv = "";

    @Parameter(style = "open", label = "Landmarks file (.csv)",
            description = "2D landmarks in BigWarp's csv format, one pair per row: name, active, moving x, moving y, fixed x, fixed y. "
                    + "Moving points are on the slice as it is after its previous registrations, fixed points on the atlas, both in mm, "
                    + "as BigWarp shows them when 'ABBA - BigWarp Registration' opens it: landmarks exported from there can be used as they are. "
                    + "Inactive rows are ignored, at least " + MIN_LANDMARKS + " active pairs are needed.")
    File landmarks_file;

    @Override
    public void run() {
        mp.waitForTasks();
        List<SliceSources> slices = SliceIndices.parse(mp, slices_csv);
        if (slices.isEmpty()) fail("No slice to register", "Please select the slice(s) you want to register, or give their indices");
        LandmarkTableModel landmarks = null;
        try {
            landmarks = readLandmarks(landmarks_file);
        } catch (IllegalArgumentException e) {
            fail("Cannot use the landmarks file", e.getMessage());
        }
        addRegistration(mp, slices, spline(landmarks), REGISTRATION_NAME, new HashMap<>());
        mp.waitForTasks();
    }

    /**
     * @param file 2D landmarks in BigWarp's csv format, read by BigWarp
     * @throws IllegalArgumentException if the file can't be read, is not 2D or has too few active landmarks
     */
    static LandmarkTableModel readLandmarks(File file) {
        if (file == null || !file.isFile()) throw new IllegalArgumentException("File not found: " + file);
        LandmarkTableModel landmarks;
        try {
            landmarks = LandmarkTableModel.loadFromCsv(file, false);
        } catch (Exception e) {
            throw new IllegalArgumentException(file.getName() + ": " + e.getMessage(), e);
        }
        // loadFromCsv gives null when the first row has neither 6 (2D) nor 8 (3D) columns
        if (landmarks == null || landmarks.getNumdims() != 2) {
            throw new IllegalArgumentException(file.getName() + " should have 6 columns per row: "
                    + "name, active, moving x, moving y, fixed x, fixed y (mm). Slices are registered in 2D.");
        }
        if (landmarks.numActive() < MIN_LANDMARKS) {
            throw new IllegalArgumentException(file.getName() + " has " + landmarks.numActive() + " active landmark pair(s), at least "
                    + MIN_LANDMARKS + " are needed.");
        }
        return landmarks;
    }

    /**
     * @return the thin-plate spline BigWarp computes from the active landmarks: the transform of a BigWarp
     * registration, which maps the fixed (atlas) points to the moving (slice) points
     */
    public static RealTransform spline(LandmarkTableModel landmarks) {
        return new BigWarpTransform(landmarks, BigWarpTransform.TPS).getTransformation();
    }

    /**
     * Adds the spline to each slice as a spline registration, in one undo step
     * @param parameters kept with the registration, in the state
     */
    public static void addRegistration(MultiSlicePositioner mp, List<SliceSources> slices, RealTransform transform,
                                       String name, Map<String, Object> parameters) {
        parameters.put(PrecomputedSplineRegistration.TRANSFORM_KEY,
                ScijavaGsonHelper.getGson(mp.getContext()).toJson(transform, RealTransform.class));
        if (slices.size() > 1) new MarkActionSequenceBatchAction(mp).runRequest();
        mp.registerSlices(slices, () -> {
            PrecomputedSplineRegistration registration = new PrecomputedSplineRegistration();
            registration.setRegistrationName(name);
            return registration;
        }, SourcesProcessorHelper.Identity(), SourcesProcessorHelper.Identity(), parameters);
        if (slices.size() > 1) new MarkActionSequenceBatchAction(mp).runRequest();
    }

    /**
     * Shows the error to the user and fails the command, see {@link RegistrationMultiChannelCommand#fail(String, String)}
     */
    protected void fail(String title, String message) {
        mp.errorMessageForUser.accept(title, message);
        throw new IllegalArgumentException(title + ": " + message);
    }
}
