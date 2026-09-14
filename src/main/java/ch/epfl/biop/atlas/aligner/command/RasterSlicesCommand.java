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
        description = "Speeds up the display of the selected slices by computing and caching their registered image "+
                      "at a fixed pixel size. The cached image has a lower resolution than the original image. Experimental.")
public class RasterSlicesCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label="Pixel size (micrometers)",
            description = "Pixel size of the cached images: larger is faster and uses less memory, but is blurrier.")
    double pixel_size_um = 10;

    @Parameter(label="Interpolate",
            description = "If checked, pixels are linearly interpolated when resampled; otherwise the nearest pixel is used.")
    boolean interpolate = false;

    @Override
    public void run() {

        if (pixel_size_um<0) {
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
            RasterSliceAction rasterSliceAction = new RasterSliceAction(mp, slice, pixel_size_um, interpolate);
            rasterSliceAction.runRequest();
        }
        new MarkActionSequenceBatchAction(mp).runRequest();

    }

}