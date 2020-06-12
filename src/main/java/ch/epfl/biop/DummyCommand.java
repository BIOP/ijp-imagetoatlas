package ch.epfl.biop;

import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import mpicbg.spim.data.sequence.Tile;
import net.imagej.ImageJ;


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

        SourceAndConverter[] sacs = ij.convert().convert("SpimData 0>Channel>1", SourceAndConverter[].class);

        mp.createSlice(sacs,8, 0.2, Tile.class, new Tile(-1));
        /*mp.getSortedSlices().stream().forEach(slice -> {
            System.out.println("coucou");
            mp.moveSlice(slice, 10*Math.random());
        });*/

	}

}