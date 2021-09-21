package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.TextWidget;

import java.util.HashMap;
import java.util.Map;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Elastix Registration (Affine) on Server",
        description = "Uses an Elastix server for affine in plane registration of selected slices")
public class RegistrationElastixAffineRemoteCommand extends SingleChannelRegistrationCommand {

    @Parameter(label = "Registration Server URL")
    String server_url = "https://snappy.epfl.ch";

    @Parameter(visibility = ItemVisibility.MESSAGE, style = TextWidget.AREA_STYLE )
    String user_consent_message =
            "<html>By clicking the checkbox below, you agree <br/>" +
                    "that the downsampled images sent to the <br/>" +
                    "server as well as their positions within the<br/>" +
                    "atlas can be stored and shared in order <br/>" +
                    "to help improve ABBA.<br/>" +
                    "All data remain anonymous.<br/>"+
                    "This will not affect the registration processing time.</html>";

    @Parameter(label = "I agree to share my registration data.")
    boolean user_consent_for_server_keeping_data = false;

    @Parameter(label = "Background offset value")
    double background_offset_value_moving = 0;

    double background_offset_value_fixed = 0;

    public void runValidated() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("serverURL", server_url);
        parameters.put("taskInfo", "");
        parameters.put("userConsentForServerKeepingData", user_consent_for_server_keeping_data);
        parameters.put("background_offset_value_moving", background_offset_value_moving);
        parameters.put("background_offset_value_fixed", background_offset_value_fixed);

        mp.register(this,
                getFixedFilter(),
                getMovingFilter(),
                parameters);
    }

}
