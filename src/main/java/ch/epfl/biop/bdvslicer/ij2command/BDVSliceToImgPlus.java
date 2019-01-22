package ch.epfl.biop.bdvslicer.ij2command;

import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
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

@Plugin(type = Command.class, menuPath = "Plugins>BigDataViewer>Current View to ImgPlus")
public class BDVSliceToImgPlus<T extends RealType<T>> implements Command {

    @Parameter
    public int sourceIndex =0;

    @Parameter
    public int mipmapLevel=0;

    @Parameter
    public BigDataViewer bdv;

    @Parameter(type = ItemIO.OUTPUT)
    public ImagePlus imp;

    @Parameter
    public int xSize =100;

    @Parameter
    public int ySize =100;

    @Parameter
    public int zSize =0;

    @Parameter
    public double samplingInXVoxelUnit = 1;

    @Parameter
    public boolean interpolate= true;

    int timepoint =0;

    @Override
    public void run() {
        // Retrieve viewer state from big data viewer
        ViewerState viewerState = bdv.getViewer().getState();
        timepoint = viewerState.getCurrentTimepoint();

        // Get the source
        Source<T> s = (Source<T>) viewerState.getSources().get(sourceIndex).getSpimSource();

        // Interpolation or not
        Interpolation interpolation;
        if (interpolate) {
            interpolation = Interpolation.NLINEAR;
        } else {
            interpolation = Interpolation.NEARESTNEIGHBOR;
        }

        // Get real random accessible from the source
        final RealRandomAccessible< T > ipimg = s.getInterpolatedSource(timepoint, mipmapLevel, interpolation );

        // Get current big dataviewer transformation : source transform and viewer transform
        AffineTransform3D transformedSourceToViewer = new AffineTransform3D(); // Empty Transform
        // 1 - viewer transform
        viewerState.getViewerTransform( transformedSourceToViewer ); // Get current transformation by the viewer state and puts it into sourceToImgPlus

        //Center on the display center of the viewer ...
        double w = bdv.getViewerFrame().getViewerPanel().getDisplay().getWidth();
        double h = bdv.getViewerFrame().getViewerPanel().getDisplay().getHeight();
        transformedSourceToViewer.translate(new double[]{-w/2,-h/2,0});

        // Getting an image independent of the view scaling unit
        double xNorm = getNormTransform(0,transformedSourceToViewer);//trans
        transformedSourceToViewer.scale(samplingInXVoxelUnit/xNorm);

        // Alternative : Get a bounding box from - (TODO interesting related post : https://forum.image.sc/t/using-imglib2-to-shear-an-image/2534/3)

        //2 - source transform
        final AffineTransform3D sourceTransform = new AffineTransform3D();
        s.getSourceTransform(timepoint, mipmapLevel, sourceTransform ); // Get current transformation of the source

        //Composition of source and viewer transform
        transformedSourceToViewer.concatenate( sourceTransform ); // Concatenate viewer state transform and source transform to know the final slice of the source

        //Gets randomAccessible view ...
        RandomAccessible< T > ra = RealViews.affine( ipimg, transformedSourceToViewer ); // Gets the view

        // ... interval
        RandomAccessibleInterval<T> view =
                Views.interval( ra, new long[] { -xSize, -ySize, -zSize}, new long[]{ +xSize, +ySize, +zSize}); //Sets the interval

        // Wraps as an ImagePlus -> ItemIO returned
        imp = ImageJFunctions.wrap(view,bdv.getViewerFrame().getTitle()
                +"- ["+ sourceIndex +","+ timepoint +","+mipmapLevel+"]"
                +"[(+"+ xSize +","+(-xSize)+");"
                +"(+"+ ySize +","+(-ySize)+");"
                +"(+"+ zSize +","+(-zSize)+")]");//+" "+transformation); // Returns an ImagePlus (virtual)

        int nSlices = imp.getNSlices();
        imp.setDimensions(1, nSlices, 1); // Set 3 dimension as Z, not as Channel

    }

    public double getNormTransform(int axis, AffineTransform3D t) {
        double f0 = t.get(axis,0);
        double f1 = t.get(axis,1);
        double f2 = t.get(axis,2);
        return java.lang.Math.sqrt(f0*f0+f1*f1+f2*f2);
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
