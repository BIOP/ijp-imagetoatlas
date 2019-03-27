package ch.epfl.biop.registration;

import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;
import ij.gui.Roi;
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
            ImagePlus imcropped = img.duplicate();//.crop();
            IJ.run(imcropped, "Scale...", "x="+scale+" y="+scale+" interpolation=None create");
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
            //.setPosition(0,(double) npx);//.setPosition(new float[]{npx, npy});
            cvtList.add(cpt);
        }
        /*
        List<RealPoint> cvtList = list.ptList.stream().map(pt -> {
            RealPoint cpt = new RealPoint();
            float npx = pt.getFloatPosition(0)*scale+roi.getBounds().x;
            float npy = pt.getFloatPosition(1)*scale+roi.getBounds().y;
            cpt.setPosition(new float[]{npx, npy});
            return cpt;
        }).collect(Collectors.toList());*/


        //System.out.println("cvtList.size()="+cvtList.size());
        return new RealPointList(cvtList);
    }

    /*@Override
    public Function<RealPointList, RealPointList> getPtsRegistration() {
        return (list) -> {
            System.out.println("apply transform crop n scale");
            List<RealPoint> cvtList = list.ptList.stream().map(pt -> {
                RealPoint cpt = new RealPoint();
                float npx = pt.getFloatPosition(0)*scale+roi.getBounds().x;
                float npy = pt.getFloatPosition(1)*scale+roi.getBounds().y;
                cpt.setPosition(new float[]{npx, npy});
                return cpt;
            }).collect(Collectors.toList());
            System.out.println("cvtList.size()="+cvtList.size());
            return new RealPointList(cvtList);
        };
    }*/
}
