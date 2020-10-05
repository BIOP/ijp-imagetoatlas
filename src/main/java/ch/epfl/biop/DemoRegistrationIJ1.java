package ch.epfl.biop;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.allen.adultmousebrain.AllenBrainAdultMouseAtlasCCF2017;
import ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices.SacMultiSacsPositionerCommand;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import ch.epfl.biop.atlastoimg2d.multislice.SliceSources;
import ch.epfl.biop.scijava.command.SourceFromImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.sequence.Tile;
import net.imagej.ImageJ;

public class DemoRegistrationIJ1 {

	public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ImagePlus demoSlice = IJ.openImage("src/main/resources/demoSlice.tif");
        demoSlice.show();

        ij.command().run(AllenBrainAdultMouseAtlasCCF2017.class, true).get();

        ij.command().run(SourceFromImagePlusCommand.class, true, "imagePlus", demoSlice).get();

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command().run(SacMultiSacsPositionerCommand.class, true).get().getOutput("mp"));

        SourceAndConverter[] sac = ij.convert().convert("SpimData 1", SourceAndConverter[].class);

        mp.createSlice(sac,4.5, 2, Tile.class, new Tile(-1));

        SliceSources slice = mp.getSortedSlices().get(0);

        mp.centerBdvViewOn(slice);
        mp.selectSlice(slice);

        mp.registerElastix(0,0);

        mp.exportSelectedSlicesRegionsToRoiManager("name");

        /*SourceAndConverter[] sacs =
                //Arrays.concat(
                ij.convert().convert("SpimData 0>Channel>1", SourceAndConverter[].class);

                //ij.convert().convert("SpimData 0>Channel>2", SourceAndConverter[].class),
                //ij.convert().convert("SpimData 0>Channel>3", SourceAndConverter[].class)
                //);*/

        //mp.createSlice(sacs,8, 0.182, Tile.class, new Tile(-1));
        /*mp.deselectSlice(mp.getSortedSlices());
        mp.selectSlice(mp.getSortedSlices().get(0));
        mp.registerElastix(0,0);
        mp.exportSlice(mp.getSortedSlices().get(0), "acronym", new File("C:\\Users\\nicol\\Desktop\\ExportROIs"), true );
        */


        /*mp.enqueueRegistration("Auto Elastix Affine", 0,0);
        mp.waitForTasks();

        mp.exportSlice(mp.getSortedSlices().get(0));*/


	}

}