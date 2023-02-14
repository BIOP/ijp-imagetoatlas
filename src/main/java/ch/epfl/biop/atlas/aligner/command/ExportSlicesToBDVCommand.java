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
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Registered Slices to BDV",
        description = "Export registered slices to a BigDataViewer window.")
public class ExportSlicesToBDVCommand implements Command {

    @Parameter(label = "Enter a tag to identify the registered sources (metadata key = \"ABBA\")" )
    String tag;

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    SourceAndConverterService sac_service;

    @Override
    public void run() {
        mp.log.accept("Waiting for the end of all current tasks...");
        mp.waitForTasks();
        mp.log.accept("All tasks ended");

        List<SliceSources> slices = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slices.size()==0) {
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

            BdvHandle bdvh = SourceAndConverterServices
                    .getBdvDisplayService().getNewBdv();

            SourceAndConverterServices
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