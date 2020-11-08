package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Elastix Registration (Spline)")
public class ElastixSplineRegistrationCommand extends RegistrationCommand {

    public void run() {
        mp.registerElastixSpline(getFixedFilter(), getMovingFilter());
    }

}
