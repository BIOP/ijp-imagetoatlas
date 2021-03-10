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

    /**
     * Function called when a registration is cancelled while being processed
     */
    void abort();

    /**
     * This function is used to:
     * - save the registration into a json file
     * - store transiently the state of a registration to cancel the edition of a transform, if needed
     * @return a serialized representation of the transform as a String
     */
    String getTransform();

    /**
     * Function used to bypass the real registration process (run)
     * in order to set directly the result of the registration.
     *
     * This is used when:
     * - loading a registration from a state file
     * - restore if needed a previous transformed state after an edition
     * a serialized representation of the transform is sent, leading
     * to an immediate registration being done
     * isDone should return true after this function is being called
     * @param serialized_transform
     */
    void setTransform(String serialized_transform);

}
