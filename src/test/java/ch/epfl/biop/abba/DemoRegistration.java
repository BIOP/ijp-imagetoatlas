package ch.epfl.biop.abba;

import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import ch.epfl.biop.atlas.aligner.commands.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import mpicbg.spim.data.sequence.Tile;
import net.imagej.ImageJ;


public class DemoRegistration {

	public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        System.out.println("BigWarp hash:"+org.scijava.util.VersionUtils.getVersion(BigWarp.class));

        String scriptTest =
                "run(\"Open [BioFormats Bdv Bridge]\", \"unit=MILLIMETER splitrgbchannels=false positioniscenter=AUTO switchzandc=AUTO flippositionx=AUTO flippositiony=AUTO flippositionz=AUTO usebioformatscacheblocksize=true cachesizex=512 cachesizey=512 cachesizez=1 refframesizeinunitlocation=0.05 refframesizeinunitvoxsize=0.05 \");\n" +
                "run(\"Basic Transformation\", \"sources_in=[SpimData 0] type=Rot180 axis=Z timepoint=0 globalchange=true\");\n" +
                "run(\"Show Sources\", \"sacs=[SpimData 0] autocontrast=true adjustviewonsource=true\");\n" +
                "run(\"Allen Brain Adult Mouse Brain CCF 2017\", \"\");\n";

        ij.script().run("test.ijm",scriptTest,true).get();

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command().run(ABBAStartCommand.class, true).get().getOutput("mp"));

        SourceAndConverter[] sacs =
                //Arrays.concat(
                ij.convert().convert("SpimData 0>Channel>1", SourceAndConverter[].class)

                //ij.convert().convert("SpimData 0>Channel>2", SourceAndConverter[].class),
                //ij.convert().convert("SpimData 0>Channel>3", SourceAndConverter[].class)
                //)
            ;

        mp.createSlice(sacs,8, 0.182, Tile.class, new Tile(-1));
        mp.deselectSlice(mp.getSortedSlices());
        mp.selectSlice(mp.getSortedSlices().get(0));



        /*mp.enqueueRegistration("Auto Elastix Affine", 0,0);
        mp.waitForTasks();

        mp.exportSlice(mp.getSortedSlices().get(0));*/

	}

}