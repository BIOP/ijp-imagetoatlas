package ch.epfl.biop.atlastoimg2d.multislice.commands;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>BigWarp Registration")
public class RegistrationBigWarpCommand extends RegistrationCommand {

    public void run() {
        mp.registerBigWarp(getFixedFilter(), getMovingFilter());
    }

}
