package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.DebugView;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - Start Logging",
        description = "Opens a debug window which logs the slice events of an ABBA session (creation, deletion, actions).")
public class ABBAStartLogCommand implements Command {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Opens a window which logs the events of the session from now on.";

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Override
    public void run() {
        DebugView dv = new DebugView(mp);
    }
}
