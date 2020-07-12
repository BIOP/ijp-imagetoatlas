package ch.epfl.biop.registration.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

public class CenterZeroRegistration implements Registration<SourceAndConverter[]> {

    SourceAndConverter[] fimg;
    SourceAndConverter[] mimg;

    AffineTransform3D at3d;

    int timePoint = 0;

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        this.fimg = fimg;
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        this.mimg = mimg;
    }

    void setTimePoint(int timePoint) {
        this.timePoint = timePoint;
    }

    @Override
    public boolean register() {
        /*if (mimg.length>0) {
            System.out.println("Warning : center registration works with the first source.");
        }*/

        SourceAndConverter sac = mimg[0];

        RealPoint center = SourceAndConverterUtils.getSourceAndConverterCenterPoint(sac);

        at3d = new AffineTransform3D();

        at3d.translate(
                -center.getDoublePosition(0),
                -center.getDoublePosition(1),
                -center.getDoublePosition(2));

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
            out[idx] = SourceTransformHelper.append(at3d, new SourceAndConverterAndTimeRange(img[idx],timePoint));
        }

        return out;
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {
        return null;
    }

    @Override
    public boolean parallelSupported() {
        return true;
    }

    @Override
    public boolean isManual() {
        return false;
    }

}
