package ch.epfl.biop.registration.imageplus;

import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;
import ij.gui.Roi;
import ij.plugin.Duplicator;
import net.imglib2.RealPoint;

import java.util.ArrayList;
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
            ImagePlus imcropped = (new Duplicator()).run(img);//.duplicate().crop();
            IJ.run(imcropped, "Scale...", "x="+scale+" y="+scale+" interpolation=Bilinear create");
            return IJ.getImage();
        };
    }

    @Override
    public RealPointList getPtsRegistration(RealPointList list) {
        ArrayList<RealPoint> cvtList = new ArrayList<>();
        for (RealPoint p : list.ptList) {
            float npx = p.getFloatPosition(0)/scale+roi.getBounds().x;
            float npy = p.getFloatPosition(1)/scale+roi.getBounds().y;
            RealPoint cpt = new RealPoint(npx, npy);
            cvtList.add(cpt);
        }
        return new RealPointList(cvtList);
    }

    @Override
    public boolean parallelSupported() {
        return false;
    }

    @Override
    public boolean isManual() {
        return true;
    }
}
