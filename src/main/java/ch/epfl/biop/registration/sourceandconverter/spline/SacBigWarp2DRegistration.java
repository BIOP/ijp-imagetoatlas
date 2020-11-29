package ch.epfl.biop.registration.sourceandconverter.spline;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import ij.gui.WaitForUserDialog;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.ThinplateSplineTransform;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import sc.fiji.bdvpg.bdv.BdvUtils;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.services.serializers.plugins.ThinPlateSplineTransformAdapter;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SacBigWarp2DRegistration implements Registration<SourceAndConverter<?>[]> {

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
    RealTransform rt;

    @Override
    public boolean register() {
        {

            List<SourceAndConverter> movingSacs = Arrays.stream(mimg).collect(Collectors.toList());

            List<SourceAndConverter> fixedSacs = Arrays.stream(fimg).collect(Collectors.toList());

            List<ConverterSetup> converterSetups = Arrays.stream(mimg).map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList());

            converterSetups.addAll(Arrays.stream(fimg).map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList()));

            // Launch BigWarp
            bwl = new BigWarpLauncher(movingSacs, fixedSacs, "Big Warp", converterSetups);
            bwl.set2d();
            bwl.run();

            // Output bdvh handles -> will be put in the object service
            BdvHandle bdvhQ = bwl.getBdvHandleQ();
            BdvHandle bdvhP = bwl.getBdvHandleP();

            bdvhP.getViewerPanel().state().setViewerTransform(BdvUtils.getViewerTransformWithNewCenter(bdvhP, new double[]{0,0,0}));
            bdvhQ.getViewerPanel().state().setViewerTransform(BdvUtils.getViewerTransformWithNewCenter(bdvhQ, new double[]{0,0,0}));


            SourceAndConverterServices.getSourceAndConverterDisplayService().pairClosing(bdvhQ,bdvhP);

            bdvhP.getViewerPanel().requestRepaint();
            bdvhQ.getViewerPanel().requestRepaint();

            bwl.getBigWarp().getLandmarkFrame().repaint();

            if (rt!=null) {
                bwl.getBigWarp().loadLandmarks(BigWarpFileFromRealTransform(rt));
                bwl.getBigWarp().setInLandmarkMode(true);
                bwl.getBigWarp().setIsMovingDisplayTransformed(true);
            }

            waitForUser.run();

            rt = bwl.getBigWarp().getTransformation();

            bwl.getBigWarp().closeAll();

            isDone = true;

            return true;

        }
    }

    public static String BigWarpFileFromRealTransform(RealTransform rt) {
        try {
            File file = File.createTempFile("temp", null);
            System.out.println(file.getAbsolutePath());
            file.deleteOnExit();

            if (rt instanceof Wrapped2DTransformAs3D) {
                rt = ((Wrapped2DTransformAs3D)rt).transform;
            }

            if (rt instanceof WrappedIterativeInvertibleRealTransform) {
                rt = ((WrappedIterativeInvertibleRealTransform)rt).getTransform();
            }

            if (!(rt instanceof ThinplateSplineTransform)) {
                System.err.println("Cannot edit the transform : it's not of class thinplatesplinetransform");
            }

            ThinplateSplineTransform tst = (ThinplateSplineTransform) rt;

            ThinPlateR2LogRSplineKernelTransform kernel = ThinPlateSplineTransformAdapter.getKernel(tst);

            double[][] srcPts = ThinPlateSplineTransformAdapter.getSrcPts(kernel);
            double[][] tgtPts = ThinPlateSplineTransformAdapter.getTgtPts(kernel);

            int nbLandmarks = kernel.getNumLandmarks();
            int nbDimensions = kernel.getNumDims();

            String toFile = "";

            for (int i = 0;i<nbLandmarks;i++) {
                toFile+="\"Pt-"+i+"\",\"true\"";
                for (int d = 0; d<nbDimensions; d++) {
                    toFile+=",\""+tgtPts[d][i]+"\"";
                }
                for (int d = 0; d<nbDimensions; d++) {
                    toFile+=",\""+srcPts[d][i]+"\"";
                }
                toFile+="\n";
            }
            //System.out.println(toFile);

            FileWriter writer = new FileWriter(file);
            writer.write(toFile);
            writer.flush();
            writer.close();

            //System.out.println(file.getAbsolutePath());

            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed( SourceAndConverter[] sacs) {

            SourceAffineTransformer satm = new SourceAffineTransformer(null, new AffineTransform3D().inverse().copy());
            SourceRealTransformer srt = new SourceRealTransformer(null, rt);//bwl.getBigWarp().getTransformation());// rts);
            SourceAffineTransformer satf = new SourceAffineTransformer(null, new AffineTransform3D());

            SourceAndConverter[] out = new SourceAndConverter[sacs.length];
            for (int i = 0; i<sacs.length; i++) {
                out[i] = satf.apply(srt.apply(satm.apply(sacs[i])));
            }
            return out;
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {
        // Transform 2D in 3D
        for
        (RealPoint pt : pts.ptList) {
            double[] tr = new double[3];
            rt.apply( new double[] {
                    pt.getDoublePosition(0), pt.getDoublePosition(1),0
            }, tr);
            pt.setPosition(tr);
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
        System.out.println("On y est! Dans l'edition de BigWarp");
        // Il faut relancer bigwarp... s'il a été lancé
        this.register();
        return true;
        //throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    private boolean isDone = false;

    public void setDone() {
        isDone = true;
    }

    public RealTransform getRealTransform() {
        return rt;
    }

    public void setRealTransform(RealTransform rt) {
        this.rt = rt;
    }

    @Override
    public boolean isRegistrationDone() {
        return isDone;
    }

    @Override
    public void resetRegistration() {
        isDone = false;
    }

    @Override
    public void setTimePoint(int timePoint) {
        // TODO
    }

}
