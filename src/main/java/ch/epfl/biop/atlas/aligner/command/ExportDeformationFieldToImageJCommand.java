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
                "Exports physical coordinates of the atlas in a " +
                "3 channel (x,y,z) image that matches pixels of the "+
                "initial unregistered slice (for each selected slice). "+
                "Resolution levels can be specified."
)
public class ExportDeformationFieldToImageJCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Resolution level (0 = max resolution)")
    int resolution_level = 0;

    @Parameter(label = "Extra DownSampling")
    int downsampling = 1;

    @Parameter(label = "Max iterations in invertible transform computation (default 200)")
    int max_number_of_iterations = 200;

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus[] images;

    @Override
    public void run() {
        // TODO : check if tasks are done
        List<SliceSources> slicesToExport = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        double tolerance = mp.getAtlas().getMap().getAtlasPrecisionInMillimeter();
        if (slicesToExport.size()==0) {
            mp.log.accept("No slice selected");
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

        ImagePlus[] images = new ImagePlus[slicesToExport.size()];
        IntStream.range(0,slicesToExport.size()).parallel().forEach(i -> {
            SliceSources slice = slicesToExport.get(i);
            boolean success = slice.waitForEndOfAction(tasks.get(slice));
            if (success) {
                images[i] = tasks.get(slice).getImagePlus();
                tasks.get(slice).clean();
                mp.log.accept("Export deformation field to ImagePlus of slice "+slice+" done ("+(i+1)+"/"+images.length+")");
                images[i].show();
            } else {
                mp.errorMessageForUser.accept("Export to ImageJ Stack error","Error in export of slice "+slice);
            }
        });

    }

}