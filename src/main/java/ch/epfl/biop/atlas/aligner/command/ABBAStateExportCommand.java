package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>ABBA - Export State For Sharing",
        description = "Takes a full project and store a downscaled version of the dataset for sharing")
public class ABBAStateExportCommand implements Command {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Select the atlas slicing orientation";

    @Parameter(callback = "coronalCB")
    Button coronal;

    @Parameter(callback = "sagittalCB")
    Button sagittal;

    @Parameter(callback = "horizontalCB")
    Button horizontal;

    @Parameter(choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String x_axis;

    @Parameter(choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String y_axis;

    @Parameter(choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String z_axis;

    @Parameter
    public Atlas ba;

    @Parameter
    Context context;

    @Parameter(style = "open", persist = false)
    File state_file;

    @Parameter(type = ItemIO.OUTPUT)
    Boolean success = false;

    @Parameter
    Context ctx;

    @Parameter
    String experiment_information;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String channels = "*";

    @Parameter
    double target_resolution_micrometer;

    @Override
    public void run() {
        AffineTransform3D orientation;
        try {
            orientation = ReslicedAtlas.getTransformFromCoronal(
                    x_axis.substring(0,2),
                    y_axis.substring(0,2),
                    z_axis.substring(0,2)
            );
        } catch (IllegalArgumentException exception) {
            System.err.println("Incorrect arguments, you need to use all three axes.");
            return;
        }

        AffineTransform3D slicingTransform = new AffineTransform3D();
        slicingTransform.set(ba.getMap().getCoronalTransform());
        slicingTransform.concatenate(orientation);

        ReslicedAtlas ra = new ReslicedAtlas(ba);
        ra.setResolution(ba.getMap().getAtlasPrecisionInMillimeter());
        ra.setSlicingTransform(slicingTransform);

        MultiSlicePositioner mp = new MultiSlicePositioner(ba, ra, context);

        mp.loadState(state_file);

        // Plan:
        // - raster the slices to the target resolution

        final SourcesProcessor preprocess;
        if (!channels.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(channels.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();
            if (maxIndex>=mp.getChannelBoundForSelectedSlices()) {
                mp.errorMessageForUser.accept("Missing channel in selected slice(s).",
                        "Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }
            preprocess = new SourcesChannelsSelect(indices);
        } else {
            preprocess = SourcesProcessorHelper.Identity();
        }

        mp.getSlices().forEach(slice -> {
            SourceAndConverter[] originalSources =
                    preprocess.apply(slice.getRegisteredSources(slice.getNumberOfRegistrations()-3));

            Source model = slice.getModelWithGridSize(target_resolution_micrometer);
            SourceAndConverter<?> modelSac = SourceAndConverterHelper.createSourceAndConverter(model);

            SourceAndConverter[] resampledSources = new SourceAndConverter[originalSources.length]; // Compulsory or the push is useless!

            for (int iChannel = 0; iChannel < originalSources.length; iChannel++) {
                SourceResampler resampler = new SourceResampler(null, modelSac,
                        "Slice_"+slice.getIndex()+"_Ch"+iChannel, true, true, true, 0);
                resampledSources[iChannel] = resampler.apply(originalSources[iChannel]);
            }

            ImageJFunctions.show(resampledSources[0].getSpimSource().getSource(0,0));

        });

        // Now we need to export the final data, resampled at the resolution of the atlas

        /*List<SliceSources> slicesToExport = mp.getSlices();

        // -------------------- Registered slices


        double[] roi = mp.getROI();

        Map<SliceSources, ExportSliceToImagePlusAction> tasks = new HashMap<>();

        ImagePlus image;

        for (SliceSources slice : slicesToExport) {
            ExportSliceToImagePlusAction export = new ExportSliceToImagePlusAction(mp, slice,
                    preprocess,
                    roi[0], roi[1], roi[2], roi[3],
                    mp.getAtlas().getMap().getAtlasPrecisionInMillimeter(), 0,true);
            tasks.put(slice, export);
            export.runRequest();
        }

        IntStream.range(0,slicesToExport.size()).parallel().forEach(i -> {
            SliceSources slice = slicesToExport.get(i);
            boolean success = slice.waitForEndOfAction(tasks.get(slice));
            if (success) {
                ImagePlus sliceImage = tasks.get(slice).getImagePlus();
                tasks.get(slice).clean();
                mp.infoMessageForUser.accept("ImagePlus export", "Slice "+slice+" done ("+(i+1)+"/"+slicesToExport.size()+")");
                sliceImage.setTitle("Slice_"+i);
                sliceImage.show();
            } else {
                mp.errorMessageForUser.accept("Export of registered slices errored","Error in export of slice "+slice);
            }
        });*/

        //--------------- Deformation field

        /*double tolerance = mp.getAtlas().getMap().getAtlasPrecisionInMillimeter();

        tasks = new HashMap<>();

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
                mp.infoMessageForUser.accept("", "Export deformation field to ImagePlus of slice "+slice+" done ("+(i+1)+"/"+images.length+")");
                images[i].show();
            } else {
                mp.errorMessageForUser.accept("Export to ImageJ Stack error","Error in export of slice "+slice);
            }
        });*/


        success = true;

    }

    void coronalCB() {
        this.x_axis = "RL (Right-Left)";
        this.y_axis = "SI (Superior-Inferior)";
        this.z_axis = "AP (Anterior-Posterior)";
    }

    void horizontalCB() {
        this.x_axis = "LR (Left-Right)";
        this.y_axis = "AP (Anterior-Posterior)";
        this.z_axis = "SI (Superior-Inferior)";
    }

    void sagittalCB() {
        this.x_axis = "AP (Anterior-Posterior)";
        this.y_axis = "SI (Superior-Inferior)";
        this.z_axis = "LR (Left-Right)";
    }
}
