package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>BigWarp Registration")
public class BigWarpRegistrationOptionCommand extends RegistrationOptionCommand {

    public void start() {
        mp.registerBigWarp(getFixedFilter(), getMovingFilter());
    }

}
