package ch.epfl.biop.abba;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.DebugView;
import ch.epfl.biop.atlas.aligner.command.RegistrationElastixAffineCommand;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.command.AllenBrainAdultMouseAtlasCCF2017Command;
import ch.epfl.biop.bdv.command.importer.SourceFromImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.File;

public class DemoRegistrationIJ1 {

	public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ImagePlus demoSlice = IJ.openImage("src/test/resources/demoSlice.tif");
        demoSlice.show();

        ij.command().run(AllenBrainAdultMouseAtlasCCF2017Command.class, true).get();

        //ij.command().run(ABBAStartCommand.class, true).get();

        ij.command().run(SourceFromImagePlusCommand.class, true, "imagePlus", demoSlice).get();

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command().run(ABBAStartCommand.class, true).get().getOutput("mp"));

        SourceAndConverter[] sac = ij.convert().convert(demoSlice.getTitle(), SourceAndConverter[].class);

        BdvHandle bdvh = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        BdvMultislicePositionerView view = new BdvMultislicePositionerView(mp, bdvh);

        DebugView debugView = new DebugView(mp);

        mp.createSlice(sac,4.5);

        //mp.createSlice(sac,6.5); // easy way to have several slices

        //mp.createSlice(sac,8.5); // easy way to have several slices
        /*
        mp.waitForTasks();

        SliceSources slice = mp.getSortedSlices().get(0);

        //mp.centerBdvViewOn(slice);
        mp.selectSlice(slice);

        ij.command().run(RegistrationElastixAffineCommand.class, true,
                "mp", mp,
                        "show_imageplus_registration_result", true,
                        "background_offset_value_moving", 0,
                        "atlas_image_channel",0,
                        "slice_image_channel",0
                ).get();
                */

        /*ij.command().run(RegistrationElastixSplineCommand.class, true,
                    "mp", mp,
                    "nbControlPointsX", 10,
                    "showImagePlusRegistrationResult", true,
                    "background_offset_value_moving", 0,
                    "atlasImageChannel",0,
                    "sliceImageChannel",0
            ).get();*/

        /*System.out.println("Waiting for registration tasks to be finished...");
        mp.waitForTasks();
        System.out.println("Saving...");
        mp.saveState(new File("src/test/resources/output/reg_demoregistrationij1.json"), true);
        System.out.println("Done");*/
            /*
        sac = ij.convert().convert(demoSlice.getTitle(), SourceAndConverter[].class);

        mp.createSlice(sac,6.5);*/


    }

}