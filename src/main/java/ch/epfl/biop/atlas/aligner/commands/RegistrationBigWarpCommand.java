package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>BigWarp Registration")
public class RegistrationBigWarpCommand extends RegistrationCommand {

    public void runValidated() {
        mp.registerBigWarp(getFixedFilter(), getMovingFilter());
    }

}
