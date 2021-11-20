package ch.epfl.biop.abba;

import bdv.util.BdvHandle;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.DebugView;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.command.ImportImagePlusCommand;
import ch.epfl.biop.atlas.aligner.command.RegistrationElastixAffineCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.command.AllenBrainAdultMouseAtlasCCF2017Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.awt.*;

public class DemoRegistrationIJ1 {

	public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ImagePlus demoSlice = IJ.openImage("src/test/resources/demoSlice.tif");
        demoSlice.show();

        Atlas mouseAtlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017Command.class, true).get().getOutput("ba");

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command()
                .run(ABBAStartCommand.class, true,
                "ba", mouseAtlas,
                        "slicing_mode", "coronal").get().getOutput("mp"));

        BdvHandle bdvh = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        BdvMultislicePositionerView view = new BdvMultislicePositionerView(mp, bdvh);

        DebugView debugView = new DebugView(mp); // Logs events in an extra frame

        ij.command().run(ImportImagePlusCommand.class, true,
                "mp", mp,
                "slice_axis", 5).get();

        final SliceSources slice = mp.getSlices().get(0); // there's only one slice

        // This is how you can perform asynchronous tasks on slices:
        CancelableAction testAction = new CancelableAction(mp) {

                @Override
                public SliceSources getSliceSources() {
                        return slice;
                }

                @Override
                protected boolean run() {
                        IJ.log("*** The test action is being executed from Thread "+Thread.currentThread().getName());
                        try {
                                Thread.sleep(3000);
                        } catch (InterruptedException e) {
                                e.printStackTrace();
                        }
                        IJ.log("*** Test action done!");
                        return true;
                }

                @Override
                protected boolean cancel() {
                        IJ.log("*** Test action cancelled.");
                        return true;
                }

                @Override
                public void drawAction(Graphics2D g, double px, double py, double scale) {
                        switch (slice.getActionState(this)){ // Will change depending on the state of this action
                                case "(done)":
                                        g.setColor(new Color(0, 255, 0, 200));
                                        break;
                                case "(locked)":
                                        g.setColor(new Color(255, 0, 0, 200));
                                        break;
                                case "(pending)":
                                        g.setColor(new Color(255, 255, 0, 200));
                                        break;
                        }
                        g.fillOval((int) (px - 7), (int) (py - 7), 14, 14);
                        g.setColor(new Color(255, 255, 255, 200));
                        g.drawString("T", (int) px - 4, (int) py + 5);
                }
        };

        testAction.runRequest();

        Thread.sleep(6000);

        IJ.log("*** Canceling now.");

        testAction.cancelRequest();

        IJ.log("*** Requesting an affine registration");

        mp.selectSlice(slice); // Should be selected to be registered

        ij.command().run(RegistrationElastixAffineCommand.class, true,
                    "mp", mp,
                    "show_imageplus_registration_result", true,
                    "background_offset_value_moving", 0,
                    "atlas_image_channel",0,
                    "slice_image_channel",0,
                    "pixel_size_micrometer", 20
            ).get();

        IJ.log("*** Wait for end of all tasks");
        mp.waitForTasks();
        IJ.log("*** Done");

        //DebugView debugView = new DebugView(mp);

        //mp.createSlice(sac,4.5);

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