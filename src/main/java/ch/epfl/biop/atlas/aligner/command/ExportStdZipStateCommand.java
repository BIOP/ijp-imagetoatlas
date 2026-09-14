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
import ch.epfl.biop.java.utilities.TempDirectory;
import ch.epfl.biop.kheops.ometiff.OMETiffExporter;
import ch.epfl.biop.source.SourceHelper;
import ch.epfl.biop.source.processor.SourcesChannelsSelect;
import ch.epfl.biop.source.processor.SourcesProcessor;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ij.IJ;
import ij.ImagePlus;
import ome.units.UNITS;
import ome.units.quantity.Length;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.task.Task;
import org.scijava.task.TaskService;
import sc.fiji.bdvpg.service.SourceServices;
import sc.fiji.bdvpg.source.transform.SourceResampler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Standardized ABBA Project (Zip)",
        description = "Creates a standardized, shareable zip of the current ABBA session: all slices downscaled to a given pixel size, "
                + "their registrations, their deformation fields and metadata (atlas, slicing orientation, experiment information). "
                + "The zip can be opened with 'Import Standardized ABBA Project (Zip)'. The session itself is not modified.")
public class ExportStdZipStateCommand implements Command {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "<html>Exports all slices of the session, downscaled, with their registrations.<br>"
            + "Wait for the end of the export before modifying the session.</html>";

    @Parameter(label = "ABBA session",
            description = "The ABBA session to export.")
    MultiSlicePositioner mp;

    @Parameter(label = "Export identifier",
            description = "Short name of the export, used in the zip file name: abba_export_<identifier>.zip.")
    String identifier;

    @Parameter(style = "directory", label = "Output folder",
            description = "Folder where the zip is written. A zip with the same name must not exist.")
    File output_folder;

    @Parameter(style = "text area", label = "Experiment information",
            description = "Free text describing the experiment (animal, staining, sectioning...), stored in the metadata of the zip.")
    String experiment_information;

    @Parameter(label = "Slice channels",
            description = "0-based indices of the slice channels to export, comma separated (e.g. '0,2'), or '*' for all channels.")
    String slice_channels_csv = "*";

    @Parameter(label = "Pixel size of the exported slices (micrometers)",
            description = "The original slice images are downscaled to this pixel size to keep the zip small.")
    double pixel_size_um = 20;

    @Parameter(label = "Deformation field downscaling factor",
            description = "The deformation field (atlas coordinates of each pixel) is computed every N pixels of the exported slices.")
    int downscale_deformation_field = 4;

    @Parameter(type = ItemIO.OUTPUT, label = "Success",
            description = "True if the zip was created.")
    Boolean success = false;

    @Parameter
    Context ctx;

    @Parameter
    TaskService taskService;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSlices();
        if (slices.isEmpty()) {
            mp.errorMessageForUser.accept("Nothing to export", "There is no slice in this ABBA session.");
            return;
        }

        File zipFile = new File(output_folder, "abba_export_" + identifier + ".zip");
        if (zipFile.exists()) {
            mp.errorMessageForUser.accept("This file already exists!", zipFile.getAbsolutePath()+"\nPlease choose a different identifier or output folder.");
            return;
        }

