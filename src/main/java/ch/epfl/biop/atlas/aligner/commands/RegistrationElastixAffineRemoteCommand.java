package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Elastix Registration (Affine) on Server")
public class RegistrationElastixAffineRemoteCommand extends RegistrationCommand {

    @Parameter(label = "Registration Server URL")
    String serverURL;

    public void runValidated() {
        mp.registerElastixAffineRemote(serverURL, getFixedFilter(), getMovingFilter());
    }

}
