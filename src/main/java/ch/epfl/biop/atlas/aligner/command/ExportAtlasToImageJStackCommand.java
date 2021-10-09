package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.action.ExportAtlasSliceToImagePlusAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.sourcepreprocessor.SourcesChannelsSelect;
import ch.epfl.biop.atlas.aligner.sourcepreprocessor.SourcesProcessor;
import ch.epfl.biop.atlas.aligner.sourcepreprocessor.SourcesProcessorHelper;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Atlas to ImageJ",
        description = "Export atlas properties as an ImageJ stack (for each selected slice).")
public class ExportAtlasToImageJStackCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Pixel Size in micron")
    double px_size_micron = 20;

    @Parameter(label = "Channels to export, '*' for all channels")//choices = {"Structural Images", "Border only", "Coordinates", "Left / Right", "Labels % 65000" })
    String atlasStringChannel = "*";//String export_type;

    @Parameter(label = "Exported image name")
    String image_name = "Atlas";

    @Parameter
    boolean interpolate;

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus image;

    @Override
    public void run() {
        // TODO : check if tasks are done
        List<SliceSources> slicesToExport = mp.getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        SourcesProcessor preprocess = SourcesProcessorHelper.Identity();

        if (!atlasStringChannel.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(atlasStringChannel.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();

            int maxChannelInAtlas = mp.getReslicedAtlas().nonExtendedSlicedSources.length;
            if (maxIndex>=maxChannelInAtlas) {
                mp.log.accept("Missing channels in atlas.");
                mp.errlog.accept("The atlas only has "+maxChannelInAtlas+" channel(s).\n Maximum index : "+(maxChannelInAtlas-1) );
                return;
            }

            preprocess = new SourcesChannelsSelect(indices);
        }

        double[] roi = mp.getROI();

        Map<SliceSources, ExportAtlasSliceToImagePlusAction> tasks = new HashMap<>();

        for (SliceSources slice : slicesToExport) {
            ExportAtlasSliceToImagePlusAction export = new ExportAtlasSliceToImagePlusAction(mp, slice,
                    preprocess,
                    roi[0], roi[1], roi[2], roi[3],
                    px_size_micron / 1000.0, 0,interpolate);

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
                mp.log.accept("Atlas export to ImagePlus of slice "+slice+" done ("+(i+1)+"/"+images.length+")");
                images[i].setTitle("Slice_"+i+"_"+slice);
                images[i].show();
            } else {
                mp.errorMessageForUser.accept("Export to ImageJ Stack error","Error in export of slice "+slice);
            }
        });

        if (images.length>1) {
            // Concatenate and restore min max display of first slice
            int nChannels = images[0].getNChannels();

            double[] min = new double[nChannels];
            double[] max = new double[nChannels];
            for (int iCh = 0; iCh<nChannels;iCh++) {
                images[0].setC(iCh+1);
                min[iCh] = images[0].getProcessor().getMin();
                max[iCh] = images[0].getProcessor().getMax();
            }
            image = Concatenator.run(images);
            for (int iCh = 0; iCh<nChannels; iCh++) {
                image.setC(iCh+1);
                image.setDisplayRange( min[iCh],  max[iCh]);
            }
        } else {
            image = images[0];
        }
        image.show();
        image.setTitle(image_name);
    }

}