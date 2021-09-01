package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.ExportDeformationFieldToImagePlus;
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

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>Export Deformation Field to ImageJ")
public class ExportDeformationFieldToImageJ implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Resolution level (0 = max resolution)")
    int resolutionlevel = 0;

    @Parameter(label = "Extra DownSampling")
    int downsampling = 1;

    @Parameter(label = "Deformation error tolerance in microns (default 10)")
    double tolerance = 10;

    @Parameter(label = "Max iterations in invertible transform computation (default 200)")
    int maxNumberOfIterations = 200;

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus[] images;

    @Override
    public void run() {
        // TODO : check if tasks are done
        List<SliceSources> slicesToExport = mp.getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        tolerance = tolerance/1000.0; // micron to mm
        if (slicesToExport.size()==0) {
            mp.log.accept("No slice selected");
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to operate on.");
            return;
        }

        images = new ImagePlus[slicesToExport.size()];

        Map<SliceSources, ExportDeformationFieldToImagePlus> tasks = new HashMap<>();

        for (SliceSources slice : slicesToExport) {
            ExportDeformationFieldToImagePlus export = new ExportDeformationFieldToImagePlus(mp, slice,
                    resolutionlevel, downsampling, 0, tolerance, maxNumberOfIterations);

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
                //images[i].setTitle("Slice_"+i+"_"+slice);
                images[i].show();
            } else {
                mp.errorMessageForUser.accept("Export to ImageJ Stack error","Error in export of slice "+slice);
                return;
            }
        });

    }

}