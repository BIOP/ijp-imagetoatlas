package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.sourceandconverter.exporter.CZTRange;
import ch.epfl.biop.sourceandconverter.exporter.ImagePlusGetter;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import ij.ImagePlus;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Original Slices to ImageJ",
        description = "Export to ImageJ the original unregistered slice data (for each selected slice)." +
                "If the image has more than 2GPixels, this will fail. "+
                "Resolution levels can be specified.")
public class ExportSlicesOriginalDataToImageJCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String slices_string_channels = "*";

    @Parameter(label = "Resolution level (0 = max resolution)")
    int resolution_level = 0;

    @Parameter(label = "verbose")
    boolean verbose = false;

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus[] images;

    @Override
    public void run() {
        // TODO : check if tasks are done
        List<SliceSources> slicesToExport = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slicesToExport.size()==0) {
            mp.log.accept("No slice selected");
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to export");
            return;
        }

        SourcesProcessor preprocess = SourcesProcessorHelper.Identity();

        if (!slices_string_channels.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(slices_string_channels.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();

            if (maxIndex>=mp.getChannelBoundForSelectedSlices()) {
                mp.log.accept("Missing channel in selected slice(s).");
                mp.errlog.accept("Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }

            preprocess = new SourcesChannelsSelect(indices);
        }

        images = new ImagePlus[slicesToExport.size()];

        try {
            int index = 0;
            for (SliceSources slice : slicesToExport) {

                List<SourceAndConverter> sliceSources = Arrays.asList(preprocess.apply(slice.getOriginalSources()));
                CZTRange range = ImagePlusGetter.fromSources(sliceSources, 0, resolution_level);
                images[index] = ImagePlusGetter.getImagePlus(sliceSources.get(0).getSpimSource().getName(), sliceSources, resolution_level, range, verbose, false, false, false);
                images[index].show();
                index++;
            }
        } catch (Exception e) {
            mp.errlog.accept(e.getMessage());
        }
    }

}