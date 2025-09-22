package ch.epfl.biop.atlas.aligner.command;


import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.DeformationFieldToImagePlus;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.kheops.ometiff.OMETiffExporter;
import ch.epfl.biop.sourceandconverter.SourceHelper;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.realtransform.AffineTransform3D;
import ome.units.UNITS;
import ome.units.quantity.Length;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.task.TaskService;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>File>ABBA - Export State For Sharing",
        description = "Takes a full project and store a downscaled version of the dataset for sharing")
public class  ABBAStateExportCommand implements Command {

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

    @Parameter(style = "open", persist = true)
    File state_file;

    @Parameter(style = "directory", persist = true)
    File save_path;

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

        //BdvMultislicePositionerView view = new BdvMultislicePositionerView(mp, new DefaultBdvSupplier(new SerializableBdvOptions()).get());

        mp.loadState(state_file);
        mp.selectSlice(mp.getSlices()); // Select all slices

        // We need a robust approach. Will it be simple ? Not sure.
        // We'll need to resample the original data at a certain resolution, and we need to avoid making an image too big. Let's start by ignoring the possibility of images too big

        // So, let's raster the registered images at a certain resolution

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

        File[] downscaledTiffs = new File[mp.getSlices().size()];

        for (SliceSources slice : mp.getSlices()) {
            SourceAndConverter[] sources = preprocess.apply(slice.getOriginalSources());

            SourceAndConverter<?> model = SourceHelper.getModelFusedMultiSources(sources,
                    0, 1,
                    0.01, 0.01, 0.01,
                    1,
                    2, 2, 2, "Model");

            SourceAndConverter[] resampled = new SourceAndConverter[sources.length];
            String sliceName = "Slice_"+slice.getIndex();
            for ( int i = 0; i< sources.length; i++) {
                resampled[i] = new SourceResampler<>( sources[i], model, sliceName+"_ch"+i, true, true,true,0).get();
            }

            String pathimage = save_path.getAbsolutePath()+File.separator+sliceName+".ome.tiff";
            try {
                OMETiffExporter.builder().put(resampled)
                        .defineMetaData(sliceName)
                        .putMetadataFromSources(sources, UNITS.MILLIMETER)
                        .voxelPhysicalSize(
                                new Length(target_resolution_micrometer, UNITS.MICROMETER),
                                new Length(target_resolution_micrometer, UNITS.MICROMETER),
                                new Length(target_resolution_micrometer, UNITS.MICROMETER))
                        .defineWriteOptions()
                        .lzw()
                        .tileSize(512, 512)
                        .monitor(ctx.getService(TaskService.class))
                        .nResolutionLevels(1)
                        .savePath(pathimage)
                        .create().export();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            downscaledTiffs[slice.getIndex()] = new File(pathimage);
        }

        // Now let's rebuild a registration pipeline
        // Let's create a new mp instance

        // We need to get a new ReslicedAtlas instance or else the rotation angles go to 0
        AffineTransform3D slicingTransform2 = new AffineTransform3D();
        slicingTransform2.set(ba.getMap().getCoronalTransform());
        slicingTransform2.concatenate(orientation);

        ReslicedAtlas ra2 = new ReslicedAtlas(ba);
        ra2.setResolution(ba.getMap().getAtlasPrecisionInMillimeter());
        ra2.setSlicingTransform(slicingTransform2);

        MultiSlicePositioner mpDS = new MultiSlicePositioner(ba, ra2, context);

        try {
            ctx.getService(CommandService.class).run(ImportSlicesFromFilesCommand.class, true,
                    "mp", mpDS,
                    "datasetname", "data",
                    "files", downscaledTiffs,
                    "split_rgb_channels", false,
                    "slice_axis_initial_mm", 0,
                    "increment_between_slices_mm", 0.1
            ).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        mpDS.waitForTasks();

        // Now let's copy all registration steps for each slice. I need to take care of the indexing!!

        // Copy to avoid issues with reindexing when slices are moved - the order is correct, we don't want to mess it up
        List<SliceSources> slicesCopied = new ArrayList<>(mpDS.getSlices());
        for (int iSlice = 0; iSlice<mp.getSlices().size();iSlice++) {
            mpDS.moveSlice(slicesCopied.get(iSlice), mp.getSlices().get(iSlice).getSlicingAxisPosition());
            RegisterSlicesCopyAndApplyCommand.copyRegistration(mp,
                    mp.getSlices().get(iSlice),
                    mpDS,slicesCopied.get(iSlice),
                    true);
        }

        mpDS.waitForTasks();

        //BdvMultislicePositionerView view = new BdvMultislicePositionerView(mpDS, new DefaultBdvSupplier(new SerializableBdvOptions()).get());

        mpDS.getReslicedAtlas().setRotateX(mp.getReslicedAtlas().getRotateX());
        mpDS.getReslicedAtlas().setRotateY(mp.getReslicedAtlas().getRotateY());

        // Now let's store the abba result
        File pathStateSave = new File(save_path+File.separator+"state.abba");
        mpDS.saveState(pathStateSave, true);

        // Now let's save the registration transformation field, downscaled 10 times (so every 100 micrometers

        mpDS.getSlices().parallelStream().forEach(slice -> {
            ImagePlus resultImage = DeformationFieldToImagePlus.export(slice, 0, 1, 0, 0.01, 400);
            IJ.saveAsTiff(resultImage, this.save_path+File.separator+"Slice_"+slice.getIndex()+"_deformation_field.tif");
        });

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
