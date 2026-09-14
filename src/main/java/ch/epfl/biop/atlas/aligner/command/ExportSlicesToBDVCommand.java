package ch.epfl.biop.atlas.aligner.command;

import bdv.util.BdvHandle;
import bdv.util.source.alpha.IAlphaSource;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.SliceGuiState;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.service.SourceService;
import sc.fiji.bdvpg.service.SourceServices;
import sc.fiji.bdvpg.source.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Registered Slices to BDV",
        description = "Shows the registered selected slices, placed in atlas coordinates, in a new BigDataViewer window. "
                + "The sources are also added to the BigDataViewer-Playground source tree. Waits for all ABBA tasks to be done.",
        iconPath = "/graphics/ABBAExportBDV.png")
public class ExportSlicesToBDVCommand implements Command {

    @Parameter(label = "Tag",
            description = "Text stored as the 'ABBA' metadata of each exported source, to identify them later in scripts." )
    String tag;

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter
    SourceService sac_service;

    @Override
    public void run() {
        mp.infoMessageForUser.accept("", "Waiting for the end of all current tasks...");
        mp.waitForTasks();
        mp.infoMessageForUser.accept("", "All tasks ended");

        List<SliceSources> slices = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slices.isEmpty()) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice to export");
        } else {
            //slices.forEach(SliceSources::setAlphaSources); // Should make things faster
            List<SourceAndConverter> sacsToAppend = new ArrayList<>();
            AffineTransform3D at3D = mp.getAffineTransformFromAlignerToAtlas();
            SourceAffineTransformer sat = new SourceAffineTransformer(null, at3D);
            slices.forEach(slice -> {
                for (SourceAndConverter sac : slice.getRegisteredSources()) {
                    SourceAndConverter source = sat.apply(alphaCulledSources(new SourceAndConverter[]{sac}, slice.getAlpha())[0]);
                    sac_service.register(source);
                    sac_service.setMetadata(source, "ABBA", tag);
                    sacsToAppend.add(source);

                }
            });

            BdvHandle bdvh = SourceServices
                    .getBdvDisplayService().getNewBdv();

            SourceServices
                    .getBdvDisplayService()
                    .show(bdvh, sacsToAppend.toArray(new SourceAndConverter[0]));
        }
    }

    private static SourceAndConverter[] alphaCulledSources(SourceAndConverter[] sources, IAlphaSource alpha) {
        SourceAndConverter[] alphaCulled = new SourceAndConverter[sources.length];
        for (int i = 0; i<alphaCulled.length; i++) {
            SourceAndConverter ori = sources[i];
            if (ori.asVolatile()!=null) {
                SourceAndConverter sac = new SourceAndConverter(
                        new SliceGuiState.AlphaCulledSource(ori.getSpimSource(), alpha),
                        ori.getConverter(),
                        new SourceAndConverter(new SliceGuiState.AlphaCulledSource(ori.asVolatile().getSpimSource(), alpha),
                                ori.asVolatile().getConverter()));
                alphaCulled[i] = sac;
            } else {
                SourceAndConverter sac = new SourceAndConverter(
                        new SliceGuiState.AlphaCulledSource(ori.getSpimSource(), alpha),
                        ori.getConverter());
                alphaCulled[i] = sac;
            }
        }
        return alphaCulled;
    }

}