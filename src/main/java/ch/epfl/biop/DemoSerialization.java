package ch.epfl.biop;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.allen.adultmousebrain.AllenBrainAdultMouseAtlasCCF2017;
import ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices.SacMultiSacsPositionerCommand;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import ch.epfl.biop.atlastoimg2d.multislice.SliceSources;
import ch.epfl.biop.scijava.command.SourceFromImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;

import java.io.File;

public class DemoSerialization {

	public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ImagePlus demoSlice = IJ.openImage("src/main/resources/demoSlice.tif");
        demoSlice.show();

        ij.command().run(AllenBrainAdultMouseAtlasCCF2017.class, true).get();

        ij.command().run(SourceFromImagePlusCommand.class, true, "imagePlus", demoSlice).get();

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command().run(SacMultiSacsPositionerCommand.class, true).get().getOutput("mp"));


        SourceAndConverter[] sac = ij.convert().convert(demoSlice.getTitle(), SourceAndConverter[].class);

        mp.createSlice(sac,4.2);//, 2, Tile.class, new Tile(-1));

        SliceSources slice = mp.getSortedSlices().get(0);
        mp.moveSlice(slice, 4.5);
        mp.centerBdvViewOn(slice);
        mp.selectSlice(slice);
        mp.registerElastixAffine(1,0);
        mp.registerElastixSpline(0,0);

        mp.saveState(new File("src/main/resources/ij1registration.json"), true);


        /*SliceSources slice = mp.getSortedSlices().get(0);

        mp.centerBdvViewOn(slice);
        mp.selectSlice(slice);

        mp.registerElastixAffine(1,0);
        mp.registerElastixSpline(0,0);

        mp.exportSelectedSlicesRegionsToRoiManager("name");*/

	}

}