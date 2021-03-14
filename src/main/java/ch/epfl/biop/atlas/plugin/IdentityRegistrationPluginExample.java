package ch.epfl.biop.atlas.plugin;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Example plugin which shows the extensibility of ABBA
 *
 * A {@link SourceAndConverter} is the data structure used to represent a single channel of an image
 * since potentially multiple channels can be used for registration, each being send to the plugin
 * is an array of SourceAndConverter, each element of the array being a single channel.
 *
 * TODO : explain how to obtain a simple data image from a SourceAndConverter object
 *
 *
 */
@Plugin(type = IABBARegistrationPlugin.class)
public class IdentityRegistrationPluginExample implements IABBARegistrationPlugin{

    public static Consumer<String> defaultLog = (string) -> System.out.println(IdentityRegistrationPluginExample.class.getSimpleName()+":"+string);

    public Consumer<String> log = defaultLog;

    /**
     * The moving sources as well as the fixed sources should not be modified
     * during the registration process. They need to be stored in the plugin
     * internally in order to be accessible inside the register, edit, etc methods.
     */
    SourceAndConverter<?>[] fixed_images;

    SourceAndConverter<?>[] moving_images;

    /**
     * Custom transform object - get and set using a serialized string object
     */
    MyTransform transform = new MyTransform();

    boolean isRegistrationDone = false;

    /**
     *
     * @param context scijava context being send by ABBA to the registration plugin
     */
    @Override
    public void setScijavaContext(Context context) {
        log.accept("Scijava context for registration class"+this.getClass().getSimpleName()+" has been set");
    }

    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {

    }

    /**
     * This function is called by ABBA in order to set the moving images
     * to the registration plugin
     * @param fimg fixed images being send by ABBA for this registration
     */
    @Override
    public void setFixedImage(SourceAndConverter<?>[] fimg) {
        log.accept("Fixed images set, there are "+fimg.length+" images.");
        fixed_images = fimg;
    }

    @Override
    public void setMovingImage(SourceAndConverter<?>[] mimg) {
        log.accept("Moving images set, there are "+mimg.length+" images.");
        moving_images = mimg;
    }

    @Override
    public void resetRegistration() {
        // TODO : document what that means
        log.accept("Registration has been reset");
    }

    @Override
    public void setTimePoint(int timePoint) {
        // Timepoint of sources and converter used for registration - normally always 0
    }

    @Override
    public boolean register() {
        // Returns true if the registration has been successfully performed

        // Let's take a break
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            log.accept("The registration has been interrupted!");
            return false;
        }

        if (Math.random()<0.1) {
            // Something went wrong in 10% of the case
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean edit() {
        return false;
    }

    @Override
    public boolean isRegistrationDone() {
        return false;
    }

    @Override
    public SourceAndConverter<?>[] getTransformedImageMovingToFixed(SourceAndConverter<?>[] img) {
        return new SourceAndConverter[0];
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {
        return null;
    }

    @Override
    public void abort() {
        // If the registration is long, perform an action which immediately
        // kills the running process, no source has to be restored
        System.out.println("Abort is called!");
    }

    @Override
    public String getTransform() {
        return transform.getJson();
    }

    @Override
    public void setTransform(String serialized_transform) {
        transform.setJson(serialized_transform);
        isRegistrationDone = true;
    }

    @Override
    public void setLogger(Consumer<String> logger) {
        log = logger;
    }

    public static class MyTransform {

        public MyTransform() {

        }

        public void setJson(String jsonString) {
            // do stuff
        }

        public String getJson() {
            // do stuff
            return "{myTransform object serialized}";
        }

    }

}
