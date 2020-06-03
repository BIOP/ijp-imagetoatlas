package ch.epfl.biop;

//import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
//import ch.epfl.biop.java.utilities.roi.types.ImageJRoisFile;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import ch.epfl.biop.atlastoimg2d.commands.multislices.MultiSlicePositioner;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import net.imagej.ImageJ;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RealRandomAccess;
import net.imglib2.Sampler;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;


public class DummyCommand {

	public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        System.out.println("BigWarp hash:"+org.scijava.util.VersionUtils.getVersion(BigWarp.class));

        String scriptTest = "run(\"Open [BioFormats Bdv Bridge]\", \"unit=MILLIMETER splitrgbchannels=false positioniscenter=AUTO switchzandc=AUTO flippositionx=AUTO flippositiony=AUTO flippositionz=AUTO usebioformatscacheblocksize=true cachesizex=512 cachesizey=512 cachesizez=1 refframesizeinunitlocation=0.05 refframesizeinunitvoxsize=0.05 \");\n" +
                "run(\"Show Sources\", \"sacs=[SpimData 0] autocontrast=true adjustviewonsource=true\");\n" +
                "run(\"Basic Transformation\", \"sources_in=[SpimData 0] type=Rot180 axis=Z timepoint=0 globalchange=false\");\n" +
                "run(\"Allen Brain Adult Mouse Brain CCF 2017\", \"\");\n" +
                "run(\"Position Multiple Slices\");\n";

        ij.script().run("test.ijm",scriptTest,true).get();

        MultiSlicePositioner mp = ij.object().getObjects(MultiSlicePositioner.class).get(0);

        for (int i=2;i<10;i++) {

            SourceAndConverter[] sacs = ij.convert().convert("SpimData 0>Tile>"+i, SourceAndConverter[].class);

            mp.createSlice(sacs,i/10+3);
        }

	}

}