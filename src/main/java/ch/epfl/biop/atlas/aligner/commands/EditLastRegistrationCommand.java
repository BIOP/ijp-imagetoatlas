package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.registration.sourceandconverter.spline.Elastix2DSplineRegistration;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Edit Last Registration")
public class EditLastRegistrationCommand implements Command {

    protected static Logger logger = LoggerFactory.getLogger(EditLastRegistrationCommand.class);

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Edit with all channels")
    boolean editWithAllChannels;

    @Override
    public void run() {
        logger.info("Edit last registration command called.");
        mp.editLastRegistration(editWithAllChannels);
    }
}
