package ch.epfl.biop.registration.sourceandconverter;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import bigwarp.BigWarp;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import ij.gui.WaitForUserDialog;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SacBigWarp2DRegistration implements Registration<SourceAndConverter[]> {

    SourceAndConverter[] fimg, mimg;

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        AffineTransform3D at3D = new AffineTransform3D();
        fimg[0].getSpimSource().getSourceTransform(0,0, at3D);
        SourceAffineTransformer sat =
                new SourceAffineTransformer(null, at3D.inverse());

        this.fimg = new SourceAndConverter[fimg.length];

        for (int i=0;i<fimg.length;i++) {
            this.fimg[i] = sat.apply(fimg[i]);
        }
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        AffineTransform3D at3D = new AffineTransform3D();
        mimg[0].getSpimSource().getSourceTransform(0,0, at3D);
        SourceAffineTransformer sat =
                new SourceAffineTransformer(null, at3D.inverse());

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
        // See https://github.com/saalfeldlab/bigwarp/blob/e490dd2ce87c6bcf3355e01e562586421f978303/scripts/Apply_Bigwarp_Xfm.groovy
        /*return ((img) ->
            ImagePlusFunctions.splitApplyRecompose(
                    imp -> ApplyBigwarpPlugin.apply(
                                imp, fimg, bw.getLandmarkPanel().getTableModel(),
                                "Target", "", "Target",
                                null, null, null,
                                Interpolation.NEARESTNEIGHBOR, false, 1 ),
                    img));*/

        return null;

    }

    @Override
    public RealPointList getPtsRegistration(RealPointList pts) {
        for (RealPoint pt : pts.ptList) {
            double[] tr = bwl.getBigWarp().getTransform().apply( new double[] {
                pt.getDoublePosition(0), pt.getDoublePosition(1)
            });
            pt.setPosition(tr);
        }
        return pts;
    }

}
