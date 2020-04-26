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
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.ThinplateSplineTransform;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SacBigWarp2DRegistration implements Registration<SourceAndConverter[]> {

    SourceAndConverter[] fimg, mimg;

    AffineTransform3D at3Dmoving, at3Dfixed;

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        at3Dfixed = new AffineTransform3D();

        fimg[0].getSpimSource().getSourceTransform(0,0, at3Dfixed);

        // We do not want to touch the scaling...
        double[] m = at3Dfixed.getRowPackedCopy();

        double sx = Math.sqrt(m[0]*m[0]+m[4]*m[4]+m[8]*m[8]);
        double sy = Math.sqrt(m[1]*m[1]+m[5]*m[5]+m[9]*m[9]);
        double sz = Math.sqrt(m[2]*m[2]+m[6]*m[6]+m[10]*m[10]);

        m[0] /= sx;
        m[4] /= sx;
        m[8] /= sx;

        m[1] /= sy;
        m[5] /= sy;
        m[9] /= sy;

        m[2] /= sz;
        m[6] /= sz;
        m[10] /= sz;

        at3Dfixed.set(m);

        SourceAffineTransformer sat =
                new SourceAffineTransformer(null, at3Dfixed.inverse().copy());

        this.fimg = new SourceAndConverter[fimg.length];

        for (int i=0;i<fimg.length;i++) {
            this.fimg[i] = sat.apply(fimg[i]);
        }
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        at3Dmoving = new AffineTransform3D();
        if (mimg[0] == null) {
            System.err.println("No moving source defined (index 0)");
            return;
        }
        mimg[0]
                .getSpimSource()
                .getSourceTransform(0,0, at3Dmoving);

        // We do not want to touch the scaling...
        double[] m = at3Dmoving.getRowPackedCopy();

        double sx = Math.sqrt(m[0]*m[0]+m[4]*m[4]+m[8]*m[8]);
        double sy = Math.sqrt(m[1]*m[1]+m[5]*m[5]+m[9]*m[9]);
        double sz = Math.sqrt(m[2]*m[2]+m[6]*m[6]+m[10]*m[10]);

        m[0] /= sx;
        m[4] /= sx;
        m[8] /= sx;

        m[1] /= sy;
        m[5] /= sy;
        m[9] /= sy;

        m[2] /= sz;
        m[6] /= sz;
        m[10] /= sz;

        at3Dmoving.set(m);

        SourceAffineTransformer sat =
                new SourceAffineTransformer(null, at3Dmoving.inverse().copy());

        this.mimg = new SourceAndConverter[mimg.length];

        for (int i=0;i<mimg.length;i++) {
            this.mimg[i] = sat.apply(mimg[i]);
        }
    }

    BigWarpLauncher bwl;

    @Override
    public void register() {
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

            bdvhP.getViewerPanel().state().setViewerTransform(at3Dfixed.copy());
            bdvhP.getViewerPanel().state().setViewerTransform(at3Dmoving.copy());

            bdvhP.getViewerPanel().requestRepaint();
            bdvhQ.getViewerPanel().requestRepaint();

            bwl.getBigWarp().getLandmarkFrame().repaint();

            WaitForUserDialog dialog = new WaitForUserDialog("Choose slice","Please perform carefully your registration then press ok.");
            dialog.show();

            bwl.getBigWarp().getViewerFrameP().setVisible(false);
            bwl.getBigWarp().getViewerFrameQ().setVisible(false);
            bwl.getBigWarp().getLandmarkFrame().setVisible(false);

        }
    }

    @Override
    public Function<SourceAndConverter[], SourceAndConverter[]> getImageRegistration() {

        if (bwl.getBigWarp().getTransformType().equals(TransformTypeSelectDialog.TPS)) {
            // Thin Plate Spline
            SourceAffineTransformer satm = new SourceAffineTransformer(null, at3Dmoving.inverse().copy());
            SourceRealTransformer srt = new SourceRealTransformer(null, bwl.getBigWarp().getTransformation());// rts);
            SourceAffineTransformer satf = new SourceAffineTransformer(null, at3Dfixed.copy());

            return (sacs) -> {
                SourceAndConverter[] out = new SourceAndConverter[sacs.length];
                for (int i = 0; i<sacs.length; i++) {
                    out[i] = satf.apply(srt.apply(satm.apply(sacs[i])));
                }
                return out;
            };
        } else {
            // TODO Just an affine transform : Let's make this quicker to compute
            // see https://github.com/saalfeldlab/bigwarp/blob/master/scripts/Bigwarp_affinePart.groovy
            SourceAffineTransformer satm = new SourceAffineTransformer(null, at3Dmoving.inverse().copy());
            SourceAffineTransformer srt = new SourceAffineTransformer(null, bwl.getBigWarp().affine3d().inverse());// rts);
            SourceAffineTransformer satf = new SourceAffineTransformer(null, at3Dfixed.copy());

            return (sacs) -> {
                SourceAndConverter[] out = new SourceAndConverter[sacs.length];
                for (int i = 0; i<sacs.length; i++) {
                    out[i] = satf.apply(srt.apply(satm.apply(sacs[i])));
                }
                return out;
            };

        }

    }

    @Override
    public RealPointList getPtsRegistration(RealPointList pts) {
        // Pb : 2D vs 3D
        for
        (RealPoint pt : pts.ptList) {
            //double ptx = pt.getDoublePosition(0);
            //double pty = pt.getDoublePosition(1);
            //double ptz = pt.getDoublePosition(2);

            RealPoint p0 = new RealPoint(3);
            p0.setPosition(pt.getDoublePosition(0),0);
            p0.setPosition(pt.getDoublePosition(1),1);

            RealPoint p1 = new RealPoint(3);
            at3Dmoving.apply(p0, p1);

            double[] pxy2 = bwl.getBigWarp().getTransform().apply( new double[] {
                p1.getDoublePosition(0), p1.getDoublePosition(1)
            });

            RealPoint p2 = new RealPoint(3);
            p2.setPosition(pxy2[0], 0);
            p2.setPosition(pxy2[1], 1);
            p2.setPosition(p1.getDoublePosition(2), 2);

            RealPoint p3 = new RealPoint(3);

            at3Dfixed.apply(p2, p3);

            pt.setPosition(p3.getDoublePosition(0),0);
            pt.setPosition(p3.getDoublePosition(1),1);
        }
        return pts;
    }

}
