package ch.epfl.biop.registration;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import net.imglib2.realtransform.RealTransform;
import org.scijava.Context;

import java.util.Map;

/**
 * Most upstream
 * @param <T> Type of the image to register ( ImagePlus / SourceAndConverter, what else ? )
 */

public interface Registration<T> {

    /**
     * Is called just after the Registration object creation to pass
     * the current scijava context
     * is adopted by all registrations
     * @param context
     */
    void setScijavaContext(Context context);

    /**
     * Is called before registration to pass any extra registration parameter
     * argument. Passed as a dictionary of String to preserve serialization
     * capability.
     * @param parameters dictionary of parameters
     */
    void setRegistrationParameters(Map<String,String> parameters);

    /**
     * For serialization and reproducibility
     * @return parameters that were used for the registration
     */
    Map<String,String> getRegistrationParameters();

    /**
     * Sets the fixed image
     * @param fimg fixed image
     */
    void setFixedImage(T fimg);

    /**
     * Set the moving image
     * @param mimg moving image
     */
    void setMovingImage(T mimg);

    /**
     * Optional - sets the fixed image mask
     * @param fimg_mask fixed image
     */
    void setFixedMask(T fimg_mask);

    /**
     * Optional - set the moving image mask
     * @param mimg_mask moving image
     */
    void setMovingMask(T mimg_mask);

    /**
     * Sets the state of the registration to not done
     * Called when the registration needs to be rerun for real
     * instead of just restored
     *
     */
    void resetRegistration();

    /**
     * Sets the timepoint of the source that should be used for the
     * registration. 0 in most cases
     * @param timePoint
     */
    void setTimePoint(int timePoint);

    /**
     * Blocking function which performs the registration
     * @return true if the registration was run succesfully
     */
    boolean register();

    /**
     * Can be called after register() return false in order to get
     * a more meaningful explanation
     * @return an error message for a failed registration
     */
    default String getExceptionMessage() {
        return "Unspecified error";
    }

    /**
     * Blocking function which is called when the user wants
     * to manually edit the result of the registration
     * @return true is the transform is been edited successfully. If not,
     * the previous state of the registration is restored thanks to the serialization
     * of the transform. An edition cannot occur if the transform has not been set
     * before (either via the run method, or via setting the transform via the
     * {@link Registration#setTransform(String)} method
     */
    boolean edit();

    /**
     * Flag functions which serves to know whether the result of the
     * registration is available. Should not be blocking.
     *
     * @return true if the transform is available
     */
    boolean isRegistrationDone();

    /**
     * Function which takes an input moving image, transform it
     * according to the result of the registration (transformation),
     * and returns it
     * @param img should not be modified
     * @return the transformed image
     */
    T getTransformedImageMovingToFixed(final T img);

    /**
     * Reverse transforms a list of points. This function takes a list
     * of points given in the fixed coordinates system, inverse transform
     * their coordinates according to the result of the registration.
     * The points can be mutated
     * @param pts points to transform from fixed to moving system coordinates
     * @return the transformed points
     */
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
     * This is used when:
     * - loading a registration from a state file
     * - restore if needed a previous transformed state after an edition
     * a serialized representation of the transform is sent, leading
     * to an immediate registration being done
     * isDone should return true after this function is being called
     * @param serialized_transform
     */
    void setTransform(String serialized_transform);

    /**
     * If the transform can be returned as a serializable RealTransform object,
     * this can be used to serialize the successive registrations as a
     * {@link net.imglib2.realtransform.RealTransformSequence} object, or even,
     * if all transformations are invertible, as a {@link net.imglib2.realtransform.InvertibleRealTransformSequence} object
     * @return
     */
    RealTransform getTransformAsRealTransform();

    /**
     * Used for serialisation
     * @return
     */
    default String getRegistrationTypeName() {
      return this.getClass().getSimpleName();
    }
}
