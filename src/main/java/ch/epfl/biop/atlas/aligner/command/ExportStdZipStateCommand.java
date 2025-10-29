package ch.epfl.biop.atlas.aligner.command;


import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.DeformationFieldToImagePlus;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import ch.epfl.biop.atlas.aligner.SetSliceBackgroundAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.UnMirrorSliceAction;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.kheops.ometiff.OMETiffExporter;
import ch.epfl.biop.sourceandconverter.SourceHelper;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.task.Task;
import org.scijava.task.TaskService;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Standardized ABBA Project (Zip)",
        description = "Takes a full project and store a downscaled version of the dataset for sharing")
public class ExportStdZipStateCommand implements Command {

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

    @Parameter
    String identifier;

    @Parameter(style = "open", persist = true)
    File state_file;

    @Parameter(style = "directory", required = false)
    File save_path;

    @Parameter(type = ItemIO.OUTPUT)
    Boolean success = false;

    @Parameter
    Context ctx;

    @Parameter
    TaskService taskService;

    @Parameter(style = "text area")
    String experiment_information;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String channels = "*";

    @Parameter
    double target_resolution_micrometer;

    @Parameter
    int downscale_deformation_field = 4;

    @Override
    public void run() {
        Task exportTask = taskService.createTask("Export of "+state_file.getName());
        exportTask.setProgressMaximum(3);
        Task createResampledData = taskService.createTask("Export slices ("+target_resolution_micrometer+" um)");
        Task computeDeformationField = taskService.createTask("Compute deformation field ("+downscale_deformation_field+"x downscaled)");
        Task zipAll = taskService.createTask("Zip all generated files");

        try {
            exportTask.start();
            if (save_path == null) save_path = new File(state_file.getParent()); // Folder containing the ABBA state

            List<File> data = new ArrayList<>();
            List<File> deformationField = new ArrayList<>();
            File stateFile;

            AffineTransform3D orientation;
            try {
                orientation = ReslicedAtlas.getTransformFromCoronal(
                        x_axis.substring(0, 2),
                        y_axis.substring(0, 2),
                        z_axis.substring(0, 2)
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
                if (maxIndex >= mp.getChannelBoundForSelectedSlices()) {
                    mp.errorMessageForUser.accept("Missing channel in selected slice(s).",
                            "Missing channel in selected slice(s)\n One selected slice only has " + mp.getChannelBoundForSelectedSlices() + " channel(s).\n Maximum index : " + (mp.getChannelBoundForSelectedSlices() - 1));
                    return;
                }
                preprocess = new SourcesChannelsSelect(indices);
            } else {
                preprocess = SourcesProcessorHelper.Identity();
            }

            File[] downscaledTiffs = new File[mp.getSlices().size()];

            createResampledData.setProgressMaximum(mp.getSlices().size());
            createResampledData.start();

            for (SliceSources slice : mp.getSlices()) {
                SourceAndConverter[] sources = preprocess.apply(slice.getOriginalSources());

                SourceAndConverter<?> model = SourceHelper.getModelFusedMultiSources(sources,
                        0, 1,
                        target_resolution_micrometer/1000.0, target_resolution_micrometer/1000.0, target_resolution_micrometer/1000.0,
                        1,
                        2, 2, 2, "Model");

                SourceAndConverter[] resampled = new SourceAndConverter[sources.length];
                String sliceName = "Slice_" + slice.getIndex();
                for (int i = 0; i < sources.length; i++) {
                    resampled[i] = new SourceResampler<>(sources[i], model, sliceName + "_ch" + i, true, true, true, 0).get();
                }

                String pathimage = save_path.getAbsolutePath() + File.separator + sliceName + ".ome.tiff";
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
                    data.add(new File(pathimage)); // Stores data image for zipping later
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                downscaledTiffs[slice.getIndex()] = new File(pathimage);
                createResampledData.setProgressValue(createResampledData.getProgressValue()+1);
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
            for (int iSlice = 0; iSlice < mp.getSlices().size(); iSlice++) {
                SliceSources slice = slicesCopied.get(iSlice);
                // Pick and apply a potential set white Pixel Background
                List<CancelableAction> setBgActions = mp.getActionsFromSlice(mp.getSlices().get(iSlice)).stream().filter(action -> action instanceof SetSliceBackgroundAction).collect(Collectors.toList());
                if (!setBgActions.isEmpty()) {
                    SetSliceBackgroundAction setSliceBGAction = new SetSliceBackgroundAction(mpDS, slice, ((SetSliceBackgroundAction)setBgActions.get(setBgActions.size()-1)).getBgValue());
                    setSliceBGAction.runRequest();
                }

                mpDS.moveSlice(slice, mp.getSlices().get(iSlice).getSlicingAxisPosition());
                RegisterSlicesCopyAndApplyCommand.copyRegistration(mp,
                        mp.getSlices().get(iSlice),
                        mpDS, slicesCopied.get(iSlice),
                        false);

                List<CancelableAction> unmirrorActions = mp.getActionsFromSlice(mp.getSlices().get(iSlice)).stream().filter(action -> action instanceof SetSliceBackgroundAction).collect(Collectors.toList());
                if (!unmirrorActions.isEmpty()) {
                    UnMirrorSliceAction unMirrorAction = new UnMirrorSliceAction(mpDS, slice);
                    unMirrorAction.runRequest();
                }
            }

            mpDS.waitForTasks();

            //BdvMultislicePositionerView view = new BdvMultislicePositionerView(mpDS, new DefaultBdvSupplier(new SerializableBdvOptions()).get());

            mpDS.getReslicedAtlas().setRotateX(mp.getReslicedAtlas().getRotateX());
            mpDS.getReslicedAtlas().setRotateY(mp.getReslicedAtlas().getRotateY());

            // Now let's store the abba result
            File pathStateSave = new File(save_path + File.separator + "state.abba");
            mpDS.saveState(pathStateSave, false);
            stateFile = pathStateSave;

            // Now let's save the registration transformation field, downscaled 10 times (so every 100 micrometers
            exportTask.setProgressValue(1);

            computeDeformationField.setProgressMaximum(mp.getSlices().size());
            computeDeformationField.start();

            for (SliceSources slice: mpDS.getSlices()) {
                ImagePlus resultImage = DeformationFieldToImagePlus.export(slice, 0, downscale_deformation_field, 0, 0.01, 400);
                String path = this.save_path + File.separator + "Slice_" + slice.getIndex() + "_deformation_field.tif";
                IJ.saveAsTiff(resultImage, path);
                deformationField.add(new File(path));
                computeDeformationField.setProgressValue(computeDeformationField.getProgressValue()+1);
            }


            exportTask.setProgressValue(2);

            exportTask.setStatusMessage("Zipping results...");
            // Zip all for convenience of sharing

            ABBAHelper.ABBAExportMeta meta = new ABBAHelper.ABBAExportMeta();
            meta.timestamp = Instant.now().toString();
            meta.resolution_um = target_resolution_micrometer;
            meta.atlas_name = ba.getName();
            meta.experiment_information = experiment_information;
            meta.n_slices = mp.getSlices().size();
            meta.x_axis = x_axis;
            meta.y_axis = y_axis;
            meta.z_axis = z_axis;
            meta.downscale_deformation_field = downscale_deformation_field;

            mp.close();
            mpDS.close();

            ctx.getService(ObjectService.class).removeObject(mp);
            ctx.getService(ObjectService.class).removeObject(mpDS);

            // Remove all sources - TODO : make this more specific!
            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .remove(
                            SourceAndConverterServices
                                    .getSourceAndConverterService().getSourceAndConverters().toArray(new SourceAndConverter[0])
                    );

            zipAll.start();
            zipAll.setProgressMaximum(data.size()+ deformationField.size()+1);

            try {
                zipAndDeleteFiles(stateFile, data, deformationField, meta, save_path + File.separator + "abba_export_" + identifier + ".zip", zipAll);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            exportTask.setProgressValue(3);

            exportTask.setStatusMessage("Done!");


            success = true;
        } finally {
            exportTask.finish();
            createResampledData.finish();
            computeDeformationField.finish();
            zipAll.finish();
        }


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

    public void zipAndDeleteFiles(File stateFile, List<File> data, List<File> deformationField,
                                  ABBAHelper.ABBAExportMeta meta, String zipFileName, Task task) throws IOException {

        // Create Gson instance
        Gson gson = new GsonBuilder()
                .setPrettyPrinting() // Optional: makes JSON readable
                .create();

        // Create the zip file
        try (FileOutputStream fos = new FileOutputStream(zipFileName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // Add meta.json first
            addMetaToZip(zos, meta, gson);

            // Add stateFile to zip
            if (stateFile != null && stateFile.exists()) {
                addFileToZip(zos, stateFile);
            }

            task.setProgressValue(task.getProgressValue()+1);

            // Add all data files to zip
            for (File file : data) {
                if (file != null && file.exists()) {
                    addFileToZip(zos, file);
                    task.setProgressValue(task.getProgressValue()+1);
                }
            }

            // Add all deformation field files to zip
            for (File file : deformationField) {
                if (file != null && file.exists()) {
                    addFileToZip(zos, file);
                    task.setProgressValue(task.getProgressValue()+1);
                }
            }
        }

        // Delete original files after successful zip creation
        deleteFileIfExists(stateFile);

        for (File file : data) {
            deleteFileIfExists(file);
        }

        for (File file : deformationField) {
            deleteFileIfExists(file);
        }
    }

    private void addMetaToZip(ZipOutputStream zos, ABBAHelper.ABBAExportMeta meta, Gson gson) throws IOException {
        // Serialize meta object to JSON string
        String jsonString = gson.toJson(meta);

        // Convert to bytes
        byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

        // Create zip entry
        ZipEntry metaEntry = new ZipEntry("meta.json");
        zos.putNextEntry(metaEntry);

        // Write JSON bytes to zip
        zos.write(jsonBytes);
        zos.closeEntry();
    }

    // Keep your existing helper methods
    private void addFileToZip(ZipOutputStream zos, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
        }
    }

    private void deleteFileIfExists(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                System.err.println("Failed to delete file: " + file.getAbsolutePath());
            }
        }
    }
}
