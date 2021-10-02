package ch.epfl.biop.abba;

import ch.epfl.biop.atlas.allen.adultmousebrain.ABBACommandAdultMouseAllenBrainCCFv3;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;

public class ABBALaunchMouse {
    static {
        LegacyInjector.preinit();
    }
    public static void main(String[] args) throws Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        //DebugTools.setRootLevel("off");
        //DebugTools.setRootLevel();

        ij.command().run(ABBACommandAdultMouseAllenBrainCCFv3.class, true).get();
        //ij.object().getObjects()

    }
}
