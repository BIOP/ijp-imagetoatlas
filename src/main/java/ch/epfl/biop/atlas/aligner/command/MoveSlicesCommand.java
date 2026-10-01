package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Move Slices",
        description = "Moves slices along the slicing axis to the given positions, as dragging them in the ABBA window does. "
                + "Positions are in mm, in the displayed convention ('z_mm' in ABBA - Get State). One undo step. "
                + "Slice indices change after a move: re-read the state.")
public class MoveSlicesCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slices", description = "0-based indices of the slices to move, comma separated (e.g. '0,3'), "
            + "in the current order along the slicing axis.")
    String slices_csv;

    @Parameter(label = "Positions (mm)", description = "New position of each slice along the slicing axis, in mm, "
            + "comma separated, one per slice index (e.g. '6.2,6.6'). Displayed convention, as 'z_mm' in ABBA - Get State.")
    String z_mm_csv;

    @Override
    public void run() {
        mp.waitForTasks();
        List<SliceSources> slices = SliceIndices.parse(mp, slices_csv);
        double[] z = Arrays.stream(z_mm_csv.split(",")).map(String::trim).mapToDouble(Double::parseDouble).toArray();
        if (slices.isEmpty()) throw new IllegalArgumentException("No slice to move: give their indices");
        if (z.length != slices.size()) {
            throw new IllegalArgumentException(slices.size() + " slice(s) but " + z.length + " position(s): give one position per slice");
        }
        new MarkActionSequenceBatchAction(mp).runRequest();
        for (int i = 0; i < z.length; i++) mp.moveSlice(slices.get(i), mp.fromAtlasZ(z[i]));
        new MarkActionSequenceBatchAction(mp).runRequest();
        mp.waitForTasks();
    }
}
