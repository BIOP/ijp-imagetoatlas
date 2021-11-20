package ch.epfl.biop.atlas.aligner.command;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.TextWidget;

import java.util.HashMap;
import java.util.Map;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Elastix Registration (Spline) on Server",
        description = "Uses an Elastix server for spline in plane registration of selected slices")
public class RegistrationElastixSplineRemoteCommand extends RegistrationSingleChannelCommand {

    @Parameter(label = "Number of control points along X, minimum 2.")
    int nb_control_points_x = 10;

    @Parameter(label = "Registration re-sampling (micrometers)")
    double pxSizeInCurrentUnit = 20;

    @Parameter(label = "Background offset value")
    double background_offset_value_moving = 0;

    double background_offset_value_fixed = 0;

    @Parameter(label = "Registration Server URL")
    String server_url = "https://snappy.epfl.ch";

    @Parameter(visibility = ItemVisibility.MESSAGE, style = TextWidget.AREA_STYLE )
    String user_consent_message =
            "<html>By clicking the checkbox below, you agree <br/>" +
                   "that the downsampled images sent to the <br/>" +
                   "server as well as their positions in the<br/>" +
                   "atlas can be stored and shared in order <br/>" +
                   "to help improve ABBA.<br/>" +
                   "All data remain anonymous.<br/>"+
                   "This will not affect the registration processing time.</html>";

    @Parameter(label = "I agree to share my registration data.")
    boolean user_consent_for_server_keeping_data = false;

    public void runValidated() {
        if (nb_control_points_x <2) {
            mp.errorMessageForUser.accept("Cannot start registration", "Number of control points too low.");
            validationError = true;
            return;
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("background_offset_value_moving", background_offset_value_moving);
        parameters.put("background_offset_value_fixed", background_offset_value_fixed);

        parameters.put("serverURL", server_url);
        parameters.put("taskInfo", "");
        parameters.put("userConsentForServerKeepingData", user_consent_for_server_keeping_data);

        parameters.put("showImagePlusRegistrationResult", false);
        parameters.put("nbControlPointsX", nb_control_points_x);
        parameters.put("pxSizeInCurrentUnit", pxSizeInCurrentUnit/1000.0);

        mp.registerSelectedSlices(this,
                getFixedFilter(),
                getMovingFilter(),
                parameters);
    }

}
