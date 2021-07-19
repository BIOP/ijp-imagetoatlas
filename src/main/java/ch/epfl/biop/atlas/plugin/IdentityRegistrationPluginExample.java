package ch.epfl.biop.atlas.plugin;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import net.imglib2.realtransform.RealTransform;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.sourceandconverter.importer.SourceAndConverterDuplicator;

import java.util.HashMap;
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
 * TODO : finish this example
 *
 */
@Plugin(type = IABBARegistrationPlugin.class)
@RegistrationTypeProperties(
        isEditable = false,
        isManual = false,
        userInterface = {IdentityRegistrationCommand.class}
)
public class IdentityRegistrationPluginExample implements IABBARegistrationPlugin{

    public static Consumer<String> defaultLog = (string) -> System.out.println(IdentityRegistrationPluginExample.class.getSimpleName()+":"+string);

    public static Consumer<String> log = defaultLog;

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
     * @param context scijava context being send by ABBA to the registration plugin
     */
    @Override
    public void setScijavaContext(Context context) {
        log.accept("Scijava context for registration class"+this.getClass().getSimpleName()+" has been set");
    }

    /**
     * Any parameter of a registration method has to be set as a String to String dictionnary
     * this same dictionnary needs to be returned in {@link IABBARegistrationPlugin#getRegistrationParameters()}
     * for a correct serialization
     * @param parameters dictionary of parameters
     */
    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {
        log.accept("Registration parameters set : "+parameters);
    }

    /**
     * see {@link IABBARegistrationPlugin#setRegistrationParameters(Map)}
     * @return the dictionnary containing the parameters for this registration
     */
    @Override
    public Map<String, String> getRegistrationParameters() {
        return new HashMap<>();
    }

    /**
     * This function is called by ABBA in order to set the moving images
     * to the registration plugin
     * @param fimg fixed images being send by ABBA for this registration
     */
    @Override
    public void setFixedImage(SourceAndConverter<?>[] fimg) {
        log.accept("Fixed images set, there are "+fimg.length+" channels.");
        fixed_images = fimg;
    }

    @Override
    public void setMovingImage(SourceAndConverter<?>[] mimg) {
        log.accept("Moving images set, there are "+mimg.length+" channels.");
        moving_images = mimg;
    }

    @Override
    public void setFixedMask(SourceAndConverter<?>[] fimg_mask) {
        // Ignored
    }

    @Override
    public void setMovingMask(SourceAndConverter<?>[] mimg_mask) {
        // Ignored
    }

    @Override
    public void resetRegistration() {
        // TODO : document what that means
        log.accept("Registration has been reset");
    }

    int timepoint;
    @Override
    public void setTimePoint(int timePoint) {
        // Timepoint of sources and converter used for registration - normally always 0
        this.timepoint = timePoint;
    }

    @Override
    public boolean register() {
        // Returns true if the registration has been successfully performed

        // Let's take a break
        try {
            // That's some heavy work
            Thread.sleep(1000+(int)(Math.random()*2000));
        } catch (InterruptedException e) {
            e.printStackTrace();
            errorMessage = "The registration has been interrupted!";
            log.accept("The registration has been interrupted!");
            return false;
        }

        if (Math.random()<0.1) {
            // Something went wrong in 10% of the case
            errorMessage = "Did you try to turn it off and on again ?";
            log.accept("Error in registration!");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean edit() {
        // Not editable
        return false;
    }

    @Override
    public boolean isRegistrationDone() {
        return isRegistrationDone;
    }

    // The image should not be mutated but copied
    // The length of the array should be identical between input and output
    @Override
    public SourceAndConverter<?>[] getTransformedImageMovingToFixed(SourceAndConverter<?>[] img) {

        SourceAndConverterDuplicator duplicator = new SourceAndConverterDuplicator(null);

        SourceAndConverter[] out = new SourceAndConverter[img.length];

        for (int i=0;i<out.length;i++) {
            out[i] = duplicator.apply(img[i]);
        }

        isRegistrationDone = true;

        return out;
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
    public RealTransform getTransformAsRealTransform() {
        return null; // unsupported, but in fact in this case
    }

    //@Override
    //public void setLogger(Consumer<String> logger) {
    //    log = logger;
    //}

    @Override
    public void setSliceInfo(MultiSlicePositioner.SliceInfo sliceInfo) {
        // Can be used to retrieve some info about the slice being registered
    }

    String errorMessage = "No error";

    @Override
    public String getExceptionMessage() {
        return errorMessage;
    }

    public static class MyTransform {

        public MyTransform() {

        }

        public void setJson(String jsonString) {
            // do stuff
            log.accept("Transform set in "+this+" : "+jsonString);
        }

        public String getJson() {
            // do stuff
            log.accept("Transform get in "+this);
            return "{myTransform object serialized}";
        }

    }

}
