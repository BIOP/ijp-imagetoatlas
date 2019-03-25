package ch.epfl.biop.registration;

import bdv.ij.ApplyBigwarpPlugin;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.viewer.DisplayMode;
import bdv.viewer.VisibilityAndGrouping;
import bigwarp.BigWarp;
import bigwarp.BigWarpInit;
import ch.epfl.biop.fiji.imageplusutils.ImagePlusFunctions;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.WaitForUserDialog;
import ij.process.LUT;
import mpicbg.spim.data.SpimDataException;
import bdv.viewer.Interpolation;
import net.imglib2.RealPoint;
import net.imglib2.type.numeric.ARGBType;

import java.util.function.Function;

public class BigWarp2DRegistration implements Registration<ImagePlus> {

    ImagePlus fimg, mimg;

    @Override
    public void setFixedImage(ImagePlus fimg) {
        this.fimg = fimg;
    }

    @Override
    public void setMovingImage(ImagePlus mimg) {
        this.mimg = mimg;
    }

    BigWarp bw;

    @Override
    public void register() {
        try
        {
            //new RepeatingReleasedEventsFixer().install();
            bw = new BigWarp( BigWarpInit.createBigWarpDataFromImages( this.mimg, this.fimg ), "Big Warp",  null ); // pb with virtualstack fimg

            int shiftChannel = this.mimg.getNChannels();
            if (this.mimg instanceof CompositeImage) {
                transferChannelSettings((CompositeImage) this.mimg, bw.getSetupAssignments(), bw.getViewerFrameP().getViewerPanel().getVisibilityAndGrouping(), 0);
            } else {
                transferImpSettings(this.mimg, bw.getSetupAssignments(), 0);
            }

            if (this.fimg instanceof CompositeImage) {
                transferChannelSettings((CompositeImage) this.fimg, bw.getSetupAssignments(), bw.getViewerFrameQ().getViewerPanel().getVisibilityAndGrouping(), shiftChannel);
            } else {
                transferImpSettings(this.fimg, bw.getSetupAssignments(), shiftChannel);
            }

            bw.getViewerFrameP().getViewerPanel().requestRepaint();
            bw.getViewerFrameQ().getViewerPanel().requestRepaint();
            bw.getLandmarkFrame().repaint();

            WaitForUserDialog dialog = new WaitForUserDialog("Choose slice","Please perform carefully your registration then press ok.");
            dialog.show();

            bw.getViewerFrameP().setVisible(false);
            bw.getViewerFrameQ().setVisible(false);
            bw.getLandmarkFrame().setVisible(false);

        }
        catch (final SpimDataException e)
        {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public Function<ImagePlus, ImagePlus> getImageRegistration() {
        // See https://github.com/saalfeldlab/bigwarp/blob/e490dd2ce87c6bcf3355e01e562586421f978303/scripts/Apply_Bigwarp_Xfm.groovy
        return ((img) ->
            ImagePlusFunctions.splitApplyRecompose(
                    imp -> ApplyBigwarpPlugin.apply(
                                imp, fimg, bw.getLandmarkPanel().getTableModel(),
                                "Target", "", "Target",
                                null, null, null,
                                Interpolation.NEARESTNEIGHBOR, false, 1 ),
                    img));
    }

    @Override
    public RealPointList getPtsRegistration(RealPointList pts) {
        for (RealPoint pt : pts.ptList) {
            double[] tr = bw.getTransform().apply( new double[] {
                pt.getDoublePosition(0), pt.getDoublePosition(1)
            });
            pt.setPosition(tr);
        }
        return pts;
    }

    /* Methods copied from BigDataviewer FIJI plugin  -> because they have protected access */

    public void transferChannelSettings(final CompositeImage ci, final SetupAssignments setupAssignments, final VisibilityAndGrouping visibility, int shiftChannel )
    {
        final int nChannels = ci.getNChannels();
        final int mode = ci.getCompositeMode();
        final boolean transferColor = mode == IJ.COMPOSITE || mode == IJ.COLOR;
        for ( int c = 0; c < nChannels; ++c )
        {
            final LUT lut = ci.getChannelLut( c + 1 );
            final ConverterSetup setup = setupAssignments.getConverterSetups().get( c + shiftChannel);
            if ( transferColor )
                setup.setColor( new ARGBType( lut.getRGB( 255 ) ) );
            setup.setDisplayRange( lut.min, lut.max );
        }
        if ( mode == IJ.COMPOSITE )
        {
            final boolean[] activeChannels = ci.getActiveChannels();
            visibility.setDisplayMode( DisplayMode.FUSED );
            //for ( int i = 0; i < activeChannels.length; ++i )
            //    visibility.setSourceActive( i+shiftChannel, activeChannels[ i ] );
        }
        else
            visibility.setDisplayMode( DisplayMode.SINGLE );

        visibility.setGroupingEnabled(true);
        //visibility.setCurrentSource( ci.getChannel() - 1 + shiftChannel);
    }

    public void transferImpSettings( final ImagePlus imp, final SetupAssignments setupAssignments, int shiftChannel )
    {
        //final ConverterSetup setup = setupAssignments.getConverterSetups().get( 0 );
        //setup.setDisplayRange( imp.getDisplayRangeMin(), imp.getDisplayRangeMax() );

        final int nChannels = imp.getNChannels();
        //final int mode = ci.getCompositeMode();
        //final boolean transferColor = mode == IJ.COMPOSITE || mode == IJ.COLOR;
        for ( int c = 0; c < nChannels; ++c )
        {

            final LUT lut = imp.getLuts()[c];//ci.getChannelLut( c + 1 );
            final ConverterSetup setup = setupAssignments.getConverterSetups().get( c + shiftChannel);
            //if ( transferColor )
                setup.setColor( new ARGBType( lut.getRGB( 255 ) ) );
            setup.setDisplayRange( lut.min, lut.max );
        }

    }


}
