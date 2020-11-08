package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Elastix Registration (Affine)")
public class RegistrationElastixAffineCommand extends RegistrationCommand {

    public void run() {
        mp.registerElastixAffine(getFixedFilter(), getMovingFilter());
    }

}
