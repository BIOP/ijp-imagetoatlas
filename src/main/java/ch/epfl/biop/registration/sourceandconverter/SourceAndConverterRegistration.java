package ch.epfl.biop.registration.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.registration.Registration;

abstract public class SourceAndConverterRegistration implements Registration<SourceAndConverter[]> {

    protected SourceAndConverter[] fimg;
    protected SourceAndConverter[] mimg;

    protected int timePoint = 0;

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        this.fimg = fimg;
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        this.mimg = mimg;
    }

    @Override
    public void setTimePoint(int timePoint) {
        this.timePoint = timePoint;
    }

}
