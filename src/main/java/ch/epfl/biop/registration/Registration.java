package ch.epfl.biop.registration;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;

public interface Registration<T> {

    void setFixedImage(T fimg);
    void setMovingImage(T mimg);
    void resetRegistration();

    boolean register();
    boolean edit();
    boolean isRegistrationDone();

    T getTransformedImageMovingToFixed(T img);
    RealPointList getTransformedPtsFixedToMoving(RealPointList pts);

    boolean parallelSupported();
    boolean isManual();
    boolean isEditable();

}
