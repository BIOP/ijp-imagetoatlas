package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.ExportAtlasSliceToImagePlus;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesChannelsSelect;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessor;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessorHelper;
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

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>Export Atlas as ImageJ Stack")
public class ExportAtlasToImageJStack implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Pixel Size in micron")
    double px_size_micron = 20;

    //@Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String atlasStringChannel;// = "*";

    @Parameter(choices = {"Structural Images", "Border only", "Coordinates", "Left / Right", "Labels % 65000" })
    String exportType;

    @Parameter(label = "Exported image name")
    String imageName = "Atlas";

    @Parameter
    boolean interpolate;

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus image;

    @Override
    public void run() {
        // TODO : check if tasks are done
        List<SliceSources> slicesToExport = mp.getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        switch (exportType) {
            case "Structural Images": atlasStringChannel = "0,1,2"; break;
            case "Border only": atlasStringChannel = "2"; break;
            case "Coordinates": atlasStringChannel = "3,4,5"; break;
            case "Left / Right": atlasStringChannel = "6"; break;
            case "Labels % 65000": atlasStringChannel = "7"; interpolate = false; break;
        }

        SourcesProcessor preprocess = SourcesProcessorHelper.Identity();

        if (!atlasStringChannel.trim().equals("*")) {
            List<Integer> indices = Arrays.asList(atlasStringChannel.trim().split(",")).stream().mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

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

        Map<SliceSources, ExportAtlasSliceToImagePlus> tasks = new HashMap<>();

        for (SliceSources slice : slicesToExport) {
            ExportAtlasSliceToImagePlus export = new ExportAtlasSliceToImagePlus(mp, slice,
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
                return;
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
        image.setTitle(imageName);
    }

}