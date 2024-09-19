package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.registration.sourceandconverter.spline.Elastix2DSplineRegistration;
import ch.epfl.biop.scijava.command.source.register.ElastixHelper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Elastix Registration (Spline)",
        description = "Uses Elastix for spline in plane registration of selected slices")
public class RegisterSlicesElastixSplineCommand extends RegistrationMultiChannelCommand {

    @Parameter(label = "Number of control points along X, minimum 2.")
    int nb_control_points_x = 10;

    @Parameter(label = "Registration re-sampling (micrometers)")
    double pixel_size_micrometer = 20;

    @Parameter(label = "Background offset value")
    double background_offset_value_moving = 0;

    double background_offset_value_fixed = 0;

    @Parameter(label = "Show registration results as ImagePlus")
    boolean show_imageplus_registration_result;

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

        ElastixHelper.checkOrSetLocal(this.mp.getContext());

        //mp.registerElastixSpline(getFixedFilter(), getMovingFilter(), nbControlPointsX, showIJ1Result);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("show_image_registration", show_imageplus_registration_result);
        parameters.put("num_ctrl_points_x", nb_control_points_x);
        parameters.put("background_offset_value_moving", background_offset_value_moving);
        parameters.put("background_offset_value_fixed", background_offset_value_fixed);
        parameters.put("px_size_in_current_unit", pixel_size_micrometer/1000.0);

        mp.registerSelectedSlices(Elastix2DSplineRegistration.class,
                getFixedFilter(),
                getMovingFilter(),
                parameters);
    }


}
