package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>ABBA - Load State",
        description = "Loads a saved ABBA state (.abba file): slices, their positions and registrations, and the atlas slicing angles. "
                + "The slices are added to the current session. The image files must still be at the location where they were when the state was saved.",
        iconPath = "/graphics/LoadState.png")
public class ABBAStateLoadCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(style = "open", label = "State file (.abba)",
            description = "ABBA state file to load. It must have been saved with the same atlas.")
    File state_file;

    @Parameter(type = ItemIO.OUTPUT, label = "Success",
            description = "True if the state was loaded.")
    Boolean success;

    @Override
    public void run() {
        success = mp.loadState(state_file);
        if (!success) mp.errorMessageForUser.accept("Could not load state", "Please check the stack trace");
    }
}
