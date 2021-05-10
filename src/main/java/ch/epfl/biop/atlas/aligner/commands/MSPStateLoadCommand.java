package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>Load State [Experimental]")
public class MSPStateLoadCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(style = "open")
    File stateFile;

    @Override
    public void run() {
        mp.loadState(stateFile);
    }
}
