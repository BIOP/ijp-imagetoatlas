package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>Set Slices Thickness",
        description = "Set the selected slices thickness - useful for a fully reconstructed brain display")
public class EditSliceThicknessCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Slice thickness in micrometer")
    double thicknessInMicrometer;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (slices.size()==0) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice to edit");
        } else {
            for (SliceSources slice : slices) {
                slice.setSliceThickness(thicknessInMicrometer/1000.);
            }
        }
        mp.updateDisplay();
    }
}
