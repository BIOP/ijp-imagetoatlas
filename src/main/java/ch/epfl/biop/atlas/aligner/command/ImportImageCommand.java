package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.bdv.bioformats.command.BasicOpenFilesWithBigdataviewerBioformatsBridgeCommand;
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

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>ABBA - Import With Bio-Formats",
        description = "Import a Bio-Formats compatible file as brain slices")
public class ImportImageCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Files to import")
    File[] files;

    @Parameter(label = "Split RGB channels")
    boolean split_rgb_channels = false;

    @Parameter(label = "Initial axis position (0 = front, mm units)")
    double slice_axis_initial;

    @Parameter(label = "Axis increment between slices (mm, can be negative for reverse order)")
    double increment_between_slices;

    @Parameter
    CommandService command_service;

    @Parameter
    SourceAndConverterService sac_service;

    @Override
    public void run() {
        try {
            for (File f : files) {
                AbstractSpimData<?> spimdata = (AbstractSpimData<?>)
                        command_service.run(
                                    BasicOpenFilesWithBigdataviewerBioformatsBridgeCommand.class,
                                    true, "files", new File[]{f},
                                    "unit", "MILLIMETER",
                                    "splitrgbchannels", split_rgb_channels
                                )
                                .get()
                                .getOutput("spimData");

                SourceAndConverter[] sacs =
                        sac_service.getSourceAndConverterFromSpimdata(spimdata)
                                .toArray(new SourceAndConverter[0]);

                List<SliceSources> slices = mp.createSlice(sacs, slice_axis_initial, increment_between_slices, Tile.class, new Tile(-1));

                slice_axis_initial += (slices.size()+1)* increment_between_slices;

            }
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
