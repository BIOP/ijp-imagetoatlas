package ch.epfl.biop;

import ch.epfl.biop.atlas.ABBACommand;
import net.imagej.ImageJ;

public class ABBALaunch {

    public static void main(String[] args) throws Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ij.command().run(ABBACommand.class, true).get();

    }
}
