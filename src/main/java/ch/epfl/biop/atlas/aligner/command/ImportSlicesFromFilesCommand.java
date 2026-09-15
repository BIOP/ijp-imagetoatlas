package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.bdv.img.bioformats.command.DatasetFromBioFormatsCreateCommand;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.Tile;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.service.SourceService;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>ABBA - Import With Bio-Formats",
        description = "Imports images from Bio-Formats compatible files (vsi, czi, ome.tiff, lif...) as slices. "
                + "Each image series of each file becomes one slice, all channels included. The imported slices are selected.",
        iconPath = "/graphics/ImportSlicesFromFiles.png")
public class ImportSlicesFromFilesCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Dataset name",
            description = "Name of the dataset grouping the imported images in the BigDataViewer-Playground source tree.")
    String datasetname;

    @Parameter(label = "Files to import",
            description = "Image files to open with Bio-Formats. Files should have a pixel size calibration.")
    File[] files;

    @Parameter(label = "Split RGB channels",
            description = "If checked, RGB images are split into three channels (red, green, blue); otherwise they are kept as a single RGB channel.")
    boolean split_rgb_channels = false;

    @Parameter(label = "Position of the first slice (mm)", style="format:0.000", stepSize = "0.1",
            description = "Initial position of the first imported slice along the atlas slicing axis, in mm, as the Z shown in the slice information (0 = front of the atlas). "
                    + "Positions can be adjusted later, for instance with DeepSlice.")
    double first_slice_position_mm;

    @Parameter(label = "Spacing between slices (mm)", style="format:0.000", stepSize = "0.02",
            description = "Distance between consecutive imported slices along the slicing axis, in mm. Use a negative value to reverse their order.")
    double slice_spacing_mm;

    @Parameter
    CommandService command_service;

    @Parameter
    SourceService sac_service;

    @Override
    public void run() {
        try {
            AbstractSpimData<?> spimdata = (AbstractSpimData<?>)
                    command_service.run(
                                DatasetFromBioFormatsCreateCommand.class,
                                true, "files", files,
                                "datasetname", datasetname,
                                "unit", "MILLIMETER",
                                "split_rgb_channels", split_rgb_channels,
                                "plane_origin_convention", "TOP LEFT",
                                "auto_pyramidize", true,
                                "disable_memo", false
                            )
                            .get()
                            .getOutput("spimdata");

            SourceAndConverter[] sacs =
                    sac_service.getSourcesFromDataset(spimdata)
                            .toArray(new SourceAndConverter[0]);

            List<SliceSources> slices = mp.createSlice(sacs, mp.fromAtlasZ(first_slice_position_mm), slice_spacing_mm, Tile.class, new Tile(-1));


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
