package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.apache.commons.io.FilenameUtils;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>ABBA - Save State",
        description = "Saves the current ABBA session (slices, positions, registrations, atlas slicing angles) in a .abba file. "
                + "Images are not copied: the file references the original image files. Existing files are not overwritten.",
        iconPath = "/graphics/SaveState.png")
public class ABBAStateSaveCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(style = "save", persist = false, label = "State file (.abba)",
            description = "File to create. The '.abba' extension is added if there is none. The file must not exist already.")
    File state_file;

    @Parameter(type = ItemIO.OUTPUT, label = "Success",
            description = "True if the state was saved.")
    Boolean success = false;

    @Override
    public void run() {
        // Appends extension
        String extension = FilenameUtils.getExtension(state_file.getAbsolutePath());
        if (extension.trim().isEmpty()) {
            mp.infoMessageForUser.accept("", "Adding abba extension to state file");
            state_file = new File(state_file.getAbsolutePath()+".abba");
        }

        if (state_file.exists()) {
           mp.errorMessageForUser.accept("This file already exists!", "Please choose a different name or location.");
        } else {
            success = mp.saveState(state_file, true);
        }
    }
}
