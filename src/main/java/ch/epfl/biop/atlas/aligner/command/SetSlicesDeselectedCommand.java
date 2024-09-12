package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.sourceandconverter.exporter.IntRangeParser;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Deselect Slices",
        description = "Set the slices to deselect.")
public class SetSlicesDeselectedCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Slices to deselect, '*' for all slices, comma separated, 0-based")
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