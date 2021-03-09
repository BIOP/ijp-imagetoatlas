package ch.epfl.biop.registration;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import org.scijava.Context;

import java.util.Map;

/**
 * Most upstream
 * @param <T> Type of the image to register ( ImagePlus / SourceAndConverter, what else ? )
 */

public interface Registration<T> {

    void setScijavaContext(Context context);
    void setRegistrationParameters(Map<String,String> parameters);

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

    void abort();

    String getTransform();
    void setTransform(String serialized_transform);

}
