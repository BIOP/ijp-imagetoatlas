package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Undo or Redo",
        description = "Undoes (or redoes) the last user actions of a session, as Ctrl+Z (Ctrl+Shift+Z) does in the ABBA window. "
                + "One step undoes a whole command, a registration of several slices included. The atlas slicing angles are not undone.")
public class UndoCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Steps", description = "Number of actions to undo; negative to redo.")
    int steps = 1;

    @Override
    public void run() {
        mp.waitForTasks();
        for (int i = 0; i < Math.abs(steps); i++) {
            if (steps > 0) mp.cancelLastAction(); else mp.redoAction();
            mp.waitForTasks();
        }
    }
}
