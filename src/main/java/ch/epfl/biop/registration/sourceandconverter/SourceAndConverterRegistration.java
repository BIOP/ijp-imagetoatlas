package ch.epfl.biop.registration.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.plugin.IABBARegistrationPlugin;
import org.scijava.Context;

abstract public class SourceAndConverterRegistration implements IABBARegistrationPlugin {

    protected SourceAndConverter[] fimg;

    protected SourceAndConverter[] mimg;

    protected int timePoint = 0;

    protected Context context;

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

    /**
     * Is called just after the Registration object creation to pass
     * the current scijava context
     *
     * TODO : replace this by an Annotation parameter once this interface
     * is adopted by all registrations
     * @param context
     */
    public void setScijavaContext(Context context) {
        this.context = context;
    }


    protected boolean isDone = false;

    @Override
    public boolean isRegistrationDone() {
        return isDone;
    }

    @Override
    public void resetRegistration() {
        isDone = false;
    }


}
