package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>ABBA - Save State [Experimental]")
public class MSPStateSaveCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(style = "save")
    File state_file;

    @Override
    public void run() {
        mp.saveState(state_file, true);
    }
}
