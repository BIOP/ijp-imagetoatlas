package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.DebugView;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - Start Logging",
        description = "Close ABBA session")
public class ABBAStartLogCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Override
    public void run() {
        DebugView dv = new DebugView(mp);
    }
}
