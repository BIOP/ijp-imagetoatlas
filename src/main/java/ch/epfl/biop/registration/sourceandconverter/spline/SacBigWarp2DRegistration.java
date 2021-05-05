package ch.epfl.biop.registration.sourceandconverter.spline;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.DisplayMode;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.RegistrationBigWarpCommand;
import ch.epfl.biop.atlas.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.plugin.RegistrationTypeProperties;
import ij.gui.WaitForUserDialog;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static bdv.util.RealTransformHelper.BigWarpFileFromRealTransform;

@Plugin(type = IABBARegistrationPlugin.class)
@RegistrationTypeProperties(
        isManual = true,
        isEditable = true,
        userInterface = {
                RegistrationBigWarpCommand.class
        }
)

public class SacBigWarp2DRegistration extends RealTransformSourceAndConverterRegistration {

    BigWarpLauncher bwl;

    Runnable waitForUser = () -> {
        WaitForUserDialog dialog = new WaitForUserDialog("Big Warp registration","Please perform carefully your registration then press ok.");
        dialog.show();
    };

    public void setWaitForUserMethod(Runnable r) {
        waitForUser = r;
    }

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

            bdvhQ.getViewerPanel().state().setDisplayMode(DisplayMode.FUSED);
            bdvhP.getViewerPanel().state().setDisplayMode(DisplayMode.FUSED);

            bdvhP.getViewerPanel().state().setViewerTransform(BdvHandleHelper.getViewerTransformWithNewCenter(bdvhP, new double[]{0,0,0}));
            bdvhQ.getViewerPanel().state().setViewerTransform(BdvHandleHelper.getViewerTransformWithNewCenter(bdvhQ, new double[]{0,0,0}));

            SourceAndConverterServices.getSourceAndConverterDisplayService().pairClosing(bdvhQ,bdvhP);

            bdvhP.getViewerPanel().requestRepaint();
            bdvhQ.getViewerPanel().requestRepaint();

            bwl.getBigWarp().getLandmarkFrame().repaint();

            // Restores landmarks if some were already defined
            if (rt!=null) {
                bwl.getBigWarp().loadLandmarks(BigWarpFileFromRealTransform(rt));
                bwl.getBigWarp().setInLandmarkMode(true);
                bwl.getBigWarp().setIsMovingDisplayTransformed(true);
            }

            waitForUser.run();

            rt = bwl.getBigWarp().getBwTransform().getTransformation();

            bwl.getBigWarp().closeAll();

            isDone = true;

            return true;

        }
    }

    @Override
    public boolean edit() {
        // Just launching again BigWarp
        this.register();
        return true;
    }

    @Override
    public void abort() {

    }

}
