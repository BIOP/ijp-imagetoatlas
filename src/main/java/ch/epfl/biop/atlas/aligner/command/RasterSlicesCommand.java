package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RasterSliceAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Raster slice",
        description = "Speed up the display of slices by precomputing and caching"+
                      " their pixel.")
public class RasterSlicesCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Pixel size (micrometer)")
    double pixel_size_micrometer = 10;

    @Parameter(label="Interpolate")
    boolean interpolate = false;

    @Override
    public void run() {

        if (pixel_size_micrometer<0) {
            mp.errorMessageForUser.accept("Raster deformation error","Please use a positive value for the pixel size.");
            return;
        }

        // TODO : check if tasks are done
        List<SliceSources> slicesToProcess = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slicesToProcess.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to operate on");
            return;
        }

        new MarkActionSequenceBatchAction(mp).runRequest();
        for (SliceSources slice : slicesToProcess) {
            RasterSliceAction rasterSliceAction = new RasterSliceAction(mp, slice, pixel_size_micrometer, interpolate);
            rasterSliceAction.runRequest();
        }
        new MarkActionSequenceBatchAction(mp).runRequest();

    }

}