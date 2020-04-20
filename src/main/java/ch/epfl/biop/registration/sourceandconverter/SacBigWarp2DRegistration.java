package ch.epfl.biop.registration.sourceandconverter;

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
import net.imglib2.util.LinAlgHelpers;
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

        double sx = Math.sqrt(m[0]*m[0]+m[1]*m[1]+m[2]*m[2]);
        double sy = Math.sqrt(m[4]*m[4]+m[5]*m[5]+m[6]*m[6]);
        double sz = Math.sqrt(m[8]*m[8]+m[9]*m[9]+m[10]*m[10]);

        m[0] /= sx;
        m[1] /= sx;
        m[2] /= sx;

        m[4] /= sy;
        m[5] /= sy;
        m[6] /= sy;

        m[8] /= sz;
        m[9] /= sz;
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

        double sx = Math.sqrt(m[0]*m[0]+m[1]*m[1]+m[2]*m[2]);
        double sy = Math.sqrt(m[4]*m[4]+m[5]*m[5]+m[6]*m[6]);
        double sz = Math.sqrt(m[8]*m[8]+m[9]*m[9]+m[10]*m[10]);

        m[0] /= sx;
        m[1] /= sx;
        m[2] /= sx;

        m[4] /= sy;
        m[5] /= sy;
        m[6] /= sy;

        m[8] /= sz;
        m[9] /= sz;
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
        // Pb : 2D vs 3D

        //ThinplateSplineTransform tr2D = new ThinplateSplineTransform(bwl.getBigWarp().getTransform());

        double[][] ptI = new double[3][bwl.getBigWarp().getTransform().getNumLandmarks()];
        double[][] ptF = new double[3][bwl.getBigWarp().getTransform().getNumLandmarks()];

        for (int i = 0;i<bwl.getBigWarp().getTransform().getNumLandmarks();i++) {
            ptF[0][i] = bwl.getBigWarp().getLandmarkPanel().getTableModel().getFixedPoint(i)[0];
            ptF[1][i] = bwl.getBigWarp().getLandmarkPanel().getTableModel().getFixedPoint(i)[1];
            ptF[2][i] = 0;

            ptI[0][i] = bwl.getBigWarp().getLandmarkPanel().getTableModel().getMovingPoint(i)[0];
            ptI[1][i] = bwl.getBigWarp().getLandmarkPanel().getTableModel().getMovingPoint(i)[1];
            ptI[2][i] = 0;
        }

        ThinplateSplineTransform tr3D = new ThinplateSplineTransform(ptF,ptI);
        RealTransformSequence rts = new RealTransformSequence();

        // Looks ok
        //rts.add(at3Dfixed.inverse().copy());
        rts.add(tr3D);
        rts.add(at3Dmoving.copy());

        SourceRealTransformer srt = new SourceRealTransformer(null, rts);
        SourceAffineTransformer sat = new SourceAffineTransformer(null, at3Dfixed.copy());

        // Yes but mipmap is screwed up

        // MipmapOrdering.MipmapHints
        // https://github.com/bigdataviewer/bigdataviewer-core/blob/master/src/main/java/bdv/util/MipmapTransforms.java


        return (sacs) -> {
            SourceAndConverter[] out = new SourceAndConverter[sacs.length];
            for (int i = 0; i<sacs.length; i++) {
                out[i] = sat.apply(srt.apply(sacs[i]));
            }
            return out;
        };

    }

    @Override
    public RealPointList getPtsRegistration(RealPointList pts) {
        // Pb : 2D vs 3D
        for (RealPoint pt : pts.ptList) {
            double[] tr = bwl.getBigWarp().getTransform().apply( new double[] {
                pt.getDoublePosition(0), pt.getDoublePosition(1)
            });
            pt.setPosition(tr);
        }
        return pts;
    }

}
