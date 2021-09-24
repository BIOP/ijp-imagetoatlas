package ch.epfl.biop.abba;

import ch.epfl.biop.atlas.ABBACommand;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import net.imagej.ImageJ;
import java.io.File;

public class DemoDeSerialization {

	public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        MultiSlicePositioner mp = (MultiSlicePositioner) ij.command().run(ABBACommand.class, true).get().getOutput("mp");

        mp.loadState(new File("src/test/resources/ij1registration.json"));
        //mp.loadState(new File("C:\\Users\\nicol\\Desktop\\sliceregsave\\qpathprojtest.json"));
	}

}