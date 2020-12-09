package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Elastix Registration (Affine)")
public class RegistrationElastixAffineCommand extends RegistrationCommand {

    @Parameter(label = "Show registration results as ImagePlus")
    boolean showIJ1Result;

    public void runValidated() {
        mp.registerElastixAffine(getFixedFilter(), getMovingFilter(), showIJ1Result);
    }

}
