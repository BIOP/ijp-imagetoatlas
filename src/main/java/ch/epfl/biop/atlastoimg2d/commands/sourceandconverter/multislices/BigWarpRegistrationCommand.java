package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>BigWarp Registration")
public class BigWarpRegistrationCommand extends RegistrationCommand {

    public void run() {
        mp.registerBigWarp(getFixedFilter(), getMovingFilter());
    }

}
