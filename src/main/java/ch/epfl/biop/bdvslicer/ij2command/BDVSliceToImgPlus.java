package ch.epfl.biop.bdvslicer.ij2command;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.RGBStackMerge;
import ij.process.LUT;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import bdv.BigDataViewer;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.state.ViewerState;
import java.awt.Color;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.lang.Math.sqrt;

/**
 * Command to export a BigDataViewer window into an ImageJ Composite Image
 * The default logic is based on the center of the BigDataViewer window
 * Limitations:
 *  - Single timepoint
 *  - All sources should have an identical bit depth when wrapped in ImageJ
 *  - LUT based on single RGB color
 *  - Isotropic export
 * T needs to be RealType for ImageJ1 Wrapping
 * @param <T>
 *
 * @author Nicolas Chiaruttini
 * BIOP, EPFL, 2019
 */

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Current View to ImgPlus", initializer = "initParams")
public class BDVSliceToImgPlus<T extends RealType<T>> implements Command {

    private static final Logger LOGGER = Logger.getLogger( BDVSliceToImgPlus.class.getName() );

    @Parameter(label="BigDataViewer Window", callback = "matchXYBDVFrame")
    public BigDataViewer bdv;

    @Parameter(label="Source indexes ('2,3-5'), starts at 0")
    public String sourceIndexString = "0";

    @Parameter(label="Mipmap level, 0 for highest resolution")
    public int mipmapLevel = 0;

    @Parameter(label="Match bdv frame window size", persist=false, callback = "matchXYBDVFrame")
    public boolean matchWindowSize=false;

    @Parameter(label = "Number of pixels X", callback = "matchXYBDVFrame")
    public double xSize = 100;

    @Parameter(label = "Number of pixels Y", callback = "matchXYBDVFrame")
    public double ySize = 100;

    @Parameter(label = "Number of slice Z (isotropic vs XY, 0 for single slice)")
    public int zSize = 0;

    @Parameter(label = "Timepoint", persist = false)
    public int timepoint = 0;

    @Parameter(label = "Pixel size output in x voxel unit size at highest resolution", callback = "matchXYBDVFrame")
    public double samplingInXVoxelUnit = 1;

    @Parameter(label = "Interpolate")
    public boolean interpolate = true;

    @Parameter(label = "Parallelize when exporting several channels")
    public boolean wrapMultichannelParallel = true;

    // Output imageplus window
    @Parameter(type = ItemIO.OUTPUT)
    public ImagePlus imp;

    // Map containing wrapped sources, can be accessed in parallel -> Concurrent
    ConcurrentHashMap<Integer,ImagePlus> genImagePlus = new ConcurrentHashMap<>();

