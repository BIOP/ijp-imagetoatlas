package ch.epfl.biop.registration;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;

/**
 *
 * @param <T> Type of the image to register ( ImagePlus / SourceAndConverter, what else ? )
 */

public interface Registration<T> {

    void setFixedImage(T fimg);
    void setMovingImage(T mimg);
    void resetRegistration();
    void setTimePoint(int timePoint);

    boolean register();
    boolean edit();
    boolean isRegistrationDone();

    T getTransformedImageMovingToFixed(T img);
    RealPointList getTransformedPtsFixedToMoving(RealPointList pts);

    boolean parallelSupported();
    boolean isManual();
    boolean isEditable();

}
