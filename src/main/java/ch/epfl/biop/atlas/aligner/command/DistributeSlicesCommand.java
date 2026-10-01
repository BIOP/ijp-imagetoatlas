package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Distribute Slices",
        description = "Spaces the selected slices evenly along the slicing axis, as 'Distribute Slices [D]' does in the "
                + "ABBA window. The first and last selected slices and the key slices (ABBA - Set Key Slices) do not move; "
                + "the slices between two of them are spaced evenly. Needs at least 3 selected slices. One undo step.")
public class DistributeSlicesCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Override
    public void run() {
        mp.waitForTasks();
        List<SliceSources> selected = mp.getSelectedSlices();
        if (selected.size() < 3) {
            throw new IllegalArgumentException(selected.size() + " slice(s) selected: select at least 3 (ABBA - Select Slices)");
        }
        mp.equalSpacingSelectedSlices();
        mp.waitForTasks();
    }
}
