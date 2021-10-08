package ch.epfl.biop.abba;

import ch.epfl.biop.atlas.aligner.commands.SacMultiSacsPositionerCommand;
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

        ij.command().run(SacMultiSacsPositionerCommand.class, true).get();
        //ij.object().getObjects()

    }
}
