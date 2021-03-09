package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import org.scijava.Context;

import java.util.Map;
import java.util.function.Function;

public class SacManualAffineRegistration implements Registration<SourceAndConverter[]> {

    SourceAndConverter[] fimg, mimg;

    @Override
    public void setScijavaContext(Context context) {
        // Ignored
    }

    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {

    }

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) { this.fimg = fimg; }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        this.mimg = mimg;
    }

    @Override
    public boolean register() {
        isDone = false;
        return false;
    }

    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed(SourceAndConverter[] img) {
        return null;
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {
        return null;
    }

    @Override
    public boolean parallelSupported() {
        return false;
    }

    @Override
    public boolean isManual() {
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
    public void setTimePoint(int timePoint) {
        // TODO
    }

    @Override
    public void abort() {

    }
}
