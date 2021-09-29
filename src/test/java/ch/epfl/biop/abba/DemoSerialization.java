package ch.epfl.biop.abba;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.ABBACommandAdultMouseAllenBrainCCFv3;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.bdv.command.importer.SourceFromImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

import java.io.File;

public class DemoSerialization {

	public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ImagePlus demoSlice = IJ.openImage("src/test/resources/demoSlice.tif");
        demoSlice.show();

        MultiSlicePositioner mp = (MultiSlicePositioner) ij.command().run(ABBACommandAdultMouseAllenBrainCCFv3.class, true).get().getOutput("mp");

        ij.command().run(SourceFromImagePlusCommand.class, true, "imagePlus", demoSlice).get();

        SourceAndConverter[] sac = ij.convert().convert(demoSlice.getTitle(), SourceAndConverter[].class);

        mp.createSlice(sac,4.2);

        SliceSources slice = mp.getSortedSlices().get(0);
        mp.moveSlice(slice, 4.5);
        mp.centerBdvViewOn(slice);
        mp.selectSlice(slice);
        //mp.registerElastixAffine(1,0, false);
        //mp.registerElastixSpline(0,0, 4,false);

        mp.saveState(new File("src/test/resources/ij1registration.json"), true);

	}

}