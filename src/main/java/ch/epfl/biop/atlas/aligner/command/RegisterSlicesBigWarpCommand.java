package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.registration.sourceandconverter.bigwarp.SacBigWarp2DRegistration;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.util.HashMap;

import static ch.epfl.biop.atlas.aligner.ABBAHelper.getResource;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - BigWarp Registration",
        description = "Uses BigWarp for in plane registration of selected slices",
        iconPath = "/graphics/ABBABigWarp.png")
public class RegisterSlicesBigWarpCommand extends RegistrationMultiChannelCommand {

    public void runValidated() {
        mp.registerSelectedSlices(SacBigWarp2DRegistration.class, getFixedFilter(), getMovingFilter(), new HashMap<>());
    }

    @Override
    protected String getMessage() {
        return "<html>" +
               "    <p><img src='"+getResource("graphics/ABBABigWarp.png")+"' width='80' height='80'></img></p>" +
               "</html>\n";
    }

}
