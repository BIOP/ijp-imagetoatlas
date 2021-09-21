package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - BigWarp Registration",
        description = "Uses BigWarp for in plane registration of selected slices")
public class RegistrationBigWarpCommand extends SingleChannelRegistrationCommand {

    public void runValidated() {
        mp.register(this, getFixedFilter(), getMovingFilter());
    }

}
