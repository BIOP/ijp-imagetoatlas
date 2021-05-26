package ch.epfl.biop;

import ch.epfl.biop.atlas.ABBACommand;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;

public class ABBALaunch {
    static {
        LegacyInjector.preinit();
    }
    public static void main(String[] args) throws Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        //DebugTools.setRootLevel("off");
        //DebugTools.setRootLevel();

        ij.command().run(ABBACommand.class, true).get();


    }
}
