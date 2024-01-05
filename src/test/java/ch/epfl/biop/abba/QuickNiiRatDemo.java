package ch.epfl.biop.abba;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.command.WaxholmSpragueDawleyRatV4p2Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.bdv.img.bioformats.command.CreateBdvDatasetBioFormatsCommand;
import ch.epfl.biop.quicknii.QuickNIISeries;
import com.google.gson.Gson;
import loci.common.DebugTools;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.io.File;
import java.io.FileReader;
import java.util.List;

// Pfou - do not work
public class QuickNiiRatDemo {

    public static void main(String... args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        DebugTools.setRootLevel("off");
        Atlas ratAtlas = (Atlas) ij.command().run(WaxholmSpragueDawleyRatV4p2Command.class, true).get().getOutput("ba");

        String path = "src/test/resources/quicknii/rat/";

        QuickNIISeries series = new Gson().fromJson(new FileReader(path+"2024-01-05_results.json"), QuickNIISeries.class);

        File[] files = series.slices.stream()
                .map(slice -> new File(path, slice.filename))
                .toArray(File[]::new);

        // Creates a spimdata object
        AbstractSpimData asd = (AbstractSpimData) ij.command().run(CreateBdvDatasetBioFormatsCommand.class,true,
                "unit", "MILLIMETER",
                "split_rgb_channels", false,
                "files", files,
                "datasetname", "rat demo deepslice",
                "plane_origin_convention", "TOP LEFT" // or TOP LEFT
        ).get().getOutput("spimdata");

        // Retrieve sources from the spimdata
        List<SourceAndConverter<?>> sources = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(asd);

        for (int i = 0; i < sources.size(); i++) {
            QuickNIISeries.SliceInfo slice = series.slices.get(i);
            SourceAndConverter source = sources.get(i);

            AffineTransform3D toCCFv3 = QuickNIISeries.getTransform(ratAtlas.getName(), slice,
                    (double) source.getSpimSource().getSource(0, 0).dimension(0)/1000., // because the default pixel size is now 1 micrometer...
                    (double) source.getSpimSource().getSource(0, 0).dimension(1)/1000.);

            SourceTransformHelper.append(toCCFv3,
                                new SourceAndConverterAndTimeRange(source, 0));

        }

        BdvHandle bdvh = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        SourceAndConverterServices.getBdvDisplayService().show(bdvh, ratAtlas.getMap().getStructuralImages().values().toArray(new SourceAndConverter[0]));
        SourceAndConverterServices.getBdvDisplayService().show(bdvh, sources.toArray(new SourceAndConverter[0]));

    }

}
