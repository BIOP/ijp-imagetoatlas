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
        description = "Saves the current registration state")
public class ABBAStateSaveCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(style = "save", persist = false)
    File state_file;

    @Parameter(type = ItemIO.OUTPUT)
    Boolean success = false;

    @Override
    public void run() {
        // Appends extension
        String extension = FilenameUtils.getExtension(state_file.getAbsolutePath());
        if ((extension==null)||(extension.trim().equals(""))) {
            mp.log.accept("Adding abba extension to state file");
            state_file = new File(state_file.getAbsolutePath()+".abba");
        }

        if (state_file.exists()) {
           mp.errlog.accept("Error, this file already exists!");
        } else {
            success = mp.saveState(state_file, true);
        }
    }
}
