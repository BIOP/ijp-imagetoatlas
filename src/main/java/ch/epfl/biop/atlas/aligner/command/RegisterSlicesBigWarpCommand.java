package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.registration.sourceandconverter.bigwarp.SacBigWarp2DRegistration;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.util.HashMap;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - BigWarp Registration",
        description = "Uses BigWarp for in plane registration of selected slices")
public class RegisterSlicesBigWarpCommand extends RegistrationMultiChannelCommand {

    public void runValidated() {
        mp.registerSelectedSlices(SacBigWarp2DRegistration.class, getFixedFilter(), getMovingFilter(), new HashMap<>());
    }

}
