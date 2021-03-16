package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.TextWidget;

import java.util.HashMap;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Elastix Registration (Spline) on Server")
public class RegistrationElastixSplineRemoteCommand extends RegistrationCommand {

    @Parameter(label = "Number of control points along X, minimum 2.")
    int nbControlPointsX = 10;

    @Parameter(label = "Registration Server URL")
    String serverURL = "https://snappy.epfl.ch";

    @Parameter(visibility = ItemVisibility.MESSAGE, style = TextWidget.AREA_STYLE )
    String userConsentMessage =
            "<html>By clicking the checkbox below, you agree <br/>" +
                   "that the downsampled images sent to the <br/>" +
                   "server as well as their positions in the<br/>" +
                   "atlas can be stored and shared in order <br/>" +
                   "to help improve ABBA.<br/>" +
                   "All data remain anonymous.<br/>"+
                   "This will not affect the registration processing time.</html>";

    @Parameter(label = "I agree to share my registration data.")
    boolean userConsentForServerKeepingData = false;

    public void runValidated() {
        if (nbControlPointsX<2) {
            mp.errorMessageForUser.accept("Cannot start registration", "Number of control points too low.");
            validationError = true;
            return;
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("serverURL", serverURL);
        parameters.put("taskInfo", "");
        parameters.put("userConsentForServerKeepingData", userConsentForServerKeepingData);

        parameters.put("showImagePlusRegistrationResult", false);
        parameters.put("nbControlPointsX", nbControlPointsX);

        mp.register(this,
                getFixedFilter(),
                getMovingFilter(),
                parameters);
    }

}
