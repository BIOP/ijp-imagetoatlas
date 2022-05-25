package ch.epfl.biop.atlas.aligner.plugin;

import bdv.util.BoundedRealTransform;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ij.ImagePlus;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.RealTransform;
import org.scijava.Context;
import org.scijava.command.Command;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.persist.ScijavaGsonHelper;
import spimdata.imageplus.ImagePlusHelper;

import java.util.*;
import java.util.stream.Collectors;

public class SimpleRegistrationWrapper implements ExternalABBARegistrationPlugin {

    public String getRegistrationTypeName() {
        return registrationTypeName;
    }

    public SimpleRegistrationWrapper(String registrationTypeName, final SimpleABBARegistrationPlugin simpleRegistration) {
        this.registration = simpleRegistration;
        this.registrationTypeName = registrationTypeName;
    }

    final String registrationTypeName;
    final SimpleABBARegistrationPlugin registration;

    Context context;

    Map<String, String> parameters;

    boolean isDone = false;

    protected RealTransform rt;

    @Override
    public void setSliceInfo(MultiSlicePositioner.SliceInfo sliceInfo) {

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

    @Override
    public void setScijavaContext(Context context) {
        this.context = context;
    }

    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Map<String, String> getRegistrationParameters() {
        return parameters;
    }

    ImagePlus fixedImage, movingImage, fixedMask, movingMask;

    @Override
    public void setFixedImage(SourceAndConverter<?>[] fimg) {
        fixedImage = export("Fixed-", Arrays.asList(fimg),
                (Double.parseDouble(parameters.get("px"))),
                (Double.parseDouble(parameters.get("py"))),
                (Double.parseDouble(parameters.get("sx"))),
                (Double.parseDouble(parameters.get("sy"))),
                registration.getVoxelSizeInMicron()/1000.0,
                0,
                false
        );
    }

    @Override
    public void setMovingImage(SourceAndConverter<?>[] mimg) {
        movingImage = export("Moving-", Arrays.asList(mimg),
                (Double.parseDouble(parameters.get("px"))),
                (Double.parseDouble(parameters.get("py"))),
                (Double.parseDouble(parameters.get("sx"))),
                (Double.parseDouble(parameters.get("sy"))),
                registration.getVoxelSizeInMicron()/1000.0,
                0,
                false
        );
    }

    @Override
    public void setFixedMask(SourceAndConverter<?>[] fimg_mask) {
        fixedMask = export("FixedMask-", Arrays.asList(fimg_mask),
                (Double.parseDouble(parameters.get("px"))),
                (Double.parseDouble(parameters.get("py"))),
                (Double.parseDouble(parameters.get("sx"))),
                (Double.parseDouble(parameters.get("sy"))),
                registration.getVoxelSizeInMicron()/1000.0,
                0,
                false
        );
    }

    @Override
    public void setMovingMask(SourceAndConverter<?>[] mimg_mask) {
        movingMask = export("MovingMask-", Arrays.asList(mimg_mask),
                (Double.parseDouble(parameters.get("px"))),
                (Double.parseDouble(parameters.get("py"))),
                (Double.parseDouble(parameters.get("sx"))),
                (Double.parseDouble(parameters.get("sy"))),
                registration.getVoxelSizeInMicron()/1000.0,
                0,
                false
        );
    }

    @Override
    public void resetRegistration() {
        isDone = false;
    }

    @Override
    public void setTimePoint(int timePoint) {
        // ignored: it's 0
    }

    @Override
    public boolean register() {
        InvertibleRealTransform inner_rt = registration.register(fixedImage, movingImage, fixedMask, movingMask);

        // This transform is for pixel to pixel coordinates -> we need to convert it to physical space.
        // Thus adding an AffineTransform to a sequence

        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();
        AffineTransform3D m = new AffineTransform3D();

        double px = (Double.parseDouble(parameters.get("px")));
        double py = (Double.parseDouble(parameters.get("py")));
        double voxSize = registration.getVoxelSizeInMicron();

        // We work in millimeters
        m.scale(voxSize/1000.0);
        m.translate(px, py, 0);

        // The umbrella trick
        irts.add(m.inverse());
        irts.add(inner_rt);
        irts.add(m);

        rt = irts;

        isDone = true;
        return true; // no error handling
    }

    @Override
    public boolean edit() {
        // TODO : find a way to edit an affine transform -> that shouldn't be so complicated
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRegistrationDone() {
        return isDone;
    }

    @Override
    public SourceAndConverter<?>[] getTransformedImageMovingToFixed(SourceAndConverter<?>[] img) {
        SourceAndConverter[] out = new SourceAndConverter[img.length];
        SourceRealTransformer srt = new SourceRealTransformer(rt);
        for (int idx = 0;idx<img.length;idx++) {
            out[idx] = srt.apply(img[idx]);
        }
        return out;
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {
        RealTransform innerRT = rt; // To unbox bounded realtransform

        // Unbox bounded transform
        if (rt instanceof BoundedRealTransform) {
            innerRT = ((BoundedRealTransform)rt).getTransform().copy();
        }

        ArrayList<RealPoint> cvtList = new ArrayList<>();

        for (RealPoint p : pts.ptList) {
            RealPoint pt3d = new RealPoint(3);
            pt3d.setPosition(new double[]{p.getDoublePosition(0), p.getDoublePosition(1),0});
            innerRT.apply(pt3d, pt3d);
            RealPoint cpt = new RealPoint(pt3d.getDoublePosition(0), pt3d.getDoublePosition(1));
            cvtList.add(cpt);
        }

        return new RealPointList(cvtList);
    }

    @Override
    public void abort() {

    }

    public RealTransform getRealTransform() {
        return rt;
    }

    public void setRealTransform(RealTransform transform) {
        this.rt = transform.copy();
    }

    @Override
    final public String getTransform() {
        //logger.debug("Serializing transform of class "+rt.getClass().getSimpleName());
        String transform = ScijavaGsonHelper.getGson(context).toJson(rt, RealTransform.class);
        //logger.debug("Serialization result = "+transform);
        return transform;
    }

    @Override
    final public void setTransform(String serialized_transform) {
        setRealTransform(ScijavaGsonHelper.getGson(context).fromJson(serialized_transform, RealTransform.class));
        isDone = true;
    }

    public RealTransform getTransformAsRealTransform() {
        return rt.copy();
    }


    public static ImagePlus export(String name, List<SourceAndConverter<?>> sourceList,
                                   double px, double py, double sx, double sy,
                                   double pixelSizeMillimeter, int timepoint,
                                   boolean interpolate) {

        AffineTransform3D at3D = new AffineTransform3D();

        SourceAndConverter model = createModelSource(px, py, sx, sy, pixelSizeMillimeter, at3D);

        List<SourceAndConverter> resampledSourceList = sourceList
                .stream()
                .map(sac -> new SourceResampler(sac,model,sac.getSpimSource().getName()+"_ResampledLike_"+model.getSpimSource().getName(), true, false, interpolate,0).get())
                .collect(Collectors.toList());

        if ((sourceList.size()>1)) {

            Map<SourceAndConverter, Integer> mapMipmap = new HashMap<>();
            sourceList.forEach(src -> {
                int mipmapLevel = SourceAndConverterHelper.bestLevel(src, timepoint, pixelSizeMillimeter);
                //logger.debug("Mipmap level chosen for source ["+src.getSpimSource().getName()+"] : "+mipmapLevel);
                mapMipmap.put(resampledSourceList.get(sourceList.indexOf(src)), mipmapLevel);
            });

            ImagePlus resultImage = ImagePlusHelper.wrap(
                    resampledSourceList,
                    mapMipmap,
                    timepoint,
                    1,
                    1);

            resultImage.setTitle(name+"-["+px+":"+(px+sx)+" | "+py+":"+(py+sy)+"]");
            ImagePlusHelper.storeExtendedCalibrationToImagePlus(resultImage, at3D.inverse(), "mm", timepoint);

            return resultImage;

        } else {
            SourceAndConverter source = resampledSourceList.get(0);
            int mipmapLevel = SourceAndConverterHelper.bestLevel(sourceList.get(0), timepoint, pixelSizeMillimeter);
            ImagePlus singleChannel = ImagePlusHelper.wrap(
                    source,
                    mipmapLevel,
                    timepoint,
                    1,
                    1);
            singleChannel.setTitle(source.getSpimSource().getName());
            ImagePlusHelper.storeExtendedCalibrationToImagePlus(singleChannel, at3D.inverse(), "mm", timepoint);

            return singleChannel;
        }

    }


    private static SourceAndConverter createModelSource(
                                                        double px, double py,
                                                        double sx, double sy,
                                                        double pixel_size_mm,
                                                        AffineTransform3D transform) {
        // Origin is in fact the point 0,0,0 of the image
        // Get current big dataviewer transformation : source transform and viewer transform
        AffineTransform3D at3D = new AffineTransform3D(); // Empty Transform
        // viewer transform
        // Center on the display center of the viewer ...
        at3D.translate(-px, -py, 0);

        at3D.scale(1/pixel_size_mm);

        // Getting an image independent of the view scaling unit (not sure)
        long nPx = (long)(sx / pixel_size_mm);
        long nPy = (long)(sy / pixel_size_mm);
        long nPz = 1;

        // At least a pixel in all directions
        if (nPz == 0) nPz = 1;
        if (nPx == 0) nPx = 1;
        if (nPy == 0) nPy = 1;

        transform.set(at3D);

        return new EmptySourceAndConverterCreator("model", at3D.inverse(), nPx, nPy, nPz).get();
    }

}
