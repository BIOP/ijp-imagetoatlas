package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.img.imageplus.ImagePlusHelper;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ij.IJ;
import ij.ImagePlus;
import net.imglib2.display.LinearRange;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SliceToImagePlus {

    static Logger logger = LoggerFactory.getLogger(SliceToImagePlus.class);

    public static <T extends NumericType<T> & NativeType<T>> ImagePlus export(List<SourceAndConverter<?>> sourceList,
                                                                             SliceSources slice,
                                                                             SourcesProcessor preprocess,
                                                                             double px, double py, double sx, double sy,
                                                                             double pixelSizeMillimeter, int timepoint,
                                                                             boolean interpolate) {
        AffineTransform3D at3D = new AffineTransform3D();

        SourceAndConverter model = createModelSource(slice, px, py, sx, sy, pixelSizeMillimeter, at3D);

        List<SourceAndConverter<T>> resampledSourceList = sourceList
                .stream()
                .map(sac -> (SourceAndConverter<T>) (new SourceResampler(sac,model,sac.getSpimSource().getName()+"_ResampledLike_"+model.getSpimSource().getName(), true, false, interpolate,0).get()))
                .collect(Collectors.toList());

        if ((sourceList.size()>1)) {

            Map<SourceAndConverter<T>, Integer> mapMipmap = new HashMap<>();
            sourceList.forEach(src -> {
                int mipmapLevel = SourceAndConverterHelper.bestLevel(src, timepoint, pixelSizeMillimeter);
                logger.debug("Mipmap level chosen for source ["+src.getSpimSource().getName()+"] : "+mipmapLevel);
                mapMipmap.put(resampledSourceList.get(sourceList.indexOf(src)), mipmapLevel);
            });

            ImagePlus resultImage = ImagePlusHelper.wrap(
                    resampledSourceList,
                    mapMipmap,
                    timepoint,
                    1,
                    1);

            resultImage.setTitle(slice.getName()+"-["+px+":"+(px+sx)+" | "+py+":"+(py+sy)+"]");
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

    public static <T extends NumericType<T> & NativeType<T>> ImagePlus export(SliceSources slice,
                                   SourcesProcessor preprocess,
                                   double px, double py, double sx, double sy,
                                   double pixelSizeMillimeter, int timepoint,
                                   boolean interpolate) {

        List<SourceAndConverter> sourceList = Arrays.asList(preprocess.apply(slice.getRegisteredSources()));

        AffineTransform3D at3D = new AffineTransform3D();

        SourceAndConverter model = createModelSource(slice, px, py, sx, sy, pixelSizeMillimeter, at3D);

        List<SourceAndConverter<T>> resampledSourceList = sourceList
                .stream()
                .map(sac -> (SourceAndConverter<T>) (new SourceResampler(sac,model,sac.getSpimSource().getName()+"_ResampledLike_"+model.getSpimSource().getName(),true, false, interpolate,0).get()))
                .collect(Collectors.toList());

        if ((sourceList.size()>1)) {

            Map<SourceAndConverter<T>, Integer> mapMipmap = new HashMap<>();
            sourceList.forEach(src -> {
                int mipmapLevel = SourceAndConverterHelper.bestLevel(src, timepoint, pixelSizeMillimeter);
                logger.debug("Mipmap level chosen for source ["+src.getSpimSource().getName()+"] : "+mipmapLevel);
                mapMipmap.put(resampledSourceList.get(sourceList.indexOf(src)), mipmapLevel);
            });

            ImagePlus resultImage = ImagePlusHelper.wrap(
                    resampledSourceList,
                    mapMipmap,
                    timepoint,
                    1,
                    1);

            resultImage.setTitle(slice.getName()+"-["+px+":"+(px+sx)+" | "+py+":"+(py+sy)+"]");
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

    public static <T extends NumericType<T> & NativeType<T>> ImagePlus exportAtlas(MultiSlicePositioner mp, SliceSources slice,
                                   SourcesProcessor preprocess,
                                   double px, double py, double sx, double sy,
                                   double pixelSizeMillimeter, int timepoint,
                                   boolean interpolate) throws UnsupportedOperationException {

        List<SourceAndConverter<?>> sourceList = Arrays.asList(preprocess.apply(mp.getReslicedAtlas().nonExtendedSlicedSources));//slice.getRegisteredSources()));

        // Put this validation as a static helper function  TODO
        Class<?> firstSourceType = sourceList.get(0).getSpimSource().getType().getClass();

        List<Class<?>> allowedClasses = new ArrayList<>();
        allowedClasses.add(UnsignedShortType.class);
        allowedClasses.add(UnsignedByteType.class);
        allowedClasses.add(FloatType.class);
        allowedClasses.add(ARGBType.class);

        if (!allowedClasses.contains(firstSourceType)) {
            throw new UnsupportedOperationException("Can't export pixel type "+firstSourceType.getClass().getSimpleName()+" to ImagePlus");
        }

        for (SourceAndConverter<?> source : sourceList) {
            if (source.getSpimSource().getType().getClass()!=firstSourceType) {
                throw new UnsupportedOperationException("Can't export combined pixel types ("+firstSourceType.getSimpleName()+") and ("+source.getSpimSource().getType().getClass().getSimpleName()+"). Please select less channels.");
            }
        }

        AffineTransform3D at3D = new AffineTransform3D();

        SourceAndConverter model = createModelSource(slice, px, py, sx, sy, pixelSizeMillimeter, at3D);

        List<SourceAndConverter<T>> resampledSourceList = sourceList
                .stream()
                .map(sac -> (SourceAndConverter<T>) (new SourceResampler(sac,model,sac.getSpimSource().getName()+"_ResampledLike_"+model.getSpimSource().getName(),true, false, interpolate,0).get()))
                .collect(Collectors.toList());

        if ((sourceList.size()>1)) {

            Map<SourceAndConverter<T>, Integer> mapMipmap = new HashMap<>();
            sourceList.forEach(src -> {
                int mipmapLevel = SourceAndConverterHelper.bestLevel(src, timepoint, pixelSizeMillimeter);
                if (!interpolate) mipmapLevel = 0; // For labels
                logger.debug("Mipmap level chosen for source ["+src.getSpimSource().getName()+"] : "+mipmapLevel);
                mapMipmap.put(resampledSourceList.get(sourceList.indexOf(src)), mipmapLevel);
            });

            ImagePlus resultImage = ImagePlusHelper.wrap(
                    resampledSourceList,
                    mapMipmap,
                    timepoint,
                    1,
                    1);

            resultImage.setTitle(slice.getName()+"-["+px+":"+(px+sx)+" | "+py+":"+(py+sy)+"]");
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


    private static SourceAndConverter createModelSource(SliceSources slice,
                                                 double px, double py,
                                                 double sx, double sy,
                                                 double pixel_size_mm,
                                                        AffineTransform3D transform) {
        // Origin is in fact the point 0,0,0 of the image
        // Get current big dataviewer transformation : source transform and viewer transform
        AffineTransform3D at3D = new AffineTransform3D(); // Empty Transform
        // viewer transform
        // Center on the display center of the viewer ...
        // Center on the display center of the viewer ...

        //at3D.scale(1/pixel_size_mm);

        at3D.translate(-px, -py, -slice.getSlicingAxisPosition());

        at3D.scale(1/pixel_size_mm);
        // Getting an image independent of the view scaling unit (not sure)

        long nPx = (long)(sx / pixel_size_mm);
        long nPy = (long)(sy / pixel_size_mm);
        long nPz = 1;

        //if (samplingZInPhysicalUnit==0) {
        //    nPz = 1;
        //} else {
        //    nPz = 1+(long)(zSize / (samplingZInPhysicalUnit/2.0)); // TODO : check div by 2
        //}

        // At least a pixel in all directions
        if (nPz == 0) nPz = 1;
        if (nPx == 0) nPx = 1;
        if (nPy == 0) nPy = 1;

        transform.set(at3D);

        return new EmptySourceAndConverterCreator("model", at3D.inverse(), nPx, nPy, nPz).get();
    }

}
