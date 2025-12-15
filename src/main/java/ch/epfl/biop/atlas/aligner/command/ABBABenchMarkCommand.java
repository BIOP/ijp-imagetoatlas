package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvStartCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr.command.AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.bdv.img.omero.OmeroChecker;
import ch.epfl.biop.bdv.img.omero.command.OmeroConnectCommand;
import ch.epfl.biop.bdv.img.qupath.command.CreateBdvDatasetQuPathCommand;
import ij.IJ;
import ij.Prefs;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.task.Task;
import org.scijava.task.TaskService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>ABBA - Run Benchmark",
        description = "Complete ABBA process in a benchmark")
public class ABBABenchMarkCommand implements Command {

    @Parameter
    String comment;

    @Parameter(choices = {"25 sections", "97 sections"})
    String demo_dataset;

    @Parameter(description = "Slower when check - wait for all sections to have finished their registration steps before starting the next one.")
    boolean wait_between_each_step;

    @Parameter
    Context ctx;

    @Parameter
    CommandService cs;

    @Parameter
    SourceAndConverterService source_service;

    @Parameter(label = "Use graphical user interface")
    boolean use_gui;

    @Parameter(type = ItemIO.OUTPUT)
    String report;

    @Parameter
    TaskService taskService;

    @Override
    public void run() {

        OmeroChecker.PromptUserIfOmeroDependenciesMissing(ctx);

        // Check whether the benchmark can work TODO:
        // - Elastix and transformix set
        // - Access to internet (to DL the OMERO public dataset)
        // - OMERO dependencies present (OMERO 5.5-5.6)
        // - DeepSlice set

        // Collect info of current configuration:
        // - What is in the ask for help command
        // - Number of threads
        // - Execution date

        Task task = taskService.createTask("ABBA Benchmark - "+demo_dataset+ " | ");
        task.start();
        try {

            File qupathProjectFile;

            System.gc();
            task.setStatusMessage("Download QuPath Project...");
            startTiming("Download QuPath Project");
            switch (demo_dataset) {
                case "25 sections":
                    qupathProjectFile = ABBAHelper.getTempQPProject("https://zenodo.org/records/14918378/files/abba-omero-gerbi-subset.zip");
                    break;
                case "97 sections":
                    qupathProjectFile = ABBAHelper.getTempQPProject("https://zenodo.org/records/14918378/files/abba-omero-gerbi-full.zip");
                    break;
                default:
                    throw new IllegalArgumentException("Demo dataset not recognized: " + demo_dataset);
            }
            endTiming("Download QuPath Project");

            task.setStatusMessage("Connect to OMERO Server...");
            startTiming("Connect to OMERO Server");
            cs.run(OmeroConnectCommand.class, true,
                    "host", "omero-tim.gerbi-gmb.de",
                    "port", 4064,
                    "username", "read-tim",
                    "password", "read-tim"
            ).get();
            endTiming("Connect to OMERO Server");


            task.setStatusMessage("Create BDV Dataset...");
            startTiming("Create BDV Dataset");
            cs.run(CreateBdvDatasetQuPathCommand.class, true,
                    "qupath_project", qupathProjectFile,
                    "datasetname", "gerbi-omero-project",
                    "unit", "MILLIMETER",
                    "split_rgb_channels", false,
                    "plane_origin_convention", "[TOP LEFT]").get();

            SourceAndConverterServiceUI treeUI = source_service.getUI();
            List<SourceAndConverter<?>[]> groupedSources = new ArrayList<>();
            treeUI.getRoot().child("gerbi-omero-project").child("ImageName").children().forEach(imageNameNode -> {
                SourceAndConverter<?>[] sources = imageNameNode.sources();
                groupedSources.add(sources);
            });
            endTiming("Create BDV Dataset");


            task.setStatusMessage("Load Atlas and Initialize Positioner...");
            startTiming("Load Atlas and Initialize Positioner");
            Atlas atlas = (Atlas) cs.run(AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.class, true).get().getOutput("ba");
            MultiSlicePositioner mp;
            BdvMultislicePositionerView view = null;

            if (use_gui) {
                view = ((BdvMultislicePositionerView) cs.run(ABBABdvStartCommand.class, true,
                        "ba", atlas,
                        "x_axis", "RL",
                        "y_axis", "SI",
                        "z_axis", "AP"
                ).get().getOutput("view"));
                mp = view.msp;
            } else {
                mp = (MultiSlicePositioner) (cs.run(ABBAStartCommand.class, true,
                        "ba", atlas,
                        "x_axis", "RL",
                        "y_axis", "SI",
                        "z_axis", "AP"
                ).get().getOutput("mp"));
            }
            endTiming("Load Atlas and Initialize Positioner");


            task.setStatusMessage("Import Sources into ABBA...");
            startTiming("Import Sources into ABBA");
            for (int index = 0; index < groupedSources.size(); index++) {
                cs.run(ImportSliceFromSourcesCommand.class, true,
                        "mp", mp,
                        "slice_axis_mm", 4.0 + index * 1.0, // False initial guess
                        "sources", groupedSources.get(index)
                ).get();
            }

            mp.getSlices().forEach(SliceSources::select);
            cs.run(SetSlicesDisplayRangeCommand.class, true,
                    "mp", mp,
                    "channels_csv", "0",
                    "display_min", 0.0,
                    "display_max", 800.0
            ).get();
            cs.run(SetSlicesDisplayRangeCommand.class, true,
                    "mp", mp,
                    "channels_csv", "1",
                    "display_min", 0.0,
                    "display_max", 1024.0
            ).get();

            if (use_gui) {
                mp.getSlices().forEach(slice -> slice.setDisplayColor(0, 0, 255, 255, 0));
                assert view != null;
                view.setSelectedSlicesVisibility(true);
                view.setSelectedSlicesVisibility(0, true);
                view.setSelectedSlicesVisibility(2, true);
            }
            endTiming("Import Sources into ABBA");

            if (!wait_between_each_step) {
                task.setStatusMessage("Registration Steps and Export...");
                startTiming("Registration Steps and Export");
            }

            int n_deepslice_runs = 2;
            for (int idxDeepSliceRun = 0; idxDeepSliceRun < n_deepslice_runs; idxDeepSliceRun++) {
                if (wait_between_each_step) {
                    task.setStatusMessage("DeepSlice Registration (Local) Run " + (idxDeepSliceRun + 1));
                    startTiming("DeepSlice Registration (Local) Run " + (idxDeepSliceRun + 1));
                }
                cs.run(RegisterSlicesDeepSliceLocalCommand.class, true,
                        "mp", mp,
                        "channels", "0,1",
                        "model", "mouse",
                        "allow_slicing_angle_change", true,
                        "ensemble", false,
                        "post_processing", "Keep order + ensure regular spacing",
                        "slices_spacing_micrometer", -1.0,
                        "px_size_micron", 30
                ).get();
                if (wait_between_each_step) {
                    mp.waitForTasks();
                    endTiming("DeepSlice Registration (Local) Run " + (idxDeepSliceRun + 1));
                }
            }

            if (wait_between_each_step) {
                task.setStatusMessage("Elastix Registration (Affine)...");
                startTiming("Elastix Registration (Affine)");
            }
            cs.run(RegisterSlicesElastixAffineCommand.class, true,
                    "mp", mp,
                    "channels_atlas_csv", "0,1",
                    "channels_slice_csv", "0,1",
                    "pixel_size_micrometer", 40.0,
                    "show_imageplus_registration_result", false,
                    "background_offset_value_moving", 0.0
            ).get();

            if (wait_between_each_step) {
                mp.waitForTasks();
                endTiming("Elastix Registration (Affine)");
            }

            if (wait_between_each_step) {
                task.setStatusMessage("Elastix Registration (Spline)...");
                startTiming("Elastix Registration (Spline)");
            }

            cs.run(RegisterSlicesElastixSplineCommand.class, true,
                    "mp", mp,
                    "channels_atlas_csv", "0,1",
                    "channels_slice_csv", "0,1",
                    "nb_control_points_x", 12,
                    "pixel_size_micrometer", 20.0,
                    "background_offset_value_moving", 0.0,
                    "show_imageplus_registration_result", false
            ).get();

            if (wait_between_each_step) {
                mp.waitForTasks();
                endTiming("Elastix Registration (Spline)");
            }

            if (wait_between_each_step) {
                task.setStatusMessage("Export Registrations to QuPath Project...");
                startTiming("Export Registrations to QuPath Project");
            }

            cs.run(ExportRegistrationToQuPathCommand.class, true,
                    "mp", mp,
                    "erase_previous_file", true
            ).get();

            if (wait_between_each_step) {
                mp.waitForTasks();
                endTiming("Export Registrations to QuPath Project");
            } else {
                mp.waitForTasks();
                endTiming("Registration Steps and Export");
            }

            report = getReport();
            IJ.log(report);

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            task.finish();
        }


    }

