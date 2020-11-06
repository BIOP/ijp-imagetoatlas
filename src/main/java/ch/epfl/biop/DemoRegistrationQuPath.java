package ch.epfl.biop;

import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices.SacMultiSacsPositionerCommand;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import mpicbg.spim.data.sequence.Tile;
import net.imagej.ImageJ;
import org.apache.commons.io.FilenameUtils;
//import org.apache.groovy.util.Arrays;

import javax.swing.*;
import java.io.File;

public class DemoRegistrationQuPath {

	public static void main(String[] args) throws Exception {

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        //System.out.println("BigWarp hash:"+org.scijava.util.VersionUtils.getVersion(BigWarp.class));

        String scriptTest =
                "run(\"Open [QuPath Project]\", \"unit=MILLIMETER " +
                        "splitrgbchannels=false " +
                        "positioniscenter=AUTO " +
                        "switchzandc=AUTO " +
                        "flippositionx=AUTO " +
                        "flippositiony=AUTO " +
                        "flippositionz=AUTO " +
                        "usebioformatscacheblocksize=true " +
                        "cachesizex=512 cachesizey=512 cachesizez=1 " +
                        "refframesizeinunitlocation=1.0 refframesizeinunitvoxsize=0.01 " +
                        //"qupathproject=[C:\\\\Users\\\\nicol\\\\Dropbox\\\\BIOP\\\\19-05-24 VSI Samples\\\\align\\\\QP0.2.1\\\\project.qpproj]\"
                        "\");\n" +
                //"run(\"Open [BioFormats Bdv Bridge]\", \"unit=MILLIMETER splitrgbchannels=false positioniscenter=AUTO switchzandc=AUTO flippositionx=AUTO flippositiony=AUTO flippositionz=AUTO usebioformatscacheblocksize=true cachesizex=512 cachesizey=512 cachesizez=1 refframesizeinunitlocation=0.05 refframesizeinunitvoxsize=0.05 \");\n" +
                "run(\"Basic Transformation\", \"sources_in=[SpimData 0] type=Rot180 axis=Z timepoint=0 globalchange=true\");\n" +
                "run(\"BDV - Show Sources\", \"sacs=[SpimData 0] autocontrast=true adjustviewonsource=true\");\n" +
                "run(\"Allen Brain Adult Mouse Brain CCF 2017\", \"\");\n";

                            ij.script().run("test.ijm", scriptTest, true).get();
                            System.out.println("Import done.");


        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command().run(SacMultiSacsPositionerCommand.class, true).get().getOutput("mp"));

        SourceAndConverter[] sacs =
                //Arrays.concat(
                ij.convert().convert("SpimData 0>Channel>1", SourceAndConverter[].class);

                //ij.convert().convert("SpimData 0>Channel>2", SourceAndConverter[].class),
                //ij.convert().convert("SpimData 0>Channel>3", SourceAndConverter[].class)
                //);

        //mp.setSingleSliceDisplayMode();
        mp.createSlice(sacs,8.32, 0.08, Tile.class, new Tile(-1));
        mp.selectSlice(mp.getSortedSlices());
        mp.registerElastixAffine(0,0);

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