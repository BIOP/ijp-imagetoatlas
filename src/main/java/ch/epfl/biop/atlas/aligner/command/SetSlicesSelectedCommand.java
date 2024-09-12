package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.sourceandconverter.exporter.IntRangeParser;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Select Slices",
        description = "Set the slices to select.")
public class SetSlicesSelectedCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Slices to select, '*' for all slices, comma separated, 0-based")
    String slices_csv = "*";

    @Override
    public void run() {
        if (slices_csv.trim().equals("*")) {
            slices_csv ="0:-1";
        }
        try {
            List<Integer> indices = new IntRangeParser(slices_csv).get(mp.getSlices().size());
            for (int index: indices) {
                mp.getSlices().get(index).select();
            }
        } catch (Exception e) {
            mp.errorMessageForUser.accept("Error during parsing of slice indices", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}