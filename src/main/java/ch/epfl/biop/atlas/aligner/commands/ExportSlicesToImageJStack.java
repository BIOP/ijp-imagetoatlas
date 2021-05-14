package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.ExportSliceToImagePlus;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessorHelper;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>Export Slices as ImageJ Stack")
public class ExportSlicesToImageJStack implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Pixel Size in micron")
    double px_size_micron;

    @Parameter
    boolean interpolate;

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus image;

    @Override
    public void run() {
        // TODO : check if tasks are done
        List<SliceSources> slicesToExport = mp.getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slicesToExport.size()==0) {
            mp.log.accept("No slice selected");
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to export");
            return;
        }

        double[] roi = mp.getROI();

        Map<SliceSources, ExportSliceToImagePlus> tasks = new HashMap<>();

        for (SliceSources slice : slicesToExport) {
            ExportSliceToImagePlus export = new ExportSliceToImagePlus(mp, slice,
                    SourcesProcessorHelper.Identity(),
                    roi[0], roi[1], roi[2], roi[3],
                    px_size_micron / 1000.0, 0,interpolate);

            tasks.put(slice, export);
            export.runRequest();
        }

        ImagePlus[] images = new ImagePlus[slicesToExport.size()];
        for (int i = 0;i<slicesToExport.size();i++) {
            SliceSources slice = slicesToExport.get(i);
            boolean success = slice.waitForEndOfAction(tasks.get(slice));
            if (success) {
                images[i] = tasks.get(slice).getImagePlus();
                tasks.get(slice).clean();
                mp.log.accept("Export to ImagePlus of slice "+slice+" done ("+(i+1)+"/"+images.length+")");
                images[i].setTitle("Slice_"+i+"_"+slice);
                images[i].show();
            } else {
                mp.errorMessageForUser.accept("Export to ImageJ Stack error","Error in export of slice "+slice);
                return;
            }
        }

        image = Concatenator.run(images);
        image.show();

    }

}