    @Override
    public void run() {

        // Transform sourceIndexString to ArrayList of indexes
        ArrayList<Integer> sourceIndexes = commaSeparatedListToArray(sourceIndexString);

        // No source specified, end of Command
        if (sourceIndexes.size()==0) {
            LOGGER.warning( "No source index defined.");
            return;
        }

        // Retrieve viewer state from big data viewer
        ViewerState viewerState = bdv.getViewer().getState();

        //Center on the display center of the viewer ...
        double w = bdv.getViewerFrame().getViewerPanel().getDisplay().getWidth();
        double h = bdv.getViewerFrame().getViewerPanel().getDisplay().getHeight();

        RealPoint pt = new RealPoint(3); // Number of dimension

        //Get global coordinates of the central position  of the viewer
        bdv.getViewerFrame().getViewerPanel().displayToGlobalCoordinates(w/2, h/2, pt);
        double posX = pt.getDoublePosition(0);
        double posY = pt.getDoublePosition(1);
        double posZ = pt.getDoublePosition(2);

        // Stream is single threaded or multithreaded based on boolean parameter
        Stream<Integer> indexStream;
        if (wrapMultichannelParallel) {
            indexStream = sourceIndexes.parallelStream();
        } else {
            indexStream = sourceIndexes.stream();
        }

        // Wrap each source independently
        indexStream.forEach( sourceIndex -> {

            // Get the source
            Source<T> s = (Source<T>) viewerState.getSources().get(sourceIndex).getSpimSource();

            // Interpolation switch
            Interpolation interpolation;
            if (interpolate) {
                interpolation = Interpolation.NLINEAR;
            } else {
                interpolation = Interpolation.NEARESTNEIGHBOR;
            }

            // Get real random accessible from the source
            final RealRandomAccessible<T> ipimg = s.getInterpolatedSource(timepoint, mipmapLevel, interpolation);

            // Get current big dataviewer transformation : source transform and viewer transform
            AffineTransform3D transformedSourceToViewer = new AffineTransform3D(); // Empty Transform
            // viewer transform
            viewerState.getViewerTransform(transformedSourceToViewer); // Get current transformation by the viewer state and puts it into sourceToImgPlus

            // Center on the display center of the viewer ...
            transformedSourceToViewer.translate(-w / 2, -h / 2, 0);

            // Getting an image independent of the view scaling unit (not sure)
            double xNorm = getNormTransform(0, transformedSourceToViewer);//trans
            transformedSourceToViewer.scale(1/samplingInXVoxelUnit);

            // Alternative : Get a bounding box from - (TODO interesting related post : https://forum.image.sc/t/using-imglib2-to-shear-an-image/2534/3)

            // Source transform
            final AffineTransform3D sourceTransform = new AffineTransform3D();
            s.getSourceTransform(timepoint, mipmapLevel, sourceTransform); // Get current transformation of the source

            // Composition of source and viewer transform
            transformedSourceToViewer.concatenate(sourceTransform); // Concatenate viewer state transform and source transform to know the final slice of the source

            // Gets randomAccessible view ...
            RandomAccessible<T> ra = RealViews.affine(ipimg, transformedSourceToViewer); // Gets the view

            // ... interval
            RandomAccessibleInterval<T> view =
                    Views.interval(ra, new long[]{-(int)(xSize/2), -(int)(ySize/2), -zSize}, new long[]{+(int)(xSize/2), +(int)(ySize/2), +zSize}); //Sets the interval

            // Wrap as ImagePlus
            ImagePlus impTemp = ImageJFunctions.wrap(view, "");

            // 'Metadata' for ImagePlus set as a Z stack (instead of a Channel stack by default)
            int nSlices = impTemp.getNSlices();
            impTemp.setDimensions(1, nSlices, 1); // Set 3 dimension as Z, not as Channel

            // Set ImagePlus display properties as in BigDataViewer
            // Min Max
            impTemp.setDisplayRange(
                    bdv.getSetupAssignments().getConverterSetups().get(sourceIndex).getDisplayRangeMin(),
                    bdv.getSetupAssignments().getConverterSetups().get(sourceIndex).getDisplayRangeMax()
            );

            // Simple Color LUT
            ARGBType c = bdv.getSetupAssignments().getConverterSetups().get(sourceIndex).getColor();
            impTemp.setLut(LUT.createLutFromColor(new Color(ARGBType.red(c.get()), ARGBType.green(c.get()), ARGBType.blue(c.get()))));

            // Store result in ConcurrentHashMap
            genImagePlus.put(sourceIndex, impTemp);
        });

        // Merging stacks, if possible, by using RGBStackMerge IJ1 class
        ImagePlus[] orderedArray = sourceIndexes.stream().map(idx -> genImagePlus.get(idx)).toArray(ImagePlus[]::new);
        if (orderedArray.length>1) {
            boolean identicalBitDepth = sourceIndexes.stream().map(idx -> genImagePlus.get(idx).getBitDepth()).distinct().limit(2).count()==1;
            if (identicalBitDepth) {
                imp = RGBStackMerge.mergeChannels(orderedArray, false);
            } else {
                LOGGER.warning("All channels do not have the same bit depth, sending back first channel only");
                imp = orderedArray[0];
            }
        } else {
            imp = orderedArray[0];
        }

        // Title
        String title = bdv.getViewerFrame().getTitle()
                + " - [T=" + timepoint + ", MML=" + mipmapLevel + "]"
                +"[SRC="+sourceIndexString+"]"+"[S="+samplingInXVoxelUnit+"]";
        imp.setTitle(title);

        // Calibration in the limit of what's possible to know and set
        Calibration calibration = new Calibration();
        calibration.setImage(imp);

        // Origin is in fact the center of the image
        calibration.xOrigin=posX;
        calibration.yOrigin=posY;
        calibration.zOrigin=posZ;

        // Scaling factor
        // Isotropic output image
        double globalScaleFactor = 1;
        calibration.pixelWidth=globalScaleFactor;
        calibration.pixelHeight=globalScaleFactor;
        calibration.pixelDepth=globalScaleFactor;

        // Set generated calibration to output image
        imp.setCalibration(calibration);
    }

    /**
     * Convert a comma separated list of indexes into an arraylist of integer
     *
     * For instance 1,2,5-7,10-12,14 returns an ArrayList containing
     * [1,2,5,6,7,10,11,12,14]
     *
     * Invalid format are ignored and an error message is displayed
     *
     * @param expression
     * @return list of indexes in ArrayList
     */

