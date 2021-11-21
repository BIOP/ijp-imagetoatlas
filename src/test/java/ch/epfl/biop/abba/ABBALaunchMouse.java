package ch.epfl.biop.abba;

import bdv.util.BdvHandle;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.command.AllenBrainAdultMouseAtlasCCF2017Command;
import ch.epfl.biop.atlas.struct.Atlas;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

public class ABBALaunchMouse {
    static {
        LegacyInjector.preinit();
    }
    public static void main(String[] args) throws Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        Atlas mouseAtlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017Command.class, true).get().getOutput("ba");

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command()
                .run(ABBAStartCommand.class, true,
                        "ba", mouseAtlas,
                        "slicing_mode", "coronal").get().getOutput("mp"));

        BdvHandle bdvh = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        BdvMultislicePositionerView view = new BdvMultislicePositionerView(mp, bdvh);

    }
}
