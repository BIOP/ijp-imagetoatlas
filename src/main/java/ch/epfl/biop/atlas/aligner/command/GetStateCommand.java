package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Inspect>ABBA - Get State",
        description = "Returns the state of slices as JSON: the content of a state file, plus, per slice, its index, name, "
                + "channel names, selection, and position along the slicing axis in both conventions ('z_mm' as displayed, "
                + "'slicing_axis_position' as stored). Registrations are listed per slice, long transforms elided.")
public class GetStateCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session to read.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slices", required = false, description = SliceIndices.DESCRIPTION)
    String slices_csv = "*";

    @Parameter(label = "Max string length", description = "Longer strings, like spline transforms, are elided; 0 keeps them all.")
    int max_string_length = 200;

    @Parameter(type = ItemIO.OUTPUT, label = "State", description = "The state, as compact JSON.")
    String state;

    @Override
    public void run() {
        mp.waitForTasks();
        state = mp.serializeSlices(SliceIndices.parse(mp, slices_csv), max_string_length);
    }
}
