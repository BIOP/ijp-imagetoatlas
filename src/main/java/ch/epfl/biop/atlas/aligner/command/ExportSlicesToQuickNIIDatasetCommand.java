package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.bdv.sourcepreprocessor.SourcesChannelsSelect;
import ch.epfl.biop.bdv.sourcepreprocessor.SourcesProcessor;
import ch.epfl.biop.bdv.sourcepreprocessor.SourcesProcessorHelper;
import ch.epfl.biop.quicknii.QuickNIIExporter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Registered Slices to Quick NII Dataset")
public class ExportSlicesToQuickNIIDatasetCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Pixel Size in micron")
    double px_size_micron = 40;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String slices_string_channels = "*";

    @Parameter(label = "Section Name Prefix")
    String image_name = "Section";

    @Parameter(label = "QuickNII dataset folder", style="directory")
    File dataset_folder;

    @Parameter(label = "Convert to 8 bit image")
    boolean convert_to_8_bits = true;

    @Parameter(label = "Convert to jpg (single channel recommended)")
    boolean convert_to_jpg = true;

    @Parameter
    boolean interpolate;

    @Override
    public void run() {

        List<SliceSources> slicesToExport = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

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

        try {

            QuickNIIExporter.builder()
                    .roi(mp.getROI())
                    .cvt8bits(convert_to_8_bits)
                    .jpeg(convert_to_jpg)
                    .setProcessor(preprocess)
                    .slices(slicesToExport)
                    .name(image_name)
                    .folder(dataset_folder)
                    .pixelSizeMicron(px_size_micron)
                    .interpolate(interpolate)
                    .create()
                    .export();

        } catch (Exception e) {
            mp.errorMessageForUser.accept("Export to Quick NII dataset error. ", e.getMessage());
        }

    }

}
