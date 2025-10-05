package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.bdv.img.omero.command.OmeroConnectCommand;
import ch.epfl.biop.bdv.img.qupath.command.CreateBdvDatasetQuPathCommand;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>ABBA - Open Demo Slices (web)",
        description = "Open a set of demo brain sections")
public class ABBAImportDemoSlicesCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(choices = {"25 sections", "97 sections"})
    String demo_dataset;

    @Parameter
    CommandService cs;

    @Parameter
    SourceAndConverterService source_service;

    @Override
    public void run() {
        try {

            File qupathProjectFile;

            System.gc();
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
            cs.run(OmeroConnectCommand.class, true,
                    "host", "omero-tim.gerbi-gmb.de",
                    "port", 4064,
                    "username", "read-tim",
                    "password", "read-tim"
            ).get();

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

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
