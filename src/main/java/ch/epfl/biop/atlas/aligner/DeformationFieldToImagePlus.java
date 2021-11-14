package ch.epfl.biop.atlas.aligner;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.LUT;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeformationFieldToImagePlus {

    static Logger logger = LoggerFactory.getLogger(DeformationFieldToImagePlus.class);

    public static ImagePlus export(SliceSources slice,
                                   int resolutionLevel,
                                   int downsample,
                                   int timepoint,
                                   double tolerance,
                                   int maxIterations) {

        RealTransform rt = ((InvertibleRealTransform)slice
                .getSlicePixToCCFRealTransform(resolutionLevel, tolerance, maxIterations))
                .inverse();

        RandomAccessibleInterval raiModel = slice.getOriginalSources()[0].getSpimSource().getSource(timepoint, resolutionLevel);
        final int sxt = (int) (raiModel.dimension(0));
        final int syt = (int) (raiModel.dimension(1));

        final int sx = sxt/downsample;
        final int sy = syt/downsample;
        final int sz = 3;

        ImageStack stack = new ImageStack(sx, sy, sz);
        stack.setBitDepth(32);

        RealPoint pt = new RealPoint(3);
        float[] pos = new float[]{0,0,(float) (downsample/2.0)};

        int nPix = sx*sy;
        float[] posX = new float[nPix];
        float[] posY = new float[nPix];
        float[] posZ = new float[nPix];

        int index = 0;
        for (int py = 0; py<sy; py++)
        for (int px = 0; px<sx; px++){
            pos[0] = (int)(px*downsample+downsample/2.0);
            pos[1] = (int)(py*downsample+downsample/2.0);
            pt.setPosition(pos);
            rt.apply(pt,pt);
            if (index<nPix) {
                posX[index] = pt.getFloatPosition(0);
                posY[index] = pt.getFloatPosition(1);
                posZ[index] = pt.getFloatPosition(2);
            }
            index++;
        }

        FloatProcessor xpr = new FloatProcessor(sx,sy,posX);
        FloatProcessor ypr = new FloatProcessor(sx,sy,posY);
        FloatProcessor zpr = new FloatProcessor(sx,sy,posZ);

        xpr.setMinAndMax(xpr.minValue(), xpr.maxValue());
        ypr.setMinAndMax(ypr.minValue(), ypr.maxValue());
        zpr.setMinAndMax(zpr.minValue(), zpr.maxValue());

        stack.setProcessor(xpr,1);
        stack.setProcessor(ypr,2);
        stack.setProcessor(zpr,3);

        ImagePlus imp = new ImagePlus(slice.getName()+"_Coords", stack);
        imp.setStack(stack, 3,1,1);

        CompositeImage ci = new CompositeImage(imp, CompositeImage.COMPOSITE);

        ci.setC(1);
        ci.setChannelLut(new LUT(STRIPES[0], STRIPES[1], STRIPES[2]));
        ci.setDisplayRange(xpr.minValue(), xpr.maxValue());

        ci.setC(2);
        ci.setChannelLut(new LUT(STRIPES[2], STRIPES[0], STRIPES[1]));
        ci.setDisplayRange(ypr.minValue(), ypr.maxValue());

        ci.setC(3);
        ci.setChannelLut(new LUT(STRIPES[1], STRIPES[2], STRIPES[0]));
        ci.setDisplayRange(zpr.minValue(), zpr.maxValue());

        return ci;
    }

    public static final byte[][] STRIPES = stripes();

    private static byte[][] stripes( )
    {

        byte[][] lut = new byte[3][256];

        for ( int i = 0; i < 256; i++ ) {
            lut[0][i] = 0;
            double h = Math.cos((double)i/256.0 * Math.PI * 10.0);
            lut[1][i] = (byte) (Math.min((h*h*h*h*254),254)/2.0);
            lut[2][i] = lut[1][i];
        }

        return lut;
    }
}
