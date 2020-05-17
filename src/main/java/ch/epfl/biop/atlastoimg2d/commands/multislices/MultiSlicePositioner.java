package ch.epfl.biop.atlastoimg2d.commands.multislices;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.viewer.SourceAndConverter;
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

    //final BdvOverlay bdvOverlay;

    public MultiSlicePositioner(BdvHandle bdvh, SourceAndConverter slicingModel) {
        this.bdvh = bdvh;
        this.slicingModel = slicingModel;
        zStepSetter = new ZStepSetter();
        this.bdvh.getViewerPanel().setTransferHandler(new MultiSlicePositioner.TransferHandler());

        BdvFunctions.showOverlay( this, "overlay", BdvOptions.options().addTo( bdvh ) );
    }

    public ZStepSetter getzStepSetter() {
        return zStepSetter;
    }

    @Override
    protected void draw(Graphics2D g) {
        int colorCode = this.info.getColor().get();
        int w = bdvh.getViewerPanel().getWidth();
        int h = bdvh.getViewerPanel().getHeight();
        g.setColor(new Color(ARGBType.red(colorCode) , ARGBType.green(colorCode), ARGBType.blue(colorCode), ARGBType.alpha(colorCode) ));
        /*g.drawLine((int)(w/2*Math.random()), h/2-h/4,w/2, h/2+h/4 );
        g.drawLine(w/2-w/4, h/2,w/2+w/4, h/2 );*/

        int nPixX = (int) slicingModel.getSpimSource().getSource(0,0).dimension(0);
        int nPixY = (int) slicingModel.getSpimSource().getSource(0,0).dimension(1);

        //AffineTransform3D at3D = new AffineTransform3D();

        //slicingModel.getSpimSource().getSourceTransform(0,0,at3D); // TODO fix

        double width = nPixX*0.01;//at3D.get(0,0);
        double height = nPixY*0.01;//at3D.get(1,1);

        RealPoint[][] ptRectWorld = new RealPoint[2][2];
        Point[][] ptRectScreen = new Point[2][2];

        AffineTransform3D bdvAt3D = new AffineTransform3D();

        bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);
        //for (int iSlice = 0;iSlice<100;iSlice++) {
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
        //}

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
            // System.out.println(pt3d);
            // Gets the location along the slicing axis...
            // First gets which z slice is targeted

            // Gets the width of the slicing model

            int nPixX = (int) slicingModel.getSpimSource().getSource(0,0).dimension(0);

            //AffineTransform3D at3D = new AffineTransform3D();

            //slicingModel.getSpimSource().getSourceTransform(0,0,at3D);

            double widthX = nPixX*0.01;//at3D.get(0,0); // TODO Fix this

            /*System.out.println(widthX);
            widthX = nPixX*at3D.get(2,0);
            System.out.println("or : "+widthX);*/

            iSliceNoStep = (int) (pt3d.getDoublePosition(0)/widthX);

            iSlice = iSliceNoStep*(int) zStepSetter.getStep();

            //System.out.println("iSlice = "+iSlice+"; iSliceNoStep = "+iSliceNoStep);

            //Repaint the overlay only
            bdvh.getViewerPanel().paint();

        }

        @Override
        public void importSourcesAndConverters(TransferSupport support, List<SourceAndConverter<?>> sacs) {
            // Can be extended for custom action on sources import
            // Do nothing (yet)
            int nPixX = (int) slicingModel.getSpimSource().getSource(0,0).dimension(0);
            int nPixY = (int) slicingModel.getSpimSource().getSource(0,0).dimension(1);

            //AffineTransform3D at3D = new AffineTransform3D();

            //slicingModel.getSpimSource().getSourceTransform(0,0,at3D);

            double width = nPixX*0.01;//at3D.get(0,0); // TODO Fix this
            double height = nPixY*0.01;
            List<SourceAndConverter<?>> sacsTransformed = new ArrayList<>();
            Optional<BdvHandle> bdvh = getBdvHandleFromViewerPanel(((bdv.viewer.ViewerPanel)support.getComponent()));
            if (bdvh.isPresent()) {

                for (SourceAndConverter sac : sacs) {
                    AffineTransform3D at3D = new AffineTransform3D();
                    sac.getSpimSource().getSourceTransform(0,0,at3D);

                    // We do not want to touch the scaling...
                    double[] m = at3D.getRowPackedCopy();

                    double sx = Math.sqrt(m[0]*m[0]+m[4]*m[4]+m[8]*m[8]);
                    double sy = Math.sqrt(m[1]*m[1]+m[5]*m[5]+m[9]*m[9]);
                    double sz = Math.sqrt(m[2]*m[2]+m[6]*m[6]+m[10]*m[10]);

                    m[0] /= sx;

                    m[1] /= sy;
                    m[5] /= sy;
                    m[9] /= sy;

                    m[2] /= sz;


                    m[6] /= sz;
                    m[10] /= sz;

                    m[3] -= iSliceNoStep*width;

                    m[7] -= height;

                    at3D.set(m);

                    sacsTransformed.add(new SourceAffineTransformer(sac, at3D.inverse().copy()).getSourceOut());
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
}