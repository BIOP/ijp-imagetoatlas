package ch.epfl.biop.registration.sourceandconverter.spline;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixSplineCommand;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixSplineRemoteCommand;
import ch.epfl.biop.atlas.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.plugin.RegistrationTypeProperties;
import ch.epfl.biop.bdv.command.register.Elastix2DAffineRegisterServerCommand;
import ch.epfl.biop.bdv.command.register.Elastix2DSplineRegisterCommand;
import ch.epfl.biop.bdv.command.register.Elastix2DSplineRegisterServerCommand;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ij.gui.WaitForUserDialog;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.RealTransform;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static bdv.util.RealTransformHelper.BigWarpFileFromRealTransform;

@Plugin(type = IABBARegistrationPlugin.class)
@RegistrationTypeProperties(
        isManual = false,
        isEditable = true,
        userInterface = {
                RegistrationElastixSplineCommand.class,
                RegistrationElastixSplineRemoteCommand.class
        }
)
public class Elastix2DSplineRegistration extends RealTransformSourceAndConverterRegistration {

    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        super.setFixedImage(fimg);
        assert fimg.length==1;
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        super.setMovingImage(mimg);
        assert mimg.length==1;
    }

    Future<CommandModule> task;

    @Override
    public boolean register() {
        try {
            boolean success = true;
            Class<? extends Command> registrationCommandClass;
            // Is it supposed to be done on a server ?
            if (parameters.containsKey("serverURL")) {
                // Yes -> changes command class name
                registrationCommandClass = Elastix2DSplineRegisterServerCommand.class;
            } else {
                registrationCommandClass = Elastix2DSplineRegisterCommand.class;
            }

            // Start registration with a 4x4 pixel iamge
            parameters.put("minPixSize","32");
            // 150 iteration steps
            parameters.put("maxIterationNumberPerScale","150");
            // Do not write anything
            parameters.put("verbose", "false");
            // Centers centers of mass of both images before starting registration
            parameters.put("automaticTransformInitialization", "false");
            // Atlas image : a single timepoint
            parameters.put("tpFixed", "0");
            // Level 2 for the atlas
            parameters.put("levelFixedSource", "1");
            // Timepoint moving source (normally 0)
            parameters.put("tpMoving", Integer.toString(timePoint));
            // Tries to be clever for the moving source sampling
            parameters.put("levelMovingSource", Integer.toString(SourceAndConverterHelper.bestLevel(fimg[0], timePoint, 0.02)));
            // 20 microns per pixel for the initial registration
            parameters.put("pxSizeInCurrentUnit", "0.02");
            // Interpolation in resampling
            parameters.put("interpolate", "true");

            // Transforms map into flat String : key1, value1, key2, value2, etc.
            // Necessary for CommandService
            List<Object> flatParameters = new ArrayList<>(parameters.size()*2+4);

            parameters.keySet().forEach(k -> {
                flatParameters.add(k);
                flatParameters.add(parameters.get(k));
            });

            flatParameters.add("sac_fixed");
            flatParameters.add(fimg[0]);

            flatParameters.add("sac_moving");
            flatParameters.add(mimg[0]);


             task = context
                   .getService(CommandService.class)
                   .run(registrationCommandClass, true,
                           flatParameters.toArray(new Object[0]));

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
    public void abort() {
        if (task!=null) {
           task.cancel(true);
        }
    }

    @Override
    public void setLogger(Consumer<String> logger) {

    }
}