        final SourcesProcessor preprocess;
        if (!slice_channels_csv.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(slice_channels_csv.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();
            int channelBound = slices.stream().mapToInt(slice -> slice.nChannels).min().getAsInt();
            if (maxIndex >= channelBound) {
                mp.errorMessageForUser.accept("Missing channel in slice(s).",
                        "Missing channel in slice(s)\n One slice only has " + channelBound + " channel(s).\n Maximum index : " + (channelBound - 1));
                return;
            }
            preprocess = new SourcesChannelsSelect(indices);
        } else {
            preprocess = SourcesProcessorHelper.Identity();
        }

        Task exportTask = taskService.createTask("Standardized export of "+identifier);
        exportTask.setProgressMaximum(3);
        Task createResampledData = taskService.createTask("Export slices ("+pixel_size_um+" um)");
        Task computeDeformationField = taskService.createTask("Compute deformation field ("+downscale_deformation_field+"x downscaled)");
        Task zipAll = taskService.createTask("Zip all generated files");

        // Only the sources created by this export are removed at the end
        Set<SourceAndConverter<?>> sourcesBefore = new HashSet<>(SourceServices.getSourceService().getSources());
        MultiSlicePositioner mpDS = null;

        try {
            exportTask.start();
            mp.waitForTasks();

            // Intermediate files are written in a temporary folder, only the zip is written in the output folder
            TempDirectory tempDirectory = new TempDirectory("abba_export");
            tempDirectory.deleteOnExit();
            File tempFolder = tempDirectory.getPath().toFile();

            List<File> data = new ArrayList<>();
            List<File> deformationField = new ArrayList<>();

            Atlas ba = mp.getAtlas();

            // - Resample the original slices at the target pixel size
            File[] downscaledTiffs = new File[slices.size()];

            createResampledData.setProgressMaximum(slices.size());
            createResampledData.start();

            for (SliceSources slice : slices) {
                SourceAndConverter[] sources = preprocess.apply(slice.getOriginalSources());

                SourceAndConverter<?> model = SourceHelper.getModelFusedMultiSources(sources,
                        0, 1,
                        pixel_size_um/1000.0, pixel_size_um/1000.0, pixel_size_um/1000.0,
                        1,
                        2, 2, 2, "Model");

                SourceAndConverter[] resampled = new SourceAndConverter[sources.length];
                String sliceName = "Slice_" + slice.getIndex();
                for (int i = 0; i < sources.length; i++) {
                    resampled[i] = new SourceResampler<>(sources[i], model, sliceName + "_ch" + i, true, true, true, 0).get();
                }

                String pathImage = tempFolder.getAbsolutePath() + File.separator + sliceName + ".ome.tiff";
                try {
                    OMETiffExporter.builder().put(resampled)
                            .defineMetaData(sliceName)
                            .putMetadataFromSources(sources, UNITS.MILLIMETER)
                            .voxelPhysicalSize(
                                    new Length(pixel_size_um, UNITS.MICROMETER),
                                    new Length(pixel_size_um, UNITS.MICROMETER),
                                    new Length(pixel_size_um, UNITS.MICROMETER))
                            .defineWriteOptions()
                            .lzw()
                            .tileSize(512, 512)
                            .monitor(ctx.getService(TaskService.class))
                            .nResolutionLevels(1)
                            .savePath(pathImage)
                            .create().export();
                    data.add(new File(pathImage)); // Stores data image for zipping later
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                downscaledTiffs[slice.getIndex()] = new File(pathImage);
                createResampledData.setProgressValue(createResampledData.getProgressValue()+1);
            }

            // - Rebuild the registration pipeline on the downscaled slices, in a new aligner with the same slicing
            ReslicedAtlas ra = new ReslicedAtlas(ba);
            ra.setResolution(ba.getMap().getAtlasPrecisionInMillimeter());
            ra.setSlicingTransform(mp.getReslicedAtlas().getSlicingTransform());

            mpDS = new MultiSlicePositioner(ba, ra, ctx);

            try {
                ctx.getService(CommandService.class).run(ImportSlicesFromFilesCommand.class, true,
                        "mp", mpDS,
                        "datasetname", "data",
                        "files", downscaledTiffs,
                        "split_rgb_channels", false,
                        "first_slice_position_mm", 0,
                        "slice_spacing_mm", 0.1
                ).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            mpDS.waitForTasks();

            // Copy to avoid issues with reindexing when slices are moved - the order is correct, we don't want to mess it up
            List<SliceSources> slicesCopied = new ArrayList<>(mpDS.getSlices());
            for (int iSlice = 0; iSlice < slices.size(); iSlice++) {
                SliceSources modelSlice = slices.get(iSlice);
                SliceSources slice = slicesCopied.get(iSlice);
                List<CancelableAction> modelActions = mp.getActionsFromSlice(modelSlice);
                if (modelActions == null) modelActions = new ArrayList<>();

                // Pick and apply a potential set white Pixel Background
                List<CancelableAction> setBgActions = modelActions.stream().filter(action -> action instanceof SetSliceBackgroundAction).collect(Collectors.toList());
                if (!setBgActions.isEmpty()) {
                    new SetSliceBackgroundAction(mpDS, slice, ((SetSliceBackgroundAction)setBgActions.get(setBgActions.size()-1)).getBgValue()).runRequest();
                }

                mpDS.moveSlice(slice, modelSlice.getSlicingAxisPosition());
                RegisterSlicesCopyAndApplyCommand.copyRegistration(mp, modelSlice, mpDS, slice, false);

                // One UnMirror per UnMirror of the model slice: each one hides the last visible mirroring
                for (CancelableAction action : modelActions) {
                    if (action instanceof UnMirrorSliceAction) new UnMirrorSliceAction(mpDS, slice).runRequest();
                }
            }

            mpDS.waitForTasks();

            mpDS.getReslicedAtlas().setRotateX(mp.getReslicedAtlas().getRotateX());
            mpDS.getReslicedAtlas().setRotateY(mp.getReslicedAtlas().getRotateY());

            // - Save the state of the downscaled slices
            File stateFile = new File(tempFolder, "state.abba");
            mpDS.saveState(stateFile, false);

            exportTask.setProgressValue(1);

            // - Save the deformation field of each slice
            computeDeformationField.setProgressMaximum(slices.size());
            computeDeformationField.start();

            for (SliceSources slice: mpDS.getSlices()) {
                ImagePlus resultImage = DeformationFieldToImagePlus.export(slice, 0, downscale_deformation_field, 0, 0.01, 400);
                String path = tempFolder.getAbsolutePath() + File.separator + "Slice_" + slice.getIndex() + "_deformation_field.tif";
                IJ.saveAsTiff(resultImage, path);
                deformationField.add(new File(path));
                computeDeformationField.setProgressValue(computeDeformationField.getProgressValue()+1);
            }

            exportTask.setProgressValue(2);

            // - Zip all for convenience of sharing
            exportTask.setStatusMessage("Zipping results...");

            String[] axes = ReslicedAtlas.getAxesFromCoronal(ba.getMap().getCoronalTransform(), mp.getReslicedAtlas().getSlicingTransform());

            ABBAHelper.ABBAExportMeta meta = new ABBAHelper.ABBAExportMeta();
            meta.timestamp = Instant.now().toString();
            meta.resolution_um = pixel_size_um;
            meta.atlas_name = ba.getName();
            meta.experiment_information = experiment_information;
            meta.n_slices = slices.size();
            meta.x_axis = axisChoice(axes[0]);
            meta.y_axis = axisChoice(axes[1]);
            meta.z_axis = axisChoice(axes[2]);
            meta.downscale_deformation_field = downscale_deformation_field;

            zipAll.start();
            zipAll.setProgressMaximum(data.size()+ deformationField.size()+1);

            try {
                zipAndDeleteFiles(stateFile, data, deformationField, meta, zipFile.getAbsolutePath(), zipAll);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            exportTask.setProgressValue(3);
            exportTask.setStatusMessage("Done!");
            mp.infoMessageForUser.accept("Standardized export done", zipFile.getAbsolutePath());

            success = true;
        } finally {
            if (mpDS != null) mpDS.close();
            List<SourceAndConverter<?>> sourcesCreated = SourceServices.getSourceService().getSources().stream()
                    .filter(source -> !sourcesBefore.contains(source))
                    .collect(Collectors.toList());
            SourceServices.getSourceService().remove(sourcesCreated.toArray(new SourceAndConverter[0]));

            exportTask.finish();
            createResampledData.finish();
            computeDeformationField.finish();
            zipAll.finish();
        }
    }

    /**
     * @param axis two letters code of an axis, AP, PA, RL, LR, SI or IS
     * @return the corresponding choice of the ABBA start commands, for instance "AP (Anterior-Posterior)"
     */
    static String axisChoice(String axis) {
        switch (axis) {
            case "AP": return "AP (Anterior-Posterior)";
            case "PA": return "PA (Posterior-Anterior)";
            case "SI": return "SI (Superior-Inferior)";
            case "IS": return "IS (Inferior-Superior)";
            case "RL": return "RL (Right-Left)";
            case "LR": return "LR (Left-Right)";
            default: throw new IllegalArgumentException("Unknown axis "+axis);
        }
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
        // Serialize metaobject to JSON string
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
