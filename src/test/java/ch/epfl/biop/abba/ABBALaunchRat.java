package ch.epfl.biop.abba;

import bdv.util.BdvHandle;
import ch.epfl.biop.atlas.aligner.DebugView;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.command.WaxholmSpragueDawleyRatV4p2Command;
import ch.epfl.biop.atlas.struct.Atlas;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

public class ABBALaunchRat {
    static {
        LegacyInjector.preinit();
    }
    public static void main(String[] args) throws Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        DebugTools.enableLogging ("OFF");

        Atlas ratAtlas = (Atlas) ij.command().run(WaxholmSpragueDawleyRatV4p2Command.class, true).get().getOutput("ba");

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command()
                .run(ABBAStartCommand.class, true,
                        "ba", ratAtlas).get().getOutput("mp"));

        BdvHandle bdvh = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        BdvMultislicePositionerView view = new BdvMultislicePositionerView(mp, bdvh);

        DebugView dv = new DebugView(mp);
        //DebugView.instance = dv;

    }
}
