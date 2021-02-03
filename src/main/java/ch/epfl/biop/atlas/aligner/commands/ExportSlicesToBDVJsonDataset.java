package ch.epfl.biop.atlas.aligner.commands;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>Export Slices to BDV Json Dataset (Experimental)")
public class ExportSlicesToBDVJsonDataset implements Command {

    @Parameter
    File datasetFile;

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    Context ctx;

    @Override
    public void run() {
        mp.log.accept("Waiting for the end of all current tasks...");
        mp.waitForTasks();
        mp.log.accept("All tasks ended");
        mp.log.accept("Starting saving...");
        List<SourceAndConverter> sacs = new ArrayList<>();
        List<SliceSources> slices = mp.getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (slices.size()==0) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice to save");
        } else {
            slices.forEach(slice -> {
                for (SourceAndConverter sac : slice.getRegisteredSources()) {
                    sacs.add(sac);
                }
            });
            new SourceAndConverterServiceSaver(datasetFile, ctx, sacs).run();
            mp.log.accept("Saved!");
        }
    }

}