    static public ArrayList<Integer> commaSeparatedListToArray(String expression) {
        String[] splitIndexes = expression.split(",");
        ArrayList<java.lang.Integer> arrayOfIndexes = new ArrayList<>();
        for (String str : splitIndexes) {
            str.trim();
            if (str.contains("-")) {
                // Array of source, like 2-5 = 2,3,4,5
                String[] boundIndex = str.split("-");
                if (boundIndex.length==2) {
                    try {
                        int binf = java.lang.Integer.valueOf(boundIndex[0].trim());
                        int bsup = java.lang.Integer.valueOf(boundIndex[1].trim());
                        for (int index = binf; index <= bsup; index++) {
                            arrayOfIndexes.add(index);
                        }
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                    }
                } else {
                    LOGGER.warning("Cannot parse expression "+str+" to pattern 'begin-end' (2-5) for instance, omitted");
                }
            } else {
                // Single source
                try {
                    int index = java.lang.Integer.valueOf(str.trim());
                    arrayOfIndexes.add(index);
                } catch (NumberFormatException e) {
                    LOGGER.warning("Number format problem with expression:"+str+" - Expression ignored");
                }
            }
        }
        return arrayOfIndexes;
    }

    /**
     * Returns the norm of an axis after an affinetransform is applied
     * @param axis
     * @param t
     * @return
     */
    static public double getNormTransform(int axis, AffineTransform3D t) {
        double f0 = t.get(axis,0);
        double f1 = t.get(axis,1);
        double f2 = t.get(axis,2);
        return sqrt(f0 * f0 + f1 * f1 + f2 * f2);
    }

    /**
     * Returns the distance between two RealPoint pt1 and pt2
     * @param pt1
     * @param pt2
     * @return
     */
    static public double distance(RealPoint pt1, RealPoint pt2) {
        assert pt1.numDimensions()==pt2.numDimensions();
        double dsquared = 0;
        for (int i=0;i<pt1.numDimensions();i++) {
            double diff = pt1.getDoublePosition(i)-pt2.getDoublePosition(i);
            dsquared+=diff*diff;
        }
        return Math.sqrt(dsquared);
    }

    // -- Initializers --

    /**
     * Initializes xSize and ySize according to the current BigDataViewer window
     */
    public void matchXYBDVFrame() {
        if (matchWindowSize) {
            // Gets window size
            double w = bdv.getViewerFrame().getViewerPanel().getDisplay().getWidth();
            double h = bdv.getViewerFrame().getViewerPanel().getDisplay().getHeight();

            // Get global coordinates of the top left position  of the viewer
            RealPoint ptTopLeft = new RealPoint(3); // Number of dimension
            bdv.getViewerFrame().getViewerPanel().displayToGlobalCoordinates(0, 0, ptTopLeft);

            // Get global coordinates of the top right position  of the viewer
            RealPoint ptTopRight = new RealPoint(3); // Number of dimension
            bdv.getViewerFrame().getViewerPanel().displayToGlobalCoordinates(0, w, ptTopRight);

            // Get global coordinates of the top right position  of the viewer
            RealPoint ptBottomLeft = new RealPoint(3); // Number of dimension
            bdv.getViewerFrame().getViewerPanel().displayToGlobalCoordinates(h,0, ptBottomLeft);

            // Get current big dataviewer transformation : source transform and viewer transform
            AffineTransform3D transformedSourceToViewer = new AffineTransform3D(); // Empty Transform

            // viewer transform
            ViewerState viewerState = bdv.getViewer().getState();
            viewerState.getViewerTransform(transformedSourceToViewer); // Get current transformation by the viewer state and puts it into sourceToImgPlus

            // Getting an image independent of the view scaling unit (not sure)
            double xNorm = getNormTransform(0, transformedSourceToViewer);//trans

            // Gets number of pixels based on window size, image sampling size and user requested pixel size
            this.xSize=(int) (distance(ptTopLeft, ptTopRight)*xNorm/samplingInXVoxelUnit);
            this.ySize=(int) (distance(ptTopLeft, ptBottomLeft)*xNorm/samplingInXVoxelUnit);
        }
    }


    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(OpenBDVServer.class, true,
                "urlServer","http://fly.mpi-cbg.de:8081",
                "datasetName", "Drosophila").get();
        ij.command().run(BDVSliceToImgPlus.class, true);//.getOutput();
    }
}
