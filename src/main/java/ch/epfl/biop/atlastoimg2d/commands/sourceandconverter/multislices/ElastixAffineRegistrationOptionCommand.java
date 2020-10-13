package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Elastix Registration (Affine)")
public class ElastixAffineRegistrationOptionCommand extends RegistrationOptionCommand {

    public void start() {
        mp.registerElastixAffine(getFixedFilter(), getMovingFilter());
    }
}
