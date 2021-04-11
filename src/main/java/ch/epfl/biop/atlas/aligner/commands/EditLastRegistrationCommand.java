package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Edit Last Registration")
public class EditLastRegistrationCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Edit with all channels")
    boolean editWithAllChannels;

    @Override
    public void run() {
        System.out.println("Edit last registration");
        mp.editLastRegistration(editWithAllChannels);
    }
}
