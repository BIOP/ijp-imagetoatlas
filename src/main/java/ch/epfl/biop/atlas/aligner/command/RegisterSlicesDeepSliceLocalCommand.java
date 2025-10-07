package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import ch.epfl.biop.wrappers.deepslice.DeepSlice;
import ch.epfl.biop.wrappers.deepslice.DeepSliceTaskSettings;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

/**
 * See {@link RegisterSlicesDeepSliceAbstractCommand}
 * This extra command allows to access (almost) all the options of DeepSlice
 */

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - DeepSlice Registration (Local)",
        description = "Uses Deepslice for affine in plane and axial registration of selected slices",
        iconPath = "/graphics/DeepSlice.png")
public class RegisterSlicesDeepSliceLocalCommand extends RegisterSlicesDeepSliceAbstractCommand {

    @Parameter(description = "Try with and without ensemble to find the model which best works for you",
    label = "Average of several models (slower)")
    boolean ensemble = false;

    final public static String KEEP_ORDER = "Keep order";
    final public static String KEEP_ORDER_REGULAR_SPACING = "Keep order + ensure regular spacing";
    final public static String KEEP_ORDER_SET_SPACING = "Keep order + set spacing (parameter below)";
    final public static String NO_POST_PROCESSING = "No post-processing";

    @Parameter(description = "DeepSlice post-processing", choices = {
            KEEP_ORDER,
            KEEP_ORDER_REGULAR_SPACING,
            KEEP_ORDER_SET_SPACING,
            NO_POST_PROCESSING
    })
    String post_processing;

    @Parameter(style = "format:0.0", label = "Spacing (micrometer), used only when 'Keep order + set spacing' is selected")
    double slices_spacing_micrometer = -1;

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
                settings.enforce_index_spacing = Double.toString(slices_spacing_micrometer);
                break;
            case NO_POST_PROCESSING:
            default:
                maintain_rank = false;
                settings.enforce_index_order = false;
                settings.use_enforce_index_spacing = false;
                settings.enforce_index_spacing = "";
                break;
        }

        if (new File(DeepSlice.envDirPath).exists()) {
            deepSliceProcessor =
                (input_folder, nSlices) -> {
                    boolean pa = settings.propagate_angles; // store
                    if (nSlices<=2) {
                        settings.propagate_angles = false; // see https://github.com/BIOP/ijp-imagetoatlas/issues/214
                    }
                    settings.input_folder = input_folder.getAbsolutePath();
                    File outDirectory =  DeepSliceHelper.deepSliceLocalRunner(settings, input_folder);
                    settings.propagate_angles = pa; // restore
                    return outDirectory;
                };
        } else {
            return false;
        }
        return true;
    }


}
