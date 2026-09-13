package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.registration.source.affine.ManualAffineRegistration;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import java.util.HashMap;

import static ch.epfl.biop.atlas.aligner.ABBAHelper.getResource;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Manual Affine Registration",
        description = "Moves, rotates, scales and shears selected slices with a gizmo, in a dedicated window",
        iconPath = "/graphics/InteractiveTransform.png")
public class RegisterSlicesManualAffineCommand extends RegistrationMultiChannelCommand {

    public void runValidated() {
        mp.registerSelectedSlices(ManualAffineRegistration.class, getFixedFilter(), getMovingFilter(), new HashMap<>());
    }

    @Override
    protected String getMessage() {
        return "<html>" +
               "    <p><img src='"+getResource("graphics/InteractiveTransform.png")+"' width='80' height='80'></img></p>" +
               "    <p>Selected channels are displayed in the editor window.</p>" +
               "</html>\n";
    }

}
