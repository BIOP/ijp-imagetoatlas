package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - DeepSlice Registration (Web)",
        description = "Uses Deepslice for affine in plane and axial registration of selected slices",
        iconPath = "/graphics/DeepSlice.png")
public class RegisterSlicesDeepSliceWebCommand extends RegisterSlicesDeepSliceAbstractCommand {

    @Parameter(label = "Keep slices order")
    boolean maintain_slices_order;

    @Override
    boolean setSettings() {

        maintain_rank = maintain_slices_order;
        if (!ctx.getService(UIService.class).isHeadless()) {
            deepSliceProcessor = (f_in, nSlices) -> DeepSliceHelper.deepSliceWebRunner(f_in, ps); // nSlices is ignored

        } else {
            mp.errorMessageForUser.accept("DeepSlice errot", "Can't use Web interface in headless mode, aborting DeepSlice registration");
            return false;
        }
        return true;

    }

}
