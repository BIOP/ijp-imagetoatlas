package ch.epfl.biop.registration.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.plugin.IABBARegistrationPlugin;
import org.scijava.Context;
import org.scijava.plugin.Parameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

abstract public class SourceAndConverterRegistration implements IABBARegistrationPlugin {

    protected SourceAndConverter[] fimg;

    protected SourceAndConverter[] mimg;

    protected SourceAndConverter[] fimg_mask;

    protected SourceAndConverter[] mimg_mask;

    protected int timePoint = 0;

    @Parameter
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
    public void setFixedMask(SourceAndConverter[] fimg) {
        this.fimg_mask = fimg;
    }

    @Override
    public void setMovingMask(SourceAndConverter[] mimg) {
        this.mimg_mask = mimg;
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

    protected Map<String, String> parameters = new HashMap<>();

    @Override
    public Map<String, String> getRegistrationParameters() {
        if (parameters!=null) {
            return parameters;
        } else {
            return new HashMap<>();
        }
    }

    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    //protected static Consumer<String> log = logger::info;//System.out::println;

    //@Override
    //public void setLogger(Consumer<String> logger) {
    //    this.log = logger;
    //}

    protected MultiSlicePositioner.SliceInfo sliceInfo;

    public void setSliceInfo(MultiSlicePositioner.SliceInfo sliceInfo){
        this.sliceInfo = sliceInfo;
    };

    protected static void addToFlatParameters(List<Object> flatParameters, Object... args) {
        flatParameters.addAll(Arrays.asList(args));
    }

    /*protected Supplier<Double> zPosition;

    public void setZPositioner(Supplier<Double> zPosition) {
        this.zPosition = zPosition;
    }*/

}
