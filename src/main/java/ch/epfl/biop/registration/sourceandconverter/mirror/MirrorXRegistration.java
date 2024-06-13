package ch.epfl.biop.registration.sourceandconverter.mirror;

import ch.epfl.biop.atlas.aligner.command.MirrorDoCommand;
import ch.epfl.biop.registration.plugin.IRegistrationPlugin;
import ch.epfl.biop.registration.plugin.RegistrationTypeProperties;
import ch.epfl.biop.registration.sourceandconverter.spline.RealTransformSourceAndConverterRegistration;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = IRegistrationPlugin.class)
@RegistrationTypeProperties(
        isManual = false,
        isEditable = true
)

public class MirrorXRegistration extends RealTransformSourceAndConverterRegistration {

    protected static Logger logger = LoggerFactory.getLogger(MirrorXRegistration.class);

    @Override
    public boolean register() {
        String mirrorSide = parameters.get("mirror_side");
        if (mirrorSide.equals("Left")) {
            rt = new MirrorXTransform(-1);
        } else {
            rt = new MirrorXTransform(1);
        }
        isDone = true;
        return true;
    }

    @Override
    public boolean edit() {
        return false;
    }

    @Override
    public void abort() {

    }

    final public static String MIRROR_X_REGISTRATION_NAME = "Mirror X";
    public String toString() {
        return MIRROR_X_REGISTRATION_NAME;
    }

}