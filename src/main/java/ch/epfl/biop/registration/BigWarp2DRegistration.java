package ch.epfl.biop.registration;

import bdv.ij.ApplyBigwarpPlugin;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.Point;
import bdv.viewer.Interpolation;
import net.imglib2.RealPoint;

import java.util.List;
import java.util.function.Function;

public class BigWarp2DRegistration implements Registration<ImagePlus> {

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

    @Override
    public void register() {
        try
        {
            //new RepeatingReleasedEventsFixer().install();
            bw = new BigWarp( BigWarpInit.createBigWarpDataFromImages( this.mimg, this.fimg ), "Big Warp",  null ); // pb with virtualstack fimg
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

    /*@Override
    public Function<RealPointList, RealPointList> getPtsRegistration() {
        return null;
    }*/

    @Override
    public RealPointList getPtsRegistration(RealPointList pts) {
        for (RealPoint pt : pts.ptList) {
            double[] tr = bw.getTransform().apply( new double[] {
                pt.getDoublePosition(0), pt.getDoublePosition(1)
            });
            pt.setPosition(tr);
        }
        return pts;
    }
}
