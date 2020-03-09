package ch.epfl.biop.registration.imageplus;

import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import ch.epfl.biop.registration.Registration;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import mpicbg.spim.data.SpimDataException;

import javax.swing.*;

public class BigWarp2DGridRegistration extends BigWarp2DRegistration implements Registration<ImagePlus> {

    //ImagePlus fimg, mimg;

    @Override
    public void setFixedImage(ImagePlus fimg) {
        this.fimg = fimg;
    }

    @Override
    public void setMovingImage(ImagePlus mimg) {
        this.mimg = mimg;
    }

    //BigWarp bw;

    int spacing = 50;

    Roi roi;

    @Override
    public void register() {
        try
        {
            //new RepeatingReleasedEventsFixer().install();
            bw = new BigWarp( BigWarpInit.createBigWarpDataFromImages( this.mimg, this.fimg ), "Big Warp",  null ); // pb with virtualstack fimg

            int shiftChannel = this.mimg.getNChannels();
            if (this.mimg instanceof CompositeImage) {
                transferChannelSettings((CompositeImage) this.mimg, bw.getSetupAssignments(), bw.getViewerFrameP().getViewerPanel().getVisibilityAndGrouping(), 0);
            } else {
                transferImpSettings(this.mimg, bw.getSetupAssignments(), 0);
            }

            if (this.fimg instanceof CompositeImage) {
                transferChannelSettings((CompositeImage) this.fimg, bw.getSetupAssignments(), bw.getViewerFrameQ().getViewerPanel().getVisibilityAndGrouping(), shiftChannel);
            } else {
                transferImpSettings(this.fimg, bw.getSetupAssignments(), shiftChannel);
            }

            // Adds grid points evenly spaced in the whole image
            int w = fimg.getWidth();
            int h = fimg.getHeight();

            boolean roiPresent = false;
            fimg.show();
            while (!roiPresent) {
                WaitForUserDialog dialog = new WaitForUserDialog("Region selection", "Draw ROI in the fixed image, where you want to adjust the registration.");
                dialog.show();
                roiPresent = fimg.getRoi()!=null;
            }
            this.roi=fimg.getRoi();
            fimg.hide();

            String s = (String) JOptionPane.showInputDialog(
                    null,
                    "Choose grid spacing in pixels",
                    "Big Warp Grid",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "50");

            spacing = Integer.valueOf(s);

            for (int x = 0;x<w;x+=spacing) {
                for (int y = 0;y<h;y+=spacing) {
                    if (roi.contains(x,y)) {
                        bw.addPoint(new double[] {x,y}, false);
                        bw.addPoint(new double[] {x,y}, true);
                    }
                }
            }

            bw.getViewerFrameP().getViewerPanel().requestRepaint();
            bw.getViewerFrameQ().getViewerPanel().requestRepaint();
            bw.getLandmarkFrame().repaint();

            WaitForUserDialog dialog = new WaitForUserDialog("Choose slice","Please perform carefully your registration then press ok.");
            dialog.show();

            bw.getViewerFrameP().setVisible(false);
            bw.getViewerFrameQ().setVisible(false);
            bw.getLandmarkFrame().setVisible(false);


        }
        catch (final SpimDataException e)
        {
            e.printStackTrace();
            return;
        }
    }

}
