package ch.epfl.biop.abba.plugin.py;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.plugin.ExternalABBARegistrationPlugin;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.scijava.Context;
import org.scijava.command.Command;

import java.util.HashMap;
import java.util.Map;

public class ExternalIdentityRegistrationPlugin implements ExternalABBARegistrationPlugin {

    Map<String, String> params = new HashMap<>();

    public String getRegistrationTypeName() {
        return "PyIdentity";
    }

    @Override
    public void setSliceInfo(MultiSlicePositioner.SliceInfo sliceInfo) {
        // Do nothing
    }

    @Override
    public boolean isManual() {
        return false;
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public Class<? extends Command>[] userInterface() {
        return new Class[0];
    }

    private Context context;

    @Override
    public void setScijavaContext(Context context) {
        this.context = context;
    }

    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {
        this.params = parameters;
    }

    @Override
    public Map<String, String> getRegistrationParameters() {
        return params;
    }

    SourceAndConverter<?>[] fimg;
    SourceAndConverter<?>[] mimg;

    @Override
    public void setFixedImage(SourceAndConverter<?>[] fimg) {
        this.fimg = fimg;
    }

    @Override
    public void setMovingImage(SourceAndConverter<?>[] mimg) {
        this.mimg = mimg;
    }

    @Override
    public void setFixedMask(SourceAndConverter<?>[] fimg_mask) {

    }

    @Override
    public void setMovingMask(SourceAndConverter<?>[] mimg_mask) {

    }

    @Override
    public void resetRegistration() {
        isDone = false;
    }

    int timepoint = 0;

    @Override
    public void setTimePoint(int timePoint) {
        this.timepoint = timePoint;
    }

    protected boolean isDone = false;

    @Override
    public boolean register() {
        System.out.println("Registering...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Done");
        isDone = true;
        return true;
    }

    @Override
    public boolean edit() {
        throw new UnsupportedOperationException("PyIdentity registration plugin is not editable");
    }

    @Override
    public boolean isRegistrationDone() {
        return isDone;
    }

    @Override
    public SourceAndConverter<?>[] getTransformedImageMovingToFixed(SourceAndConverter<?>[] img) {
        return img;
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {
        return pts;
    }

    @Override
    public void abort() {

    }

    @Override
    public String getTransform() {
        return "";
    }

    @Override
    public void setTransform(String serialized_transform) {
        isDone = true;
    }

    @Override
    public RealTransform getTransformAsRealTransform() {
        return new AffineTransform3D();
    }
}
