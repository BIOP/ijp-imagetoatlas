package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.sourceandconverter.SourceAndConverterRegistration;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.util.ArrayList;

abstract public class AffineTransformSourceAndConverterRegistration extends SourceAndConverterRegistration {

    protected AffineTransform3D at3d = new AffineTransform3D();

    public int timePoint = 0;

    @Override
    public void resetRegistration() {
        at3d = new AffineTransform3D();
    }

    public AffineTransform3D getAffineTransform() {
        return at3d;
    }

    public void setAffineTransform(AffineTransform3D at3d) {
        this.at3d = at3d;
    }

    @Override
    public boolean edit() {
        throw new UnsupportedOperationException();
    }


    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed(SourceAndConverter[] img) {
        SourceAndConverter[] out = new SourceAndConverter[img.length];

        for (int idx = 0;idx<img.length;idx++) {
            out[idx] = SourceTransformHelper.append(at3d, new SourceAndConverterAndTimeRange(img[idx],timePoint));
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
    public boolean isEditable() {
        return false; // TODO : make it editable
    }
}
