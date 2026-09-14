package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>(Obsolete)>ABBA - DeepSlice Registration (Web)",
        description = "Obsolete, use 'ABBA - DeepSlice Registration' instead. Registers the selected slices with the DeepSlice "
                + "web interface: the downsampled slices are sent to the website by hand, and the result file is loaded back. "
                + "Requires a graphical user interface.",
        iconPath = "/graphics/DeepSlice.png")
public class RegisterSlicesDeepSliceWebCommand extends RegisterSlicesDeepSliceAbstractCommand {

    @Parameter(label = "Keep slice order",
            description = "If checked, slices keep their current order: slices swapped by DeepSlice are moved back.")
    boolean maintain_slices_order;

    @Override
    boolean setSettings() {

        maintain_rank = maintain_slices_order;
        if (!ctx.getService(UIService.class).isHeadless()) {
            deepSliceProcessor = (f_in, nSlices) -> DeepSliceHelper.deepSliceWebRunner(f_in, ps); // nSlices is ignored

        } else {
            mp.errorMessageForUser.accept("DeepSlice error", "Can't use Web interface in headless mode, aborting DeepSlice registration");
            return false;
        }
        return true;

    }

}
