package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Elastix Registration (Spline)")
public class RegistrationElastixSplineCommand extends SingleChannelRegistrationCommand {

    @Parameter(label = "Number of control points along X, minimum 2.")
    int nbControlPointsX = 10;

    @Parameter(label = "Background offset value")
    double background_offset_value_moving = 0;

    double background_offset_value_fixed = 0;

    @Parameter(label = "Show registration results as ImagePlus")
    boolean showImagePlusRegistrationResult;

    public void runValidated() {
        if (nbControlPointsX<2) {
            mp.errorMessageForUser.accept("Cannot start registration", "Number of control points too low.");
            validationError = true;
            return;
        }

        //mp.registerElastixSpline(getFixedFilter(), getMovingFilter(), nbControlPointsX, showIJ1Result);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("showImagePlusRegistrationResult", showImagePlusRegistrationResult);
        parameters.put("nbControlPointsX", nbControlPointsX);
        parameters.put("background_offset_value_moving", background_offset_value_moving);
        parameters.put("background_offset_value_fixed", background_offset_value_fixed);

        mp.register(this,
                getFixedFilter(),
                getMovingFilter(),
                parameters);
    }


}
