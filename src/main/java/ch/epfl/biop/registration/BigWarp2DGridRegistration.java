package ch.epfl.biop.registration;

import bdv.ij.ApplyBigwarpPlugin;
import bdv.viewer.Interpolation;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.Point;

import java.util.List;
import java.util.function.Function;

public class BigWarp2DGridRegistration implements Registration<ImagePlus> {

    ImagePlus fimg, mimg;

    @Override
    public void setFixedImage(ImagePlus fimg) {
        System.out.println("fimg = "+fimg.getTitle());
        this.fimg = fimg;
    }

    @Override
    public void setMovingImage(ImagePlus mimg) {
        System.out.println("mimg = "+mimg.getTitle());
        this.mimg = mimg;
    }

    BigWarp bw;

    int spacing = 50;

    Roi roi;

    @Override
    public void register() {
        try
        {
            //new RepeatingReleasedEventsFixer().install();
            bw = new BigWarp( BigWarpInit.createBigWarpDataFromImages( this.mimg, this.fimg ), "Big Warp",  null ); // pb with virtualstack fimg
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

        }
        catch (final SpimDataException e)
        {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public Function<ImagePlus, ImagePlus> getImageRegistration() {
        // See https://github.com/saalfeldlab/bigwarp/blob/e490dd2ce87c6bcf3355e01e562586421f978303/scripts/Apply_Bigwarp_Xfm.groovy
        return ((img) -> {
                    return ApplyBigwarpPlugin.apply(
                            img, fimg, bw.getLandmarkPanel().getTableModel(),
                            "Target", "", "Target",
                            null, null, null,
                            Interpolation.NEARESTNEIGHBOR, false, 1 );});
    }

    @Override
    public Function<List<Point>, List<Point>> getPtsRegistration() {
        return null;
    }
}
