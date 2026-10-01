package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.DeleteSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Delete Slices",
        description = "Removes slices from the session, as the delete action of the ABBA window does. "
                + "No file is touched, and undo restores them. Slide scanner files often hold images that are not sections "
                + "(label, overview, macro): check their names in the state before deleting.")
public class DeleteSlicesCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slices", required = false, description = SliceIndices.DESCRIPTION)
    String slices_csv = "";

    @Override
    public void run() {
        mp.waitForTasks();
        List<SliceSources> slices = SliceIndices.parse(mp, slices_csv);
        if (slices.isEmpty()) throw new IllegalArgumentException("No slice to delete: select slices, or give their indices");
        slices.forEach(slice -> new DeleteSliceAction(mp, slice).runRequest());
        mp.waitForTasks();
    }
}
