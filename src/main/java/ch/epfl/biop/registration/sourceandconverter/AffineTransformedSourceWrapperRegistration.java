package ch.epfl.biop.registration.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.util.HashMap;
import java.util.Map;

public class AffineTransformedSourceWrapperRegistration implements Registration<SourceAndConverter[]> {

    SourceAndConverter[] fimg;
    SourceAndConverter[] mimg;

    int timePoint = 0;

    AffineTransform3D at3d = new AffineTransform3D();

    Map<SourceAndConverter, SourceAndConverter> alreadyTransformedSources = new HashMap<>();

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
        return true;
    }

    public void setAffineTransform(AffineTransform3D at3d_in) {
        this.at3d = at3d_in;
        alreadyTransformedSources.keySet().forEach(sac -> {
                SourceTransformHelper.set(at3d_in, new SourceAndConverterAndTimeRange(alreadyTransformedSources.get(sac), timePoint));
        });
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

    @Override
    public boolean edit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEditable() {
        return false;
    }
}
