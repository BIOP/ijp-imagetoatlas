package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import ch.epfl.biop.wrappers.deepslice.DeepSliceTaskSettings;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import java.io.File;

/**
 * See {@link RegisterSlicesDeepSliceAbstractCommand}
 * This extra command allows to access (almost) all the options of DeepSlice
 */

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - DeepSlice Registration",
        description = "Runs DeepSlice (installed automatically in a local Python environment) on the selected slices: "
                + "sets their position along the slicing axis, adds an in-plane affine registration "
                + "and optionally adjusts the atlas slicing angle. The result depends on the slices display settings: avoid saturation.",
        iconPath = "/graphics/DeepSlice.png")
public class RegisterSlicesDeepSliceApposeCommand extends RegisterSlicesDeepSliceAbstractCommand {

    @Parameter(label = "Ensemble (average of several models, slower)",
            description = "If checked, DeepSlice averages the predictions of several models. "
                    + "Results may be better or not depending on the dataset: try both.")
    boolean ensemble = false;

    final public static String KEEP_ORDER = "Keep order";
    final public static String KEEP_ORDER_REGULAR_SPACING = "Keep order + ensure regular spacing";
    final public static String KEEP_ORDER_SET_SPACING = "Keep order + set spacing (parameter below)";
    final public static String NO_POST_PROCESSING = "No post-processing";
    final public static String KEEP_POSITIONS = "Keep current slice positions (in-plane registration only)";

    @Parameter(label = "Position post-processing",
            description = "How the slice positions predicted by DeepSlice are corrected. "
                    + "'" + KEEP_ORDER + "': the current slice order is preserved. "
                    + "'" + KEEP_ORDER_REGULAR_SPACING + "': order preserved and slices evenly spaced, spacing estimated by DeepSlice. "
                    + "'" + KEEP_ORDER_SET_SPACING + "': order preserved and slices evenly spaced with the spacing given below. "
                    + "'" + NO_POST_PROCESSING + "': raw DeepSlice positions, slices may be reordered. "
                    + "'" + KEEP_POSITIONS + "': the slices are not moved, only registered in plane.",
            choices = {
            KEEP_ORDER,
            KEEP_ORDER_REGULAR_SPACING,
            KEEP_ORDER_SET_SPACING,
            NO_POST_PROCESSING,
            KEEP_POSITIONS
    })
    String post_processing;

    @Parameter(style = "format:0.0", label = "Slice spacing (micrometers)",
            description = "Distance between consecutive slices. Only used with '" + KEEP_ORDER_SET_SPACING + "'.")
    double slice_spacing_um = -1;

    @Override
    boolean setSettings() {
        DeepSliceTaskSettings settings = new DeepSliceTaskSettings();
        settings.model = model;
        settings.output_folder = null;
        settings.propagate_angles = true;
        settings.section_numbers = true;
        settings.ensemble = ensemble;

        switch (post_processing) {
            case KEEP_ORDER:
                maintain_rank = true;
                settings.enforce_index_order = true;

                settings.use_enforce_index_spacing = false;
                settings.enforce_index_spacing = "";
                break;
            case KEEP_ORDER_REGULAR_SPACING:
                maintain_rank = true;
                settings.enforce_index_order = true;

                settings.use_enforce_index_spacing = true;
                settings.enforce_index_spacing = "None";
                break;
            case KEEP_ORDER_SET_SPACING:
                maintain_rank = true;
                settings.enforce_index_order = true;

                settings.use_enforce_index_spacing = true;
                settings.enforce_index_spacing = Double.toString(slice_spacing_um);
                break;
            case KEEP_POSITIONS:
                allow_change_slicing_position = false;
                settings.enforce_index_order = true;

                settings.use_enforce_index_spacing = false;
                settings.enforce_index_spacing = "";
                break;
            case NO_POST_PROCESSING:
            default:
                maintain_rank = false;
                settings.enforce_index_order = false;
                settings.use_enforce_index_spacing = false;
                settings.enforce_index_spacing = "";
                break;
        }
            deepSliceProcessor =
                (input_folder, nSlices) -> {
                    boolean pa = settings.propagate_angles; // store
                    if (nSlices<=2) {
                        settings.propagate_angles = false; // see https://github.com/BIOP/ijp-imagetoatlas/issues/214
                    }
                    settings.input_folder = input_folder.getAbsolutePath();

                    UIService uiService = ctx.getService(UIService.class);
                    boolean headless = (uiService == null) || uiService.isHeadless();


                    File outDirectory =
                            DeepSliceHelper.deepSliceLocalApposeRunner(settings,
                                    input_folder,
                                    (message) -> mp.infoMessageForUser.accept("Appose DeepSlice Environment Installation...", message),
                                    (message) -> mp.infoMessageForUser.accept("Appose DeepSlice Run", message),
                                    headless);
                    settings.propagate_angles = pa; // restore
                    return outDirectory;
                };
        return true;
    }


}