    private final Map<String, Long> timings = new LinkedHashMap<>();
    private final Map<String, Long> memoryUsage = new LinkedHashMap<>();

    private long totalTime = 0;

    private void startTiming(String stepName) {
        timings.put(stepName, System.nanoTime());
    }

    private void endTiming(String stepName) {
        long endTime = System.nanoTime();
        long startTime = timings.get(stepName);
        long elapsedTime = endTime - startTime;
        totalTime += elapsedTime;
        timings.put(stepName, elapsedTime);

        // Capture RAM usage
        System.gc();
        long ramUsage = getCurrentRamUsage();
        memoryUsage.put(stepName, ramUsage);
    }

    private long getCurrentRamUsage() {
        // Runtime runtime = Runtime.getRuntime();
        return IJ.currentMemory() / (1024 * 1024); // Convert to MB
    }

    private String getReport() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String formattedDateTime = now.format(formatter);

        long maxMemory = Runtime.getRuntime().maxMemory();
        long maxMemoryMB = maxMemory / (1024 * 1024); // Convert to MB

        StringBuilder builder = new StringBuilder();

        builder.append("# ABBA Benchmark\n\n");

        builder.append("- Date and Time: " + formattedDateTime + "\n");

        builder.append("- Comments: "+comment+"\n");

        builder.append("- Dataset: "+this.demo_dataset+"\n");

        builder.append("- Number of threads: "+Prefs.getThreads()+"\n");

        builder.append("- Max Allocatable Memory: " + maxMemoryMB + " MB\n\n");

        builder.append("# Software configuration\n\n");

        builder.append(ABBAForumHelpCommand.getConfigInfos("\n"));

        builder.append("\n# Result\n\n");

        builder.append("| Step | Time (%) | RAM Usage (MB) |\n");
        builder.append("|------|----------|----------------|\n");

        for (Map.Entry<String, Long> entry : timings.entrySet()) {
            double percentage = (entry.getValue().doubleValue() / totalTime) * 100;
            // Round to one significant digit
            double roundedPercentage = Math.round(percentage * 10.0) / 10.0;
            long ramUsage = memoryUsage.get(entry.getKey());
            builder.append(String.format("| %s | %.1f | %d |\n", entry.getKey(), roundedPercentage, ramUsage));
        }

        double totalTimeSeconds = totalTime / 1e9; // Convert nanoseconds to seconds
        builder.append(String.format("\nTotal Time: %.1f seconds\n", totalTimeSeconds));

        return builder.toString();
    }

}
