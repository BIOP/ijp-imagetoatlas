package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.bdv.img.qupath.command.DatasetFromQuPathCreateCommand;
import ch.epfl.biop.bdv.img.qupath.entity.QuPathEntryIdEntity;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.service.SourceService;

import java.io.File;
import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>ABBA - Import QuPath Project",
        description = "Imports all images of a QuPath project as slices, one slice per project image, all channels included. "
                + "Registrations can then be exported back to the QuPath project. "
                + "Do not add or remove images in the QuPath project after the import.",
        iconPath = "/graphics/ImportSlicesFromQuPath.png")
public class ImportSlicesFromQuPathCommand implements Command {

    @Parameter(style = "message", visibility = ItemVisibility.MESSAGE)
    String message = "<html><b>WARNING:</b> Do not delete or add any image in <br>"+
            " the QuPath project after it has been imported in ABBA!";
    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "QuPath project file (.qpproj)",
            description = "The project.qpproj file of the QuPath project to import.")
    File qupath_project;

    @Parameter(label = "Position of the first slice (mm)", style="format:0.000", stepSize = "0.1",
            description = "Initial position of the first imported slice along the atlas slicing axis, in mm, as the Z shown in the slice information (0 = front of the atlas). "
                    + "Positions can be adjusted later, for instance with DeepSlice.")
    double first_slice_position_mm;

    @Parameter(label = "Spacing between slices (mm)", style="format:0.000", stepSize = "0.01",
            description = "Distance between consecutive imported slices along the slicing axis, in mm. "
                    + "Use a negative value to reverse their order. Cannot be 0.")
    double slice_spacing_mm;

    @Parameter
    CommandService command_service;

    @Parameter
    SourceService sac_service;

    @Override
    public void run() {
        try {
            if (slice_spacing_mm == 0) {
               mp.errorMessageForUser.accept("Spacing between slices: 0", "Please specify a non-zero increment between slices.");
               return;
            }
            AbstractSpimData<?> spimdata = (AbstractSpimData<?>) command_service
                    .run(DatasetFromQuPathCreateCommand.class,true,
                            "qupath_project", qupath_project,
                            "unit", "MILLIMETER").get().getOutput("spimData");
            SourceAndConverter<?>[] sacs =
                    sac_service.getSourcesFromDataset(spimdata)
                            .toArray(new SourceAndConverter[0]);

            if (sacs.length>0) { // Because the action could have been canceled
                mp.createSlice(sacs, first_slice_position_mm + mp.getReslicedAtlas().getZOffset(), slice_spacing_mm, QuPathEntryIdEntity.class, new QuPathEntryIdEntity(-1));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            mp.errorMessageForUser.accept("QuPath Import Error",
                    "QuPath project couldn't be imported.\n"+
                       "Check whether the project can be opened in QuPath, fix URI if necessary.\n"+
                       "OpenSlide and ImageJ image servers are unsupported.");
            e.printStackTrace();
        }
    }

}
