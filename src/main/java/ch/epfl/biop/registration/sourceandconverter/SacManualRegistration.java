package ch.epfl.biop.registration.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;

import java.util.function.Function;

public class SacManualRegistration implements Registration<SourceAndConverter[]> {

    SourceAndConverter[] fimg, mimg;

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) { this.fimg = fimg; }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        this.mimg = mimg;
    }

    @Override
    public boolean register() {
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
}
