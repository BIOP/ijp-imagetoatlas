package ch.epfl.biop.registration.sourceandconverter.spline;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.command.register.Elastix2DSplineRegisterCommand;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.sourceandconverter.SourceAndConverterRegistration;
import ij.gui.WaitForUserDialog;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.RealTransform;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static bdv.util.RealTransformHelper.BigWarpFileFromRealTransform;

public class Elastix2DSplineRegistration extends SourceAndConverterRegistration {

    Context ctx;

    Map<String, Object> parameters = new HashMap<>();

    RealTransform rt;

    public void setScijavaContext(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    /*public void setScijavaParameters(Map<String, Object> scijavaParameters) {
        this.scijavaParameters.putAll(scijavaParameters);
    }*/

    public RealTransform getRealTransform() {
        return rt;
    }

    public void setRealTransform(RealTransform rt) {
        this.rt = rt;
    }

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        super.setFixedImage(fimg);
        assert fimg.length==1;
        parameters.put("sac_fixed", fimg[0]);
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        super.setMovingImage(mimg);
        assert mimg.length==1;
        parameters.put("sac_moving", mimg[0]);
    }

    Class<? extends Command> registrationCommandClass = Elastix2DSplineRegisterCommand.class;

    public void setRegistrationCommand(Class<? extends Command> registrationCommandClass) {
        this.registrationCommandClass = registrationCommandClass;
    }

    Future<CommandModule> task = null;
    @Override
    public boolean register() {
        try {
            boolean success = true;
             task = ctx
                   .getService(CommandService.class)
                   .run(registrationCommandClass, false, parameters);

            CommandModule module = task.get();

            if (module.getOutputs().keySet().contains("success")) {
                success = (boolean) module.getOutput("success");
            }
            if (success) {
                rt = (RealTransform) module.getOutput("rt");
            }

            isDone = true;
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    Supplier<Double> zPosition;

    public void setZPositioner(Supplier<Double> zPosition) {
        this.zPosition = zPosition;
    }

    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed(SourceAndConverter[] img) {
        SourceAndConverter[] out = new SourceAndConverter[img.length];
        SourceRealTransformer srt = new SourceRealTransformer(null, rt);

        for (int idx = 0;idx<img.length;idx++) {
            out[idx] = srt.apply(img[idx]);
        }

        return out;
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {

        ArrayList<RealPoint> cvtList = new ArrayList<>();

        for (RealPoint p : pts.ptList) {
            RealPoint pt3d = new RealPoint(3);
            pt3d.setPosition(new double[]{p.getDoublePosition(0), p.getDoublePosition(1),0});
            if (zPosition!=null) {
                pt3d.setPosition(zPosition.get(), 2);
            }
            rt.apply(pt3d, pt3d);
            RealPoint cpt = new RealPoint(pt3d.getDoublePosition(0), pt3d.getDoublePosition(1));
            cvtList.add(cpt);
        }
        return new RealPointList(cvtList);
    }

    @Override
    public boolean parallelSupported() {
        return true;
    }

    @Override
    public boolean isManual() {
        return false;
    }

    Runnable waitForUser = () -> {
        WaitForUserDialog dialog = new WaitForUserDialog("Choose slice","Please perform carefully your registration then press ok.");
        dialog.show();
    };

    @Override
    public boolean edit() {

        List<SourceAndConverter> movingSacs = Arrays.stream(mimg).collect(Collectors.toList());

        List<SourceAndConverter> fixedSacs = Arrays.stream(fimg).collect(Collectors.toList());

        List<ConverterSetup> converterSetups = Arrays.stream(mimg).map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList());

        converterSetups.addAll(Arrays.stream(fimg).map(src -> SourceAndConverterServices.getSourceAndConverterDisplayService().getConverterSetup(src)).collect(Collectors.toList()));

        // Launch BigWarp
        BigWarpLauncher bwl = new BigWarpLauncher(movingSacs, fixedSacs, "Big Warp", converterSetups);
        bwl.set2d();
        bwl.run();

        // Output bdvh handles -> will be put in the object service
        BdvHandle bdvhQ = bwl.getBdvHandleQ();
        BdvHandle bdvhP = bwl.getBdvHandleP();

        bdvhP.getViewerPanel().state().setViewerTransform(BdvHandleHelper.getViewerTransformWithNewCenter(bdvhP, new double[]{0,0,0}));
        bdvhQ.getViewerPanel().state().setViewerTransform(BdvHandleHelper.getViewerTransformWithNewCenter(bdvhQ, new double[]{0,0,0}));

        SourceAndConverterServices.getSourceAndConverterDisplayService().pairClosing(bdvhQ,bdvhP);

        bdvhP.getViewerPanel().requestRepaint();
        bdvhQ.getViewerPanel().requestRepaint();

        bwl.getBigWarp().getLandmarkFrame().repaint();

        if (rt!=null) {
            bwl.getBigWarp().loadLandmarks(BigWarpFileFromRealTransform(rt));
            //bwl.getBigWarp().setInLandmarkMode(true);
            bwl.getBigWarp().setIsMovingDisplayTransformed(true);
        }

        waitForUser.run();

        rt = bwl.getBigWarp().getTransformation();

        bwl.getBigWarp().closeAll();

        isDone = true;

        return true;

    }

    @Override
    public boolean isEditable() {
        return true;
    }

    private boolean isDone = false;

    @Override
    public boolean isRegistrationDone() {
        return isDone;
    }

    public void setDone() {
        isDone = true;
    }

    @Override
    public void resetRegistration() {
        isDone = false;
    }

    @Override
    public void setTimePoint(int timePoint) {
        // TODO
    }

    @Override
    public void abort() {
        if (task!=null) {
           task.cancel(true);
        }
    }

}
