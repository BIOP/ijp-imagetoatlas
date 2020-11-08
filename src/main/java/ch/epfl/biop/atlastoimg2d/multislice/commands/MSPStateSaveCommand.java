package ch.epfl.biop.atlastoimg2d.multislice.commands;

import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>MP Save State [Experimental]")
public class MSPStateSaveCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    File stateFile;

    @Parameter
    Boolean overwrite;

    @Override
    public void run() {
        mp.saveState(stateFile, overwrite);
    }
}
