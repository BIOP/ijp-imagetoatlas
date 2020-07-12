package ch.epfl.biop.registration;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;

public interface Registration<T> {

    void setFixedImage(T fimg);
    void setMovingImage(T mimg);

    boolean register();
    boolean edit();

    T getTransformedImageMovingToFixed(T img);
    RealPointList getTransformedPtsFixedToMoving(RealPointList pts);

    boolean parallelSupported();
    boolean isManual();
    boolean isEditable();

}
