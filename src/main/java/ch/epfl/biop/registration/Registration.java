package ch.epfl.biop.registration;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;

public interface Registration<T> {

    void setFixedImage(T fimg);
    void setMovingImage(T mimg);

    boolean register();

    T getTransformedImageMovingToFixed(T img);
    RealPointList getTransformedPtsFixedToMoving(RealPointList pts);

    boolean parallelSupported();
    boolean isManual();

}
