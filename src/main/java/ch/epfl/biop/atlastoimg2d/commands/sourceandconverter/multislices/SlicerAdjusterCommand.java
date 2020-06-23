package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.Set;

@Plugin(type = InteractiveCommand.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Adjust Slicing")
public class SlicerAdjusterCommand extends InteractiveCommand {

    @Parameter(min = "1", max = "50", stepSize = "1", style = "slider")
    int zSamplingSteps = 1;

    int previouszSampingStep=-1;

    @Parameter(min = "-20", max = "+20", stepSize = "1", style = "slider")
    int rotateX = 0;

    @Parameter(min = "-20", max = "+20", stepSize = "1", style = "slider")
    int rotateY = 0;

    @Parameter
    SourceAndConverter modelSlicing;

    @Parameter
    AffineTransform3D originalAffineTransform3D;

    @Parameter
    SourceAndConverter[] slicedSources;

    @Parameter
    MultiSlicePositioner.SlicerSetter sliceSetter;

    public void run() {

        sliceSetter.setStep(zSamplingSteps);
        sliceSetter.setRotateX(rotateX/180.0*Math.PI);
        sliceSetter.setRotateY(rotateY/180.0*Math.PI);


        long nPixX = modelSlicing.getSpimSource().getSource(0,0).max(0);
        long nPixY = modelSlicing.getSpimSource().getSource(0,0).max(1);
        long nPixZ = modelSlicing.getSpimSource().getSource(0,0).max(2);

        AffineTransform3D slicingTransfom = originalAffineTransform3D.copy();

        // Removes shift XYZ
        slicingTransfom.set(0,0,3);
        slicingTransfom.set(0,1,3);
        slicingTransfom.set(0,2,3);

        // Store Z Axis
        double m20 = slicingTransfom.get(0,2);
        double m21 = slicingTransfom.get(1,2);
        double m22 = slicingTransfom.get(2,2);

        slicingTransfom.rotate(0,rotateX/180.0*Math.PI);
        slicingTransfom.rotate(1,rotateY/180.0*Math.PI);

        // ReStore ZAxis
        slicingTransfom.set(m20, 0,2 );
        slicingTransfom.set(m21, 1,2 );
        slicingTransfom.set(m22, 2,2 );

        SacMultiSacsPositionerCommand.adjustShiftSlicingTransform(slicingTransfom, nPixX, nPixY, nPixZ);

        ((TransformedSource) (modelSlicing.getSpimSource())).setFixedTransform(slicingTransfom);

        // Get BdvHandle where modelSlicing is shown
        Set<BdvHandle> bdvhs = SourceAndConverterServices.getSourceAndConverterDisplayService().getDisplaysOf(slicedSources);

        if (previouszSampingStep!=-1) {
            if (previouszSampingStep!=zSamplingSteps) {
                // sampling has been moved : need to update the position of looking in the BdvHandle
                // For each bdvhandle
                for (BdvHandle bdv : bdvhs) {
                    // Where were we ?

                    double cur_wcx = bdv.getViewerPanel().getWidth()/2.0; // Current Window Center X
                    double cur_wcy = bdv.getViewerPanel().getHeight()/2.0; // Current Window Center Y

                    RealPoint centerScreenCurrentBdv = new RealPoint(new double[]{cur_wcx, cur_wcy, 0});
                    RealPoint centerScreenGlobalCoord = new RealPoint(3);

                    AffineTransform3D at3D = new AffineTransform3D();
                    bdv.getBdvHandle().getViewerPanel().state().getViewerTransform(at3D);

                    at3D.inverse().apply(centerScreenCurrentBdv, centerScreenGlobalCoord);

                    // New target
                    centerScreenGlobalCoord.setPosition( centerScreenGlobalCoord.getDoublePosition(0)*(double)previouszSampingStep/(double) zSamplingSteps, 0);

                    // How should we translate at3D, such as the screen center is the new one

                    // Now compute what should be the matrix in the next bdv frame:
                    AffineTransform3D nextAffineTransform = new AffineTransform3D();

                    // It should have the same scaling and rotation than the current view
                    nextAffineTransform.set(at3D);

                    // No Shift
                    nextAffineTransform.set(0,0,3);
                    nextAffineTransform.set(0,1,3);
                    nextAffineTransform.set(0,2,3);

                    // But the center of the window should be centerScreenGlobalCoord
                    // Let's compute the shift
                    double next_wcx = bdv.getViewerPanel().getWidth()/2.0; // Next Window Center X
                    double next_wcy = bdv.getViewerPanel().getHeight()/2.0; // Next Window Center Y

                    RealPoint centerScreenNextBdv = new RealPoint(new double[]{next_wcx, next_wcy, 0});
                    RealPoint shiftNextBdv = new RealPoint(3);

                    nextAffineTransform.inverse().apply(centerScreenNextBdv, shiftNextBdv);
                    //System.out.println( "shiftNextBdv:"+shiftNextBdv);
                    double sx = -centerScreenGlobalCoord.getDoublePosition(0)+shiftNextBdv.getDoublePosition(0);
                    double sy = -centerScreenGlobalCoord.getDoublePosition(1)+shiftNextBdv.getDoublePosition(1);
                    double sz = -centerScreenGlobalCoord.getDoublePosition(2)+shiftNextBdv.getDoublePosition(2);

                    RealPoint shiftWindow = new RealPoint(new double[]{sx, sy, sz});
                    RealPoint shiftMatrix = new RealPoint(3);
                    nextAffineTransform.apply(shiftWindow, shiftMatrix);

                    //System.out.println("shiftMatrix:"+shiftMatrix);

                    nextAffineTransform.set(shiftMatrix.getDoublePosition(0),0,3);
                    nextAffineTransform.set(shiftMatrix.getDoublePosition(1),1,3);
                    nextAffineTransform.set(shiftMatrix.getDoublePosition(2),2,3);

                    bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform(nextAffineTransform);

                }
            }
        }

        SourceAndConverterServices.getSourceAndConverterDisplayService().getDisplaysOf(modelSlicing)
                .forEach(bdv -> bdv.getViewerPanel().requestRepaint());

        bdvhs.forEach(bdv -> bdv.getViewerPanel().requestRepaint());

        previouszSampingStep = zSamplingSteps;
    }

    public int getZSamplingStep() {
        return zSamplingSteps;
    }

}
