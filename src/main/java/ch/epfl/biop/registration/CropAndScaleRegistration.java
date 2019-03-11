package ch.epfl.biop.registration;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;
import net.imglib2.Point;
import ij.gui.Roi;

import java.util.List;
import java.util.function.Function;

public class CropAndScaleRegistration implements Registration<ImagePlus> {
    ImagePlus fimg, mimg;

    Roi roi;
    float scale = 1f;

    @Override
    public void setFixedImage(ImagePlus fimg) {
        this.fimg = fimg;
    }

    @Override
    public void setMovingImage(ImagePlus mimg) {
        this.mimg = mimg;
    }

    @Override
    public void register() {
        boolean roiPresent = false;
        mimg.show();
        while (!roiPresent) {
            WaitForUserDialog dialog = new WaitForUserDialog("Region selection", "Draw a rectangle ROI around the part of interest.");
            dialog.show();
            roiPresent = mimg.getRoi()!=null;
        }
        this.roi=mimg.getRoi();

        float areaIn = mimg.getRoi().getBounds().height*mimg.getRoi().getBounds().width;
        float areaOut = fimg.getHeight()*fimg.getWidth();

        scale = (float) java.lang.Math.sqrt(areaOut/areaIn);
    }

    @Override
    public Function<ImagePlus, ImagePlus> getImageRegistration() {
        return (img) -> {
            img.setRoi(this.roi);
            ImagePlus imcropped = img.crop();
            IJ.run(imcropped, "Scale...", "x="+scale+" y="+scale+" interpolation=None create");
            return IJ.getImage();
        };
    }

    @Override
    public Function<List<Point>, List<Point>> getPtsRegistration() {
        return null;
    }
}
