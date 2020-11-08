package ch.epfl.biop.atlastoimg2d.multislice.commands;

import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>MP Load State [experimental]")
public class MSPStateLoadCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    File stateFile;

    @Override
    public void run() {
        mp.loadState(stateFile);
    }
}
