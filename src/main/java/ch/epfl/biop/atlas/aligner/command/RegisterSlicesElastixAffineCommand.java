package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.registration.source.affine.Elastix2DAffineRegistration;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

import static ch.epfl.biop.atlas.aligner.ABBAHelper.getResource;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Elastix Registration (Affine)",
        description = "Automatic in-plane affine registration (translation, rotation, scaling, shear) of the selected slices "
                + "to the atlas with Elastix, restricted to the current region of interest. "
                + "Atlas and slice channels are paired in order and must be equal in number.",
        iconPath = "/graphics/ABBAAffine.png")
public class RegisterSlicesElastixAffineCommand extends RegistrationMultiChannelCommand {

    @Parameter(label = "Registration pixel size (micrometers)",
            description = "Atlas and slices are resampled at this pixel size before being registered. "
                    + "Smaller is more precise but slower; 40 is a good start for a mouse brain.")
    double pixel_size_um = 40;

    @Parameter(label = "Show registration images",
            description = "If checked, the resampled atlas and slice images sent to Elastix are displayed as ImageJ images (for debugging).")
    boolean show_imageplus_registration_result;

    @Override
    protected boolean requiresConsistentPixelTypes() {
        // Elastix registers a single multichannel ImagePlus built out of the selected channels
        return true;
    }

    public void runValidated() {

        if (atlas_channels.size()!=slice_channels.size()) {
            mp.errorMessageForUser.accept("Number of channels issue", "The number of slice channel(s) should be equal to the number of atlas channel(s).");
            return;
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("show_image_registration", show_imageplus_registration_result);
        parameters.put("px_size_in_current_unit", pixel_size_um/1000.0);

        mp.registerSelectedSlices(Elastix2DAffineRegistration.class,
                getFixedFilter(),
                getMovingFilter(),
                parameters);
    }


    @Override
    protected String getMessage() {
        return "<html>" +
                "    <p><img src='"+getResource("graphics/ABBAAffine.png")+"' width='80' height='80'></img></p>" +
                "    <p>Atlas and slice channels are paired in order: '0,1' and '1,0' registers<br>" +
                "    atlas channel 0 with slice channel 1, and atlas channel 1 with slice channel 0.</p>" +
                "</html>\n";
    }

}
