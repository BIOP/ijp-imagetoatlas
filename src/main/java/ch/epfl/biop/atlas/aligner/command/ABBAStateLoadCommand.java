package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>ABBA - Load State",
        description = "Loads a previous registration state into ABBA")
public class ABBAStateLoadCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(style = "open")
    File state_file;

    @Parameter(type = ItemIO.OUTPUT)
    Boolean success;

    @Override
    public void run() {
        success = mp.loadState(state_file);
        if (!success) mp.errorMessageForUser.accept("Could not load state", "Please check the stack trace");
    }
}
