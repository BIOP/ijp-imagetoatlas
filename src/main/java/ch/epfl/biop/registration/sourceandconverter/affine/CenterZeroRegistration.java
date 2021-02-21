package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.util.ArrayList;

public class CenterZeroRegistration extends AffineTransformSourceAndConverterRegistration {//implements Registration<SourceAndConverter[]> {

    //SourceAndConverter[] fimg;
    //SourceAndConverter[] mimg;

    AffineTransform3D at3d;

    /*int timePoint = 0;

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        this.fimg = fimg;
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        this.mimg = mimg;
    }*/



    @Override
    public boolean register() {

        SourceAndConverter sac = mimg[0];

        RealPoint center = SourceAndConverterHelper.getSourceAndConverterCenterPoint(sac);

        at3d = new AffineTransform3D();

        at3d.translate(
                -center.getDoublePosition(0),
                -center.getDoublePosition(1),
                -center.getDoublePosition(2));

        isDone = true;

        return true;
    }

    @Override
    public boolean edit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed(SourceAndConverter[] img) {

        SourceAndConverter[] out = new SourceAndConverter[img.length];

        for (int idx = 0;idx<img.length;idx++) {
            out[idx] = SourceTransformHelper.createNewTransformedSourceAndConverter(at3d, new SourceAndConverterAndTimeRange(img[idx], timePoint));
        }

        return out;
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {
        ArrayList<RealPoint> cvtList = new ArrayList<>();
        for (RealPoint p : pts.ptList) {
            RealPoint pt3d = new RealPoint(3);
            pt3d.setPosition(new double[]{p.getDoublePosition(0), p.getDoublePosition(1),0});
            at3d.inverse().apply(pt3d, pt3d);
            RealPoint cpt = new RealPoint(pt3d.getDoublePosition(0), pt3d.getDoublePosition(1));
            cvtList.add(cpt);
        }
        return new RealPointList(cvtList);
    }

    @Override
    public boolean parallelSupported() {
        return true;
    }

    @Override
    public boolean isManual() {
        return false;
    }

    private boolean isDone = false;

    @Override
    public boolean isRegistrationDone() {
        return isDone;
    }

    @Override
    public void resetRegistration() {
        isDone = false;
    }

    @Override
    public void abort() {

    }
}
