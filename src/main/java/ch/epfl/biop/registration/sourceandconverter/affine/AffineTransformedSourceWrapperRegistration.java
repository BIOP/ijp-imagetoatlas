package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.sourceandconverter.SourceAndConverterRegistration;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AffineTransformedSourceWrapperRegistration extends SourceAndConverterRegistration {

    AffineTransform3D at3d = new AffineTransform3D();

    Map<SourceAndConverter, SourceAndConverter> alreadyTransformedSources = new HashMap<>();

    @Override
    public boolean register() {
        isDone = true;
        return true;
    }

    public void setAffineTransform(AffineTransform3D at3d_in) {
        this.at3d = at3d_in;
        alreadyTransformedSources.keySet().forEach(sac -> {
                SourceTransformHelper.set(at3d_in, new SourceAndConverterAndTimeRange(alreadyTransformedSources.get(sac), timePoint));
        });
    }

    public AffineTransform3D getAffineTransform() {
        return at3d.copy();
    }

    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed(SourceAndConverter[] img) {

        SourceAndConverter[] out = new SourceAndConverter[img.length];

        for (int idx = 0;idx<img.length;idx++) {
            if (alreadyTransformedSources.keySet().contains(img[idx])) {
                out[idx] = alreadyTransformedSources.get(img[idx]);
                SourceTransformHelper.set(at3d, new SourceAndConverterAndTimeRange(out[idx], timePoint));
            } else {
                out[idx] = SourceTransformHelper.createNewTransformedSourceAndConverter(at3d, new SourceAndConverterAndTimeRange(img[idx], timePoint));
                alreadyTransformedSources.put(img[idx], out[idx]);
            }
        }

        return out;
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {
        ArrayList<RealPoint> cvtList = new ArrayList<>();
        for (RealPoint p : pts.ptList) {
            RealPoint pt3d = new RealPoint(3);
            pt3d.setPosition(new double[]{p.getDoublePosition(0), p.getDoublePosition(1),0});
            //float npx = p.getFloatPosition(0)/scale+roi.getBounds().x;
            //float npy = p.getFloatPosition(1)/scale+roi.getBounds().y;
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

    @Override
    public boolean edit() {
        throw new UnsupportedOperationException();
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
    public boolean isEditable() {
        return false;
    }
}
