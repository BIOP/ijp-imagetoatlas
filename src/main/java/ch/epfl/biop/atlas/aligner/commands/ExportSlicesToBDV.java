package ch.epfl.biop.atlas.aligner.commands;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Registered Slices to BDV",
        description = "Export registered slices to a BigDataViewer window.")
public class ExportSlicesToBDV implements Command {

    @Parameter(label = "Select the bdv to append the file - only one")
    BdvHandle[] bdvh;

    @Parameter(label = "Enter a tag to identify the registered sources (metadata key = \"ABBA\")" )
    String tag;

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    SourceAndConverterService sac_service;

    @Override
    public void run() {
        if ((mp.hasGUI())&&(bdvh[0].equals(mp.getBdvh()))) {
            mp.errorMessageForUser.accept("Error","Do not use the same window as ABBA!");
            return;
        }
        mp.log.accept("Waiting for the end of all current tasks...");
        mp.waitForTasks();
        mp.log.accept("All tasks ended");

        List<SliceSources> slices = mp.getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (slices.size()==0) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice to export");
        } else {
            List<SourceAndConverter> sacsToAppend = new ArrayList<>();
            AffineTransform3D at3D = mp.getAffineTransformFormAlignerToAtlas();
            SourceAffineTransformer sat = new SourceAffineTransformer(null, at3D);
            slices.forEach(slice -> {
                for (SourceAndConverter sac : slice.getRegisteredSources()) {
                    SourceAndConverter source = sat.apply(sac);
                    sac_service.register(source);
                    sac_service.setMetadata(source, "ABBA", tag);
                    sacsToAppend.add(source);
                }
            });

            SourceAndConverterServices
                    .getBdvDisplayService()
                    .show(bdvh[0], sacsToAppend.toArray(new SourceAndConverter[0]));
        }
    }

}