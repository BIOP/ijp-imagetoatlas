package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.registration.source.bigwarp.BigWarpSource2DRegistration;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.util.HashMap;

import static ch.epfl.biop.atlas.aligner.ABBAHelper.getResource;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - BigWarp Registration",
        description = "Manual in-plane non-linear registration of the selected slices: each slice is opened in BigWarp, "
                + "one after the other, where landmarks are placed by hand. Selected channels are only used for display.",
        iconPath = "/graphics/ABBABigWarp.png")
public class RegisterSlicesBigWarpCommand extends RegistrationMultiChannelCommand {

    public void runValidated() {
        mp.registerSelectedSlices(BigWarpSource2DRegistration.class, getFixedFilter(), getMovingFilter(), new HashMap<>());
    }

    @Override
    protected String getMessage() {
        return "<html>" +
               "    <p><img src='"+getResource("graphics/ABBABigWarp.png")+"' width='80' height='80'></img></p>" +
               "    <p>Selected channels are displayed in BigWarp.</p>" +
               "</html>\n";
    }

}
