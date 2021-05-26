package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.ExportSliceToImagePlus;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesChannelsSelect;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessor;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessorHelper;
import ch.epfl.biop.quicknii.QuickNIIExporter;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;
import ij.process.ImageConverter;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Command which is using the amazing DeepSlice (https://researchers.mq.edu.au/en/publications/deepslice-a-deep-neural-network-for-fully-automatic-alignment-of-)
 * workflow by Harry Carey, Simon McMullan and William Redmond
 *
 *
 * You will need an internet connection (https://www.deepslice.com.au/)
 * in order to use this command, and there's some manual interaction
 * required (but it's worth it!)
 * Contrary to other registration command, the slices are not registered independently:
 * * it's easier to drag many files at once
 * * in ABBA, there is less degree of freedom from what deepslice allows, because the atlas has to be slice
 * in one angle. Because of that, this command works in several steps, the first step being to determine what
 * is the most appropriate slicing angle for all slices. Each slice has a prefered angle, and ABBA takes
 * the median of all slices.
 *
 * It is possible to forbid the angle adjustement, but in ABBA only. DeepSlice doesn't care. That could
 * result in worse results than the output of DeepSlice, but in fact, this is also positive in certain
 * cases because a single slicing angle leads to some 'regularisation'.
 *
 * Also, contrary to other registration methods, DeepSlice can help defining the location in Z of slices.
 * However, sometimes, DeepSlice is swapping slices incorrectly. So there should be an option to avoid swapping
 * slices position ( TODO ).
 *
 * DeepSlice provides the fastest way to have an initial:
 * * correct positioning in Z a
 * * slicing angle
 * * a first affine in-plane registration
 *
 * By default, ABBA downsamples to 30 microns per pixel for DeepSlice and saves as an 8 bit jpeg image.
 * Make sure you have multiresolution files if you don't want your downscaling to look bad!
 *
 * For now, only one channel can be used for the registration. (TODO : allow dual channel export)
 *
 */

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>DeepSlice Registration")
public class DeepSliceCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    PlatformService ps;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String slicesStringChannel = "*";

    @Parameter(label = "Section Name Prefix")
    String imageName = "Section";

    @Parameter(label = "QuickNII dataset folder", style="directory")
    File datasetFolder;

    //@Parameter(label = "pixel size in micrometer")
    double px_size_micron = 30;

    //@Parameter(label = "Convert to 8 bit image")
    boolean convertTo8Bits = false;

    //@Parameter(label = "Convert to jpg (single channel recommended)")
    boolean convertToJpg = true;

    //@Parameter
    boolean interpolate = false;

    @Override
    public void run() {
        List<SliceSources> slicesToExport = mp.getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (mp.getNumberOfSelectedSources()==0) {
            mp.log.accept("No slice selected");
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
            return;
        }


        /*exportDownsampledDataset(slicesToExport);

        IJ.log("Dataset exported in folder "+datasetFolder.getAbsolutePath());
        new WaitForUserDialog("Now opening DeepSlice webpage",
                "Drag and drop all slices into the webpage.")
                .show();
        try {
            ps.open(new URL("https://www.deepslice.com.au/"));
        } catch (Exception e) {
            mp.errorMessageForUser.accept("Couldn't open DeepSlice from Fiji, ",
                    "please go to https://www.deepslice.com.au/ and drag and drop your images");
        }

        new WaitForUserDialog("DeepSlice result",
                "Put the 'results.xml' file into "+datasetFolder.getAbsolutePath()+" then press ok.")
                .show();*/

        File deepSliceResult = new File(datasetFolder, "results.xml");

        if (!deepSliceResult.exists()) {
            mp.errorMessageForUser.accept("Could not find DeepSlice result file "+deepSliceResult.getAbsolutePath(),
                    "Command aborted");
            return;
        }

        // Ok, now comes the big deal. First, the an


    }

    private void exportDownsampledDataset(List<SliceSources> slices) {

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

        try {

            QuickNIIExporter.builder()
                    .roi(mp.getROI())
                    .cvt8bits(convertTo8Bits)
                    .jpeg(convertToJpg)
                    .setProcessor(preprocess)
                    .slices(slices)
                    .name(imageName)
                    .folder(datasetFolder)
                    .pixelSizeMicron(px_size_micron)
                    .interpolate(interpolate)
                    .create()
                    .export();

        } catch (Exception e) {
            mp.errorMessageForUser.accept("Export to Quick NII dataset error. ", e.getMessage());
        }
    }

}
