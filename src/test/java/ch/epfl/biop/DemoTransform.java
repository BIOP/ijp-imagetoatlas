package ch.epfl.biop;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.struct.Atlas;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.io.File;

public class DemoTransform {

    public static void main(String[] args) throws Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        Atlas mouseAtlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command()
                .run(ABBAStartCommand.class, true,
                        "ba", mouseAtlas,
                        "slicing_mode", "coronal").get().getOutput("mp"));

        BdvHandle bdvh = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        BdvMultislicePositionerView view = new BdvMultislicePositionerView(mp, bdvh);


        mp.loadState(new File("C:\\Users\\chiarutt\\Desktop\\TestTheresa\\state-affineandspline.json"));

        SliceSources slice0 = mp.getSlices().get(0);
        SliceSources slice10 = mp.getSlices().get(10);

        BdvHandle bdvh2 = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        SourceAndConverterServices
                .getBdvDisplayService()
                .show(bdvh2, slice0.getOriginalSources());

        /*SourceAndConverterServices
                .getBdvDisplayService()
                .show(bdvh2, slice10.getRegisteredSources());*/

        AffineTransform3D at3D = mp.getAffineTransformFromAlignerToAtlas();

        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();

        slice10.getSlicePixToCCFRealTransform();

        SourceAffineTransformer sat = new SourceAffineTransformer(null, at3D);



        for (SourceAndConverter sac : slice10.getRegisteredSources()) {
            SourceAndConverter source = sat.apply(sac);
            //sac_service.register(source);
            //sac_service.setMetadata(source, "ABBA", tag);
            //sacsToAppend.add(source);
            SourceAndConverterServices
                    .getBdvDisplayService()
                    .show(bdvh2, source);
        }


    }
}
