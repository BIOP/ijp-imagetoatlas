package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.action.ExportDeformationFieldToImagePlusAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ij.ImagePlus;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Atlas Coordinates of Original Slices to ImageJ",
        description =
                "For each selected slice, exports a 3-channel (x, y, z) 32-bit ImageJ image with the same pixels as the " +
                "original unregistered slice image: each pixel holds its atlas coordinates, in mm. "+
                "Waits for the registrations of the slices to be done."
)
public class ExportDeformationFieldToImageJCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Resolution level",
            description = "Resolution level of the original image the exported image matches: 0 = full resolution, "
                    + "1 = first downscaled level, etc. Must exist in the slice files.")
    int resolution_level = 0;

    @Parameter(label = "Additional downsampling factor",
            description = "Integer factor to further downsample the exported image, on top of the resolution level. 1 = no downsampling.")
    int downsampling = 1;

    @Parameter(label = "Max iterations for transform inversion",
            description = "Maximum number of iterations used to numerically invert non-linear (spline) registrations. "
                    + "Increase it if the inversion does not converge. Default 200.")
    int max_number_of_iterations = 200;

    @Parameter(type = ItemIO.OUTPUT, label = "Images",
            description = "One coordinates image per selected slice.")
    ImagePlus[] images;

    @Override
    public void run() {
        // TODO : check if tasks are done
        List<SliceSources> slicesToExport = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        double tolerance = mp.getAtlas().getMap().getAtlasPrecisionInMillimeter();
        if (slicesToExport.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to operate on.");
            return;
        }

        images = new ImagePlus[slicesToExport.size()];

        Map<SliceSources, ExportDeformationFieldToImagePlusAction> tasks = new HashMap<>();

        for (SliceSources slice : slicesToExport) {
            ExportDeformationFieldToImagePlusAction export = new ExportDeformationFieldToImagePlusAction(mp, slice,
                    resolution_level, downsampling, 0, tolerance, max_number_of_iterations);

            tasks.put(slice, export);
            export.runRequest();
        }

        IntStream.range(0,slicesToExport.size()).parallel().forEach(i -> {
            SliceSources slice = slicesToExport.get(i);
            boolean success = slice.waitForEndOfAction(tasks.get(slice));
            if (success) {
                images[i] = tasks.get(slice).getImagePlus();
                tasks.get(slice).clean();
                mp.infoMessageForUser.accept("", "Export deformation field to ImagePlus of slice "+slice+" done ("+(i+1)+"/"+images.length+")");
                images[i].show();
            } else {
                mp.errorMessageForUser.accept("Export to ImageJ Stack error","Error in export of slice "+slice);
            }
        });

    }

}