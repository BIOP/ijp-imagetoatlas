package ch.epfl.biop;

import ch.epfl.biop.atlas.allen.adultmousebrain.AllenBrainAdultMouseAtlasCCF2017;
import ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices.SacMultiSacsPositionerCommand;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import net.imagej.ImageJ;

import java.io.File;

public class DemoDeSerialization {

	public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ij.command().run(AllenBrainAdultMouseAtlasCCF2017.class, true).get();

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command().run(SacMultiSacsPositionerCommand.class, true).get().getOutput("mp"));

        mp.loadState(new File("src/main/resources/ij1registration-bw.json"));
	}

}