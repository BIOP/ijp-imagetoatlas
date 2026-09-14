package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.registration.source.spline.Elastix2DSplineRegistration;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

import static ch.epfl.biop.atlas.aligner.ABBAHelper.getResource;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Elastix Registration (Spline)",
        description = "Automatic in-plane non-linear (B-spline) registration of the selected slices to the atlas with Elastix, "
                + "restricted to the current region of interest. Usually run after an affine registration. "
                + "Atlas and slice channels are paired in order and must be equal in number. The result can be edited in BigWarp.",
        iconPath = "/graphics/ABBASpline.png")
public class RegisterSlicesElastixSplineCommand extends RegistrationMultiChannelCommand {

    @Parameter(label = "Number of control points along X",
            description = "Size of the B-spline control point grid along X (minimum 2), the Y count follows the region of interest aspect ratio. "
                    + "More points allow more local deformations.")
    int nb_control_points_x = 10;

    @Parameter(label = "Registration pixel size (micrometers)",
            description = "Atlas and slices are resampled at this pixel size before being registered. "
                    + "Smaller is more precise but slower; 20 is a good start for a mouse brain.")
    double pixel_size_um = 20;

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
            mp.errorMessageForUser.accept("Number of channel issue", "The number of slice channel(s) should be equal to the number of atlas channel(s).");
            return;
        }

        if (nb_control_points_x <2) {
            mp.errorMessageForUser.accept("Cannot start registration", "Number of control points too low.");
            validationError = true;
            return;
        }

        //mp.registerElastixSpline(getFixedFilter(), getMovingFilter(), nbControlPointsX, showIJ1Result);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("show_image_registration", show_imageplus_registration_result);
        parameters.put("num_ctrl_points_x", nb_control_points_x);
        parameters.put("px_size_in_current_unit", pixel_size_um/1000.0);

        mp.registerSelectedSlices(Elastix2DSplineRegistration.class,
                getFixedFilter(),
                getMovingFilter(),
                parameters);
    }


    @Override
    protected String getMessage() {
        return "<html>" +
                "    <p><img src='"+getResource("graphics/ABBASpline.png")+"' width='80' height='80'></img></p>" +
                "    <p>Atlas and slice channels are paired in order: '0,1' and '1,0' registers<br>" +
                "    atlas channel 0 with slice channel 1, and atlas channel 1 with slice channel 0.</p>" +
                "</html>\n";
    }



}
