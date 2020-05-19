package ch.epfl.biop.atlastoimg2d.commands.multislices;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.MultiResolutionRenderer;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All specific functions and method dedicated to the multislice positioner
 */

public class MultiSlicePositioner extends BdvOverlay {

    final BdvHandle bdvh;
    final MultiSlicePositioner.ZStepSetter zStepSetter;
    final SourceAndConverter slicingModel;

    int iSliceNoStep = 0, iSlice = 0;

    List<SliceSources> slices;


    //final BdvOverlay bdvOverlay;

    public MultiSlicePositioner(BdvHandle bdvh, SourceAndConverter slicingModel) {
        this.bdvh = bdvh;
        this.slicingModel = slicingModel;
        zStepSetter = new ZStepSetter();
        this.bdvh.getViewerPanel().setTransferHandler(new MultiSlicePositioner.TransferHandler());

        BdvFunctions.showOverlay( this, "MultiSlice Overlay", BdvOptions.options().addTo( bdvh ) );

    }

    public ZStepSetter getzStepSetter() {
        return zStepSetter;
    }

    @Override
    protected void draw(Graphics2D g) {
        int colorCode = this.info.getColor().get();
        g.setColor(new Color(ARGBType.red(colorCode) , ARGBType.green(colorCode), ARGBType.blue(colorCode), ARGBType.alpha(colorCode) ));

        int nPixX = (int) slicingModel.getSpimSource().getSource(0,0).dimension(0);
        int nPixY = (int) slicingModel.getSpimSource().getSource(0,0).dimension(1);

        double width = nPixX*0.01;//at3D.get(0,0); // TODO fix
        double height = nPixY*0.01;//at3D.get(1,1);

        RealPoint[][] ptRectWorld = new RealPoint[2][2];
        Point[][] ptRectScreen = new Point[2][2];

        AffineTransform3D bdvAt3D = new AffineTransform3D();

        bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);

        for (int xp = 0; xp < 2; xp++) {
            for (int yp = 0; yp < 2; yp++) {
                ptRectWorld[xp][yp] = new RealPoint(3);
                RealPoint pt = ptRectWorld[xp][yp];
                pt.setPosition(width * (iSliceNoStep + xp), 0);
                pt.setPosition(height * (1 + yp), 1);
                pt.setPosition(0, 2);
                bdvAt3D.apply(pt, pt);
                ptRectScreen[xp][yp] = new Point((int) pt.getDoublePosition(0), (int) pt.getDoublePosition(1));
            }
        }

        g.drawLine(ptRectScreen[0][0].x, ptRectScreen[0][0].y, ptRectScreen[1][0].x, ptRectScreen[1][0].y);
        g.drawLine(ptRectScreen[1][0].x, ptRectScreen[1][0].y, ptRectScreen[1][1].x, ptRectScreen[1][1].y);
        g.drawLine(ptRectScreen[1][1].x, ptRectScreen[1][1].y, ptRectScreen[0][1].x, ptRectScreen[0][1].y);
        g.drawLine(ptRectScreen[0][1].x, ptRectScreen[0][1].y, ptRectScreen[0][0].x, ptRectScreen[0][0].y);

    }

    /**
     * TransferHandler class :
     * Controls drag and drop actions in the multislice positioner
     */
    class TransferHandler extends BdvTransferHandler {

        @Override
        public void updateDropLocation(TransferSupport support, DropLocation dl) {
            // Gets the point in real coordinates
            RealPoint pt3d = new RealPoint(3);
            bdvh.getViewerPanel().displayToGlobalCoordinates(dl.getDropPoint().x, dl.getDropPoint().y, pt3d);

            // Gets the location along the slicing axis...
            // First gets which z slice is targeted

            // Gets the width of the slicing model

            int nPixX = (int) slicingModel.getSpimSource().getSource(0,0).dimension(0);

            double widthX = nPixX*0.01;//at3D.get(0,0); // TODO Fix this

            iSliceNoStep = (int) (pt3d.getDoublePosition(0)/widthX);

            iSlice = iSliceNoStep*(int) zStepSetter.getStep();

            //Repaint the overlay only
            bdvh.getViewerPanel().paint();

        }

        @Override
        public void importSourcesAndConverters(TransferSupport support, List<SourceAndConverter<?>> sacs) {

            int nPixX = (int) slicingModel.getSpimSource().getSource(0,0).dimension(0);
            int nPixY = (int) slicingModel.getSpimSource().getSource(0,0).dimension(1);
            int nPixZ = (int) slicingModel.getSpimSource().getSource(0,0).dimension(1);

            double width = nPixX*0.01;//at3D.get(0,0); // TODO Fix this
            double height = nPixY*0.01;

            List<SourceAndConverter<?>> sacsTransformed = new ArrayList<>();
            Optional<BdvHandle> bdvh = getBdvHandleFromViewerPanel(((bdv.viewer.ViewerPanel)support.getComponent()));
            if (bdvh.isPresent()) {

                for (SourceAndConverter sac : sacs) {
                    // Affine Transform source such as the center is located at the center of where
                    // we want it to be
                    AffineTransform3D at3D = new AffineTransform3D();
                    at3D.identity();
                    //double[] m = at3D.getRowPackedCopy();
                    sac.getSpimSource().getSourceTransform(0,0,at3D);
                    long[] dims = new long[3];
                    sac.getSpimSource().getSource(0,0).dimensions(dims);

                    RealPoint ptCenterGlobal = new RealPoint(3);
                    RealPoint ptCenterPixel = new RealPoint((double)(dims[0]-1.0)/2.0, (double)(dims[1]-1.0)/2.0, (double)(dims[2]-1.0)/2.0);

                    at3D.apply(ptCenterPixel, ptCenterGlobal);

                    // Just shifting
                    double[] m = new AffineTransform3D().getRowPackedCopy();

                    RealPoint shift = new RealPoint(3);

                    shift.setPosition(new double[]{(iSliceNoStep+0.5)*width, 1.5*height, 0});

                    m[3] = shift.getDoublePosition(0)-ptCenterGlobal.getDoublePosition(0);

                    m[7] = shift.getDoublePosition(1)-ptCenterGlobal.getDoublePosition(1);

                    m[11] = shift.getDoublePosition(2)-ptCenterGlobal.getDoublePosition(2);

                    at3D.set(m);

                    sacsTransformed.add(new SourceAffineTransformer(sac, at3D).getSourceOut());
                }

                SourceAndConverterServices.getSourceAndConverterDisplayService()
                        .show(bdvh.get(), sacsTransformed.toArray(new SourceAndConverter[sacsTransformed.size()]));

            }
        }

    }

    /**
     * Simple to enable setting z step and allowing its communication
     * in multiple Commands
     */
    public static class ZStepSetter {

        private int zStep = 1;

        public void setStep(int zStep) {
            if (zStep>0) {
                this.zStep = zStep;
            }
        }

        public long getStep() {
            return (long) zStep;
        }
    }

    /**
     * Inner class which contains the information necessary for the viewing of the slices:
     *
     */
    private class SliceSources {
        // What are they ?
        SourceAndConverter[] sacs;

        // Where are they ?
        int iSlice;

        // For fast display : TODO

    }
}