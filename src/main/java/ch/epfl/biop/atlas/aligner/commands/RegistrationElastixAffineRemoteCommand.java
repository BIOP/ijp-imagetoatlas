package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.TextWidget;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Elastix Registration (Affine) on Server")
public class RegistrationElastixAffineRemoteCommand extends RegistrationCommand {

    @Parameter(label = "Registration Server URL")
    String serverURL;

    @Parameter(visibility = ItemVisibility.MESSAGE, style = TextWidget.AREA_STYLE )
    String userConsentMessage =
            "<html>By clicking the checkbox below, you agree <br/>" +
                    "that the downsampled images sent to the <br/>" +
                    "server as well as their positions within the<br/>" +
                    "atlas can be stored and shared in order <br/>" +
                    "to help improve ABBA.<br/>" +
                    "All data remain anonymous.<br/>"+
                    "This will not affect the registration processing time.</html>";

    @Parameter(label = "I agree to share my registration data.")
    boolean userConsentForServerKeepingData = false;

    public void runValidated() {
        mp.registerElastixAffineRemote(serverURL, getFixedFilter(), getMovingFilter(), userConsentForServerKeepingData);
    }

}
