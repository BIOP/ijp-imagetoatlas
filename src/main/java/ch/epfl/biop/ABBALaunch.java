package ch.epfl.biop;

import ch.epfl.biop.atlas.ABBACommand;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;

public class ABBALaunch {
    static {
        LegacyInjector.preinit();
    }
    public static void main(String[] args) throws Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ij.command().run(ABBACommand.class, true).get();

    }
}
