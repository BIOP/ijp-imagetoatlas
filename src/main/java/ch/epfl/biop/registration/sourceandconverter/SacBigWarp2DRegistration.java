package ch.epfl.biop.registration.sourceandconverter;

import bdv.gui.TransformTypeSelectDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import ij.gui.WaitForUserDialog;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SacBigWarp2DRegistration implements Registration<SourceAndConverter[]> {

    SourceAndConverter[] fimg, mimg;

    Runnable waitForUser = () -> {
        WaitForUserDialog dialog = new WaitForUserDialog("Choose slice","Please perform carefully your registration then press ok.");
        dialog.show();
    };

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        this.fimg = fimg;
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        this.mimg = mimg;
    }

    public void setWaitForUserMethod(Runnable r) {
        waitForUser = r;
    }

    BigWarpLauncher bwl;
    InvertibleRealTransformSequence irts;

    @Override
    public boolean register() {
        {
            List<SourceAndConverter> movingSacs = Arrays.stream(mimg).collect(Collectors.toList());
            List<SourceAndConverter> fixedSacs = Arrays.stream(fimg).collect(Collectors.toList());

            List<ConverterSetup> converterSetups = Arrays.stream(mimg).map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList());
            converterSetups.addAll(Arrays.stream(fimg).map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList()));

            // Launch BigWarp
            bwl = new BigWarpLauncher(movingSacs, fixedSacs, "Big Warp", converterSetups);

            bwl.run();

            // Output bdvh handles -> will be put in the object service
            BdvHandle bdvhQ = bwl.getBdvHandleQ();
            BdvHandle bdvhP = bwl.getBdvHandleP();

            SourceAndConverterServices.getSourceAndConverterDisplayService().pairClosing(bdvhQ,bdvhP);

            bdvhP.getViewerPanel().requestRepaint();
            bdvhQ.getViewerPanel().requestRepaint();

            bwl.getBigWarp().getLandmarkFrame().repaint();

            waitForUser.run();

            bwl.getBigWarp().closeAll();

            isDone = true;

            return true;

        }
    }

    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed( SourceAndConverter[] sacs) {

        if (bwl.getBigWarp().getTransformType().equals(TransformTypeSelectDialog.TPS)) {
            // Thin Plate Spline
            SourceAffineTransformer satm = new SourceAffineTransformer(null, new AffineTransform3D().inverse().copy());
            SourceRealTransformer srt = new SourceRealTransformer(null, bwl.getBigWarp().getTransformation());// rts);
            SourceAffineTransformer satf = new SourceAffineTransformer(null, new AffineTransform3D());
            irts = new InvertibleRealTransformSequence();
            //irts.add(at3Dmoving.inverse());
            irts.add(bwl.getBigWarp().getTransformation());
            //irts.add(at3Dfixed);

            SourceAndConverter[] out = new SourceAndConverter[sacs.length];
            for (int i = 0; i<sacs.length; i++) {
                out[i] = satf.apply(srt.apply(satm.apply(sacs[i])));
            }
            return out;
        } else {
            // Just an affine transform
            SourceAffineTransformer satm = new SourceAffineTransformer(null, new AffineTransform3D().copy());
            SourceAffineTransformer srt = new SourceAffineTransformer(null, bwl.getBigWarp().affine3d().inverse());// rts);
            SourceAffineTransformer satf = new SourceAffineTransformer(null, new AffineTransform3D().copy());

            irts = new InvertibleRealTransformSequence();
            //irts.add(at3Dmoving.inverse());
            irts.add(bwl.getBigWarp().affine3d().inverse());
            //irts.add(at3Dfixed);

            SourceAndConverter[] out = new SourceAndConverter[sacs.length];
            for (int i = 0; i<sacs.length; i++) {
                out[i] = satf.apply(srt.apply(satm.apply(sacs[i])));
            }
            return out;
        }
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {
        // Transform 2D in 3D
        for
        (RealPoint pt : pts.ptList) {
            double[] tr = bwl.getBigWarp().getTransform().apply( new double[] {
                    pt.getDoublePosition(0), pt.getDoublePosition(1)
            });
            pt.setPosition(tr);

/*            bwl.getBigWarp().getTransform().apply()
            RealPoint pi = new RealPoint(3);
            pi.setPosition(pt.getDoublePosition(0),0);
            pi.setPosition(pt.getDoublePosition(1),1);
            pi.setPosition(0,2);

            RealPoint pf0 = new RealPoint(3);

            new AffineTransform3D().inverse().apply(pi,pf0);

            RealPoint pf1 = new RealPoint(3);
            bwl.getBigWarp().unwrap2d(bwl.getBigWarp().getTransformation()).inverse().apply(pf0,pf1);
            //bwl.getBigWarp().getTransformation().inverse().apply(pf0,pf1);

            RealPoint pf2 = new RealPoint(3);
            new AffineTransform3D().apply(pf1,pf2);

            //irts.apply(pi, pf);

            pt.setPosition(pf2.getDoublePosition(0),0);
            pt.setPosition(pf2.getDoublePosition(1),1);
            if (Math.random()>0.99) {
                System.out.println(pf2.getDoublePosition(0)+":"+pf2.getDoublePosition(1));
            }*/
        }
        return pts;
    }

    @Override
    public boolean parallelSupported() {
        return false;
    }

    @Override
    public boolean isManual() {
        return true;
    }

    @Override
    public boolean edit() {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    private boolean isDone = false;

    @Override
    public boolean isRegistrationDone() {
        return isDone;
    }

    @Override
    public void resetRegistration() {
        isDone = false;
    }

}
