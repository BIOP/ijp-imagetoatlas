package ch.epfl.biop.registration;

import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;
import mpicbg.spim.data.SpimDataException;
import net.imglib2.Point;

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
            bw = new BigWarp( BigWarpInit.createBigWarpDataFromImages( this.fimg, this.mimg ), "Big Warp",  null ); // pb with virtualstack fimg
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

        return null;
    }

    @Override
    public Function<List<Point>, List<Point>> getPtsRegistration() {
        return null;
    }
}
