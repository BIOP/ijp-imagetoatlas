package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Registered Slices to BDV Json Dataset (Experimental)",
        description = "Export registered slices as a BigDataViewer json dataset (very experimental).")
public class ExportSlicesToBDVJsonDatasetCommand implements Command {

    @Parameter(label = "Please specify a json file to store the reconstructed data")
    File dataset_file;

    @Parameter(label = "Enter a tag to identify the registered sources (metadata key = \"ABBA\")" )
    String tag;

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    Context ctx;

    @Parameter
    SourceAndConverterService sac_service;

    @Override
    public void run() {
        mp.log.accept("Waiting for the end of all current tasks...");
        mp.waitForTasks();
        mp.log.accept("All tasks ended");
        mp.log.accept("Starting saving...");
        List<SourceAndConverter> sacs = new ArrayList<>();
        List<SliceSources> slices = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (slices.size()==0) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice to save");
        } else {
            AffineTransform3D at3D = mp.getAffineTransformFormAlignerToAtlas();
            SourceAffineTransformer sat = new SourceAffineTransformer(null, at3D);
            slices.forEach(slice -> {
                for (SourceAndConverter sac : slice.getRegisteredSources()) {
                    SourceAndConverter source = sat.apply(sac);
                    sac_service.register(source);
                    sac_service.setMetadata(source, "ABBA", tag);
                    sacs.add(source);
                }
            });
            new SourceAndConverterServiceSaver(dataset_file, ctx, sacs).run();

            mp.log.accept("Saved!");
        }
    }

}