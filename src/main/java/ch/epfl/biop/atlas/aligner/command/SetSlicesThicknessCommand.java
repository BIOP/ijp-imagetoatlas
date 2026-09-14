package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Set Slices Thickness",
        description = "Sets the thickness of the selected slices along the slicing axis. "
                + "It affects the 3D display (reconstructed brain) and the 3D resampled export, not the registrations.")
public class SetSlicesThicknessCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slice thickness (micrometers)", style="format:0.00",
            description = "Thickness of each selected slice, centered on its position.")
    double thickness_um;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (slices.isEmpty()) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice to edit");
        } else {
            for (SliceSources slice : slices) {
                slice.setSliceThickness(thickness_um /1000.);
            }
        }
    }
}
