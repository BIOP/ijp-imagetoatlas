package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.ExportSliceToImagePlus;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesChannelsSelect;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessor;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessorHelper;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>Export Slices as Quick NII Dataset")
public class ExportSlicesToQuickNIIDatasetCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Pixel Size in micron")
    double px_size_micron = 40;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String slicesStringChannel = "*";

    @Parameter(label = "Section Name Prefix")
    String imageName = "Section";

    @Parameter(label = "QuickNII dataset folder", style="directory")
    File datasetFolder;

    @Parameter(label = "Convert to 8 bit image")
    boolean convertTo8Bits = true;

    @Parameter(label = "Convert to jpg (single channel recommended)")
    boolean convertToJpg = true;

    @Parameter
    boolean interpolate;

    @Override
    public void run() {

        List<SliceSources> slicesToExport = mp.getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        SourcesProcessor preprocess = SourcesProcessorHelper.Identity();

        if (!slicesStringChannel.trim().equals("*")) {
            List<Integer> indices = Arrays.asList(slicesStringChannel.trim().split(",")).stream().mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();

            if (maxIndex>=mp.getChannelBoundForSelectedSlices()) {
                mp.log.accept("Missing channel in selected slice(s).");
                mp.errlog.accept("Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }

            preprocess = new SourcesChannelsSelect(indices);
        }

        // Creates
        if (!datasetFolder.exists()) {
            if (!datasetFolder.mkdir()) {
                mp.errorMessageForUser.accept("QuickNII dataset export failure", "Cannot create folder "+datasetFolder.getAbsolutePath());
            }
        }

        if (slicesToExport.size()==0) {
            mp.log.accept("No slice selected");
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to export");
            return;
        }

        double[] roi = mp.getROI();

        Map<SliceSources, ExportSliceToImagePlus> tasks = new HashMap<>();

        for (SliceSources slice : slicesToExport) {
            ExportSliceToImagePlus export = new ExportSliceToImagePlus(mp, slice,
                    preprocess,
                    roi[0], roi[1], roi[2], roi[3],
                    px_size_micron / 1000.0, 0,interpolate);

            tasks.put(slice, export);
            export.runRequest();
        }

        //ImagePlus[] images = new ImagePlus[slicesToExport.size()];

        DecimalFormat df = new DecimalFormat("000");
        IntStream.range(0,slicesToExport.size()).parallel().forEach(i -> {
            SliceSources slice = slicesToExport.get(i);
            boolean success = slice.waitForEndOfAction(tasks.get(slice));
            if (success) {
                ImagePlus imp = tasks.get(slice).getImagePlus();
                tasks.get(slice).clean();
                imp.setTitle(imageName+"_s"+df.format(i));

                if (convertTo8Bits) {
                    new ImageConverter(imp).convertToGray8();
                }

                if (convertToJpg) {

                    IJ.saveAs(imp,"jpeg",
                            datasetFolder.getAbsolutePath() + File.separator + // Folder
                                    imageName + "_s" + df.format(i) + ".jpg" // image name, three digits, and underscore s
                    );

                } else {
                    IJ.save(imp,
                            datasetFolder.getAbsolutePath() + File.separator + // Folder
                                    imageName + "_s" + df.format(i) + ".tif" // image name, three digits, and underscore s
                    );
                }

                mp.log.accept("Export of slice "+slice+" done ("+(i+1)+"/"+slicesToExport.size()+")");
            } else {
                mp.errorMessageForUser.accept("Slice export error","Error in export of slice "+slice);
                return;
            }
        });

        mp.warningMessageForUser.accept("Export as QuickNii Dataset done", "Folder : "+datasetFolder.getAbsolutePath());

    }

}
