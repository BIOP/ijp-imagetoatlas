package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.apache.commons.io.FilenameUtils;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>ABBA - Save State",
        description = "Saves the current registration state")
public class ABBAStateSaveCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(style = "save", persist = false)
    File state_file;

    @Parameter(persist = false, required = false, type = ItemIO.OUTPUT)
    Boolean success = false;

    @Override
    public void run() {
        // Appends extension
        String extension = FilenameUtils.getExtension(state_file.getAbsolutePath());
        if ((extension==null)||(extension.trim().equals(""))) {
            mp.log.accept("Adding json extension to state file");
            state_file = new File(state_file.getAbsolutePath()+".json");
        }

        if ((state_file.getAbsolutePath().endsWith("_sources.json"))||(state_file.getAbsolutePath().endsWith("_sources"))) {
            mp.errlog.accept("Please choose a different file name.");
            success = false;
        } else {
            if (state_file.exists()) {
               mp.errlog.accept("Error, this file already exists!");
            } else {
                String fileNoExt = FilenameUtils.removeExtension(state_file.getAbsolutePath());
                File sacsFile = new File(fileNoExt+"_sources.json");
                if (sacsFile.exists()) {
                    mp.errlog.accept("Error, the file "+sacsFile.getAbsolutePath()+" already exists!");
                } else {
                    success = mp.saveState(state_file, true);
                }
            }
        }
    }
}
