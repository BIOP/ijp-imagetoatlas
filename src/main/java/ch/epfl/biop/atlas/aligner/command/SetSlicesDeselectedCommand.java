package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.source.exporter.IntRangeParser;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Deselect Slices",
        description = "Removes slices from the current selection, by index. Other slices keep their selection state.")
public class SetSlicesDeselectedCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slice indices",
            description = "0-based indices of the slices, in the order along the slicing axis. '*' for all slices, "
                    + "comma separated values ('0,3'), inclusive ranges ('2:5'), ranges with a step ('0:2:10'). Negative indices count from the end: -1 is the last slice.")
    String slices_csv = "*";

    @Override
    public void run() {
        if (slices_csv.trim().equals("*")) {
            slices_csv ="0:-1";
        }
        try {
            List<Integer> indices = new IntRangeParser(slices_csv).get(mp.getSlices().size());
            for (int index: indices) {
                mp.getSlices().get(index).deSelect();
            }
        } catch (Exception e) {
            mp.errorMessageForUser.accept("Error during parsing of slice indices", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}