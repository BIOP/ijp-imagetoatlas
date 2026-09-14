package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.action.ExportAtlasSliceToImagePlusAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.source.processor.SourcesChannelsSelect;
import ch.epfl.biop.source.processor.SourcesProcessor;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;
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

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Atlas to ImageJ",
        description = "Exports the atlas section at the position of each selected slice as an ImageJ image, one plane per slice, "
                + "within the current region of interest. Same geometry as 'Export Registered Slices to ImageJ' "
                + "with the same pixel size, so both images can be overlaid.")
public class ExportAtlasToImageJCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label="Pixel size (micrometers)",
            description = "Pixel size of the exported image.")
    double pixel_size_um = 20;

    @Parameter(label = "Atlas channels",
            description = "0-based indices of the atlas channels to export, comma separated (e.g. '0,1'), or '*' for all channels. "
                    + "Depending on the atlas, channels include anatomical images, region borders, atlas coordinates, "
                    + "a left/right indicator and region labels.")
    String atlas_channels_csv = "*";

    @Parameter(label = "Image name",
            description = "Title of the exported ImageJ image.")
    String image_name = "Atlas";

    @Parameter(label = "Interpolate",
            description = "If checked, pixels are linearly interpolated when resampled; otherwise the nearest pixel is used (keep it unchecked for label channels).")
    boolean interpolate;

    @Parameter(type = ItemIO.OUTPUT, label = "Image",
            description = "Exported atlas image: one channel per atlas channel, one plane per selected slice.")
    ImagePlus image;

    @Override
    public void run() {
        // TODO : check if tasks are done
        List<SliceSources> slicesToExport = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        SourcesProcessor preprocess = SourcesProcessorHelper.Identity();

        if (!atlas_channels_csv.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(atlas_channels_csv.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();

            int maxChannelInAtlas = mp.getReslicedAtlas().nonExtendedSlicedSources.length;
            if (maxIndex>=maxChannelInAtlas) {
                mp.errorMessageForUser.accept("Wrong atlas channel index",
                        "The atlas only has "+maxChannelInAtlas+" channel(s).\n Maximum index : "+(maxChannelInAtlas-1) );
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
                    pixel_size_um / 1000.0, 0,interpolate);

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
                mp.infoMessageForUser.accept("", "Atlas export to ImagePlus of slice "+slice+" done ("+(i+1)+"/"+images.length+")");
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
        if (image!=null) {
            image.show();
            image.setTitle(image_name);
        }
    }

}