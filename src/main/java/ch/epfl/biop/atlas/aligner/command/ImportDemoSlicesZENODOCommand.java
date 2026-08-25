package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.DatasetHelper;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.task.Task;
import org.scijava.task.TaskService;

import java.io.File;
import java.util.Arrays;

import static ch.epfl.biop.atlas.aligner.ABBAHelper.getResource;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>ABBA - Open Demo Slices From ZENODO",
        description = "Open a set of demo brain sections",
        iconPath = "/graphics/zenodo-icon-blue.png")
public class ImportDemoSlicesZENODOCommand implements Command {

    //https://zenodo.org/records/14918378>https://zenodo.org/records/14918378
    @Parameter(style = "message", visibility = ItemVisibility.MESSAGE)
    String message =  "<html>" +
            "<h1>Import Demo Slices</h1>\n" +
            "    <p><img src='"+getResource("graphics/zenodo-gradient-200.png")+"' width='200' height='80'></img></p>" +
            "    <p>These Demo slices are hosted within Zenodo. They will be downloaded and cached during the first run (~1Gb per slide).</p>\n" +
            "    <p>For more information, please visit <a href=https://zenodo.org/records/14918378>https://zenodo.org/records/14918378</a> </p>\n" +
            "\n</html>\n";

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    CommandService cs;

    @Parameter
    TaskService taskService;

    @Parameter(label = "Number of slides to use (7 max)", max = "7", min = "1")
    int number_of_slides = 1;

    @Override
    public void run() {

        Task taskDL = taskService.createTask("Fetching Slices Data");
        try {
            mp.addTask();

            // Slides are picked around the central one, in this order: 3 4 2 5 1 6 0
            final int[] slidesByPriority = {3, 4, 2, 5, 1, 6, 0};
            int nSlides = Math.max(1, Math.min(slidesByPriority.length, number_of_slides));
            int[] slides = Arrays.copyOf(slidesByPriority, nSlides);
            Arrays.sort(slides); // keeps the slides in their anatomical order

            taskDL.setProgressMaximum(nSlides);
            taskDL.start();
            File[] files = new File[nSlides];
            for (int i = 0; i < nSlides; i++) {
                int iSlice = slides[i];
                taskDL.setStatusMessage("Download slices from slide "+iSlice);
                files[i] = new File(DatasetHelper.dowloadBrainVSIDataset(iSlice), "Slide_0"+iSlice+".vsi");
                taskDL.setProgressValue(i+1);
            }

            taskDL.setStatusMessage("Opening local files...");

            cs.run(ImportSlicesFromFilesCommand.class, true,
                        "mp", mp,
                        "datasetname", "Zenodo Demo Sections ("+nSlides+" Slides)",
                        "files", files,
                        "split_rgb_channels", false,
                        "slice_axis_initial_mm", 0,
                        "increment_between_slices_mm", 0.08
                    ).get();

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

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            mp.removeTask();
            taskDL.finish();
        }

    }

}
