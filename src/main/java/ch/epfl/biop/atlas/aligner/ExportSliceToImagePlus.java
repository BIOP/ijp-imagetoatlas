package ch.epfl.biop.atlas.aligner;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessor;
import ch.epfl.biop.atlas.plugin.RegistrationPluginHelper;
import ij.ImagePlus;
import net.imglib2.converter.Converter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.listeners.Listeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import spimdata.imageplus.ImagePlusHelper;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExportSliceToImagePlus extends CancelableAction {

    Logger logger = LoggerFactory.getLogger(ExportSliceToImagePlus.class);

    final SliceSources slice;
    final SourcesProcessor preprocess;
    final double px, py, sx, sy, pixel_size_mm;
    final int timepoint;
    final boolean interpolate;

    ImagePlus resultImage = null;
    AffineTransform3D at3D;

    public ExportSliceToImagePlus(MultiSlicePositioner mp,
                         SliceSources slice,
                         SourcesProcessor preprocess,
                                  double px, double py, double sx, double sy,
                                  double pixel_size_millimeter, int timepoint,
                                  boolean interpolate) {
        super(mp);
        this.slice = slice;
        this.preprocess = preprocess;
        this.px = px;
        this.py = py;
        this.sx = sx;
        this.sy = sy;
        this.pixel_size_mm = pixel_size_millimeter;
        this.timepoint = timepoint;
        this.interpolate = interpolate;
    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    @Override
    protected boolean run() {

        List<SourceAndConverter> sourceList = Arrays.asList(preprocess.apply(slice.getRegisteredSources()));

        SourceAndConverter model = createModelSource();

        List<SourceAndConverter> resampledSourceList = sourceList
                .stream()
                .map(sac -> new SourceResampler(sac,model,true, false, interpolate).get())
                .collect(Collectors.toList());

        boolean ignoreSourceLut = false;

        if ((sourceList.size()>1)) {

            Map<SourceAndConverter, ConverterSetup> mapCS = new HashMap<>();
            sourceList.forEach(src -> mapCS.put(resampledSourceList.get(sourceList.indexOf(src)),
                    getConverterSetupFromConverter(src.getConverter())));

            Map<SourceAndConverter, Integer> mapMipmap = new HashMap<>();
            sourceList.forEach(src -> {
                int mipmapLevel = SourceAndConverterHelper.bestLevel(src, timepoint, pixel_size_mm);
                logger.debug("Mipmap level chosen for source ["+src.getSpimSource().getName()+"] : "+mipmapLevel);
                mapMipmap.put(resampledSourceList.get(sourceList.indexOf(src)), mipmapLevel);
            });

            resultImage = ImagePlusHelper.wrap(
                    resampledSourceList,
                    mapCS,
                    mapMipmap,
                    timepoint,
                    timepoint+1,
                    ignoreSourceLut);

            resultImage.setTitle(slice.getName()+"-["+px+":"+(px+sx)+" | "+py+":"+(py+sy)+"]");
            ImagePlusHelper.storeExtendedCalibrationToImagePlus(resultImage, at3D.inverse(), "mm", timepoint);

        } else {
            resampledSourceList.forEach(source -> {
                int mipmapLevel = SourceAndConverterHelper.bestLevel(sourceList.get(0), timepoint, pixel_size_mm);
                ImagePlus singleChannel = ImagePlusHelper.wrap(
                        source,
                        getConverterSetupFromConverter(source.getConverter()),
                        mipmapLevel,
                        timepoint,
                        timepoint+1,
                        ignoreSourceLut);
                singleChannel.setTitle(source.getSpimSource().getName());
                ImagePlusHelper.storeExtendedCalibrationToImagePlus(singleChannel, at3D.inverse(), "mm", timepoint);
                if (resampledSourceList.size()>1) {
                    singleChannel.show();
                } else {
                    resultImage = singleChannel;
                }
            });
        }

        return true;
    }

    @Override
    protected boolean cancel() {
        clean(); // Allows GC
        return true;
    }

    public ImagePlus getImagePlus() {
        return resultImage;
    }

    public void clean() {
        resultImage = null;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        switch (slice.getActionState(this)) {
            case "(done)":
                g.setColor(new Color(0, 255, 0, 200));
                break;
            case "(locked)":
                g.setColor(new Color(255, 0, 0, 200));
                break;
            case "(pending)":
                g.setColor(new Color(255, 255, 0, 200));
                break;
        }
        g.fillRect((int) (px - 7), (int) (py - 7), 14, 14);
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("E", (int) px - 4, (int) py + 5);
    }


    private SourceAndConverter createModelSource() {
        // Origin is in fact the point 0,0,0 of the image
        // Get current big dataviewer transformation : source transform and viewer transform
        at3D = new AffineTransform3D(); // Empty Transform
        // viewer transform
        // Center on the display center of the viewer ...
        // Center on the display center of the viewer ...

        //at3D.scale(1/pixel_size_mm);

        at3D.translate(-px, -py, -slice.getZAxisPosition());

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

        return new EmptySourceAndConverterCreator("model", at3D.inverse(), nPx, nPy, nPz).get();
    }


    public ConverterSetup getConverterSetupFromConverter(final Converter converter) {
        return new ConverterSetup() {
            @Override
            public Listeners<SetupChangeListener> setupChangeListeners() {
                return null;
            }

            @Override
            public int getSetupId() {
                return 0;
            }

            @Override
            public void setDisplayRange(double min, double max) {

            }

            @Override
            public void setColor(ARGBType color) {

            }

            @Override
            public boolean supportsColor() {
                return converter instanceof RealARGBColorConverter;
            }

            @Override
            public double getDisplayRangeMin() {
                return ((RealARGBColorConverter) converter).getMin();
            }

            @Override
            public double getDisplayRangeMax() {
                return ((RealARGBColorConverter) converter).getMax();
            }

            @Override
            public ARGBType getColor() {
                return ((RealARGBColorConverter) converter).getColor();
            }
        };
    }
}
