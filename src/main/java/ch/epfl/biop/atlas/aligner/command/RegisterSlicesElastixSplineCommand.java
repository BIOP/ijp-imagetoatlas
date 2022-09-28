package ch.epfl.biop.atlas.aligner.command;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

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
        if (nb_control_points_x <2) {
            mp.errorMessageForUser.accept("Cannot start registration", "Number of control points too low.");
            validationError = true;
            return;
        }

        //mp.registerElastixSpline(getFixedFilter(), getMovingFilter(), nbControlPointsX, showIJ1Result);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("showImagePlusRegistrationResult", show_imageplus_registration_result);
        parameters.put("nbControlPointsX", nb_control_points_x);
        parameters.put("background_offset_value_moving", background_offset_value_moving);
        parameters.put("background_offset_value_fixed", background_offset_value_fixed);
        parameters.put("pxSizeInCurrentUnit", pixel_size_micrometer/1000.0);

        mp.registerSelectedSlices(this,
                getFixedFilter(),
                getMovingFilter(),
                parameters);
    }


}
