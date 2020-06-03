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
    public void register() {

    }

    @Override
    public Function<SourceAndConverter[], SourceAndConverter[]> getImageRegistration() {
        return null;
    }

    @Override
    public RealPointList getPtsRegistration(RealPointList pts) {
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
}
