package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.bdv.img.bioformats.command.CreateBdvDatasetBioFormatsCommand;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.Tile;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>ABBA - Import With Bio-Formats",
        description = "Import a Bio-Formats compatible file as brain slices")
public class ImportSlicesFromFilesCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Dataset Name")
    String datasetname;

    @Parameter(label = "Files to import")
    File[] files;

    @Parameter(label = "Split RGB channels")
    boolean split_rgb_channels = false;

    @Parameter(label = "Initial axis position (0 = front, mm units)", style="format:0.000", stepSize = "0.1")
    double slice_axis_initial_mm;

    @Parameter(label = "Axis increment between slices (mm, can be negative for reverse order)", style="format:0.000", stepSize = "0.02")
    double increment_between_slices_mm;

    @Parameter
    CommandService command_service;

    @Parameter
    SourceAndConverterService sac_service;

    @Override
    public void run() {
        try {
            AbstractSpimData<?> spimdata = (AbstractSpimData<?>)
                    command_service.run(
                                CreateBdvDatasetBioFormatsCommand.class,
                                true, "files", files,
                                "datasetname", datasetname,
                                "unit", "MILLIMETER",
                                "split_rgb_channels", split_rgb_channels,
                                "plane_origin_convention", "TOP LEFT"
                            )
                            .get()
                            .getOutput("spimdata");

            SourceAndConverter[] sacs =
                    sac_service.getSourceAndConverterFromSpimdata(spimdata)
                            .toArray(new SourceAndConverter[0]);

            List<SliceSources> slices = mp.createSlice(sacs, slice_axis_initial_mm, increment_between_slices_mm, Tile.class, new Tile(-1));

            slice_axis_initial_mm += (slices.size()+1)* increment_between_slices_mm;

            mp.selectSlice(mp.getSlices());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }  catch (ExecutionException e) {
            mp.errorMessageForUser.accept("Image Import Error",
                    "An image couldn't be imported.");
            e.printStackTrace();
        }
    }

}
