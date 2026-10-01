package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.KeySliceOffAction;
import ch.epfl.biop.atlas.aligner.action.KeySliceOnAction;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Set Key Slices",
        description = "Marks slices as key slices (or unmarks them). Key slices keep their position when "
                + "ABBA - Distribute Slices spaces the others evenly: place 2 or 3 of them where you trust the position. "
                + "One undo step.")
public class SetKeySlicesCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slices", required = false, description = SliceIndices.DESCRIPTION)
    String slices_csv = "";

    @Parameter(label = "Key slice", description = "Checked: mark as key slices. Unchecked: remove the mark.")
    boolean key = true;

    @Override
    public void run() {
        mp.waitForTasks();
        List<SliceSources> slices = SliceIndices.parse(mp, slices_csv);
        if (slices.isEmpty()) throw new IllegalArgumentException("No slice: select slices, or give their indices");
        new MarkActionSequenceBatchAction(mp).runRequest();
        for (SliceSources slice : slices) {
            if (slice.isKeySlice() == key) continue;
            if (key) new KeySliceOnAction(mp, slice).runRequest(); else new KeySliceOffAction(mp, slice).runRequest();
        }
        new MarkActionSequenceBatchAction(mp).runRequest();
        mp.waitForTasks();
    }
}
