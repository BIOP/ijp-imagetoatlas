package ch.epfl.biop.registration.sourceandconverter.spline;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixSplineCommand;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixSplineRemoteCommand;
import ch.epfl.biop.atlas.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.plugin.RegistrationTypeProperties;
import ch.epfl.biop.bdv.command.register.Elastix2DSplineRegisterCommand;
import ch.epfl.biop.bdv.command.register.Elastix2DSplineRegisterServerCommand;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import com.google.gson.Gson;
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

    Future<CommandModule> task;

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        if (fimg.length==0) {
            System.err.println("Error, no fixed image set in class "+this.getClass().getSimpleName());
        }
        if (fimg.length>1) {
            log.accept("Multichannel image registration not supported for class "+this.getClass().getSimpleName());
        }
        super.setFixedImage(fimg);
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        if (mimg.length==0) {
            System.err.println("Error, no fixed image set in class "+this.getClass().getSimpleName());
        }
        if (mimg.length>1) {
            log.accept("Multichannel image registration not supported for class "+this.getClass().getSimpleName());
        }
        super.setMovingImage(mimg);
    }

    @Override
    public boolean register() {
        try {
            boolean success = true;
            Class<? extends Command> registrationCommandClass;
            // Is it supposed to be done on a server ?
            if (parameters.containsKey("serverURL")) {
                // Yes -> changes command class name
                registrationCommandClass = Elastix2DSplineRegisterServerCommand.class;
                if (parameters.get("userConsentForServerKeepingData").equals("true")) {
                    String taskInfo = new Gson().toJson(sliceInfo);
                    parameters.put("taskInfo", taskInfo);
                }
                parameters.remove("userConsentForServerKeepingData");
            } else {
                registrationCommandClass = Elastix2DSplineRegisterCommand.class;
            }

            // Transforms map into flat String : key1, value1, key2, value2, etc.
            // Necessary for CommandService
            List<Object> flatParameters = new ArrayList<>(parameters.size()*2+4);

            parameters.keySet().forEach(k -> {
                flatParameters.add(k);
                flatParameters.add(parameters.get(k));
            });

            addToFlatParameters(flatParameters,
                    // Fixed image
                    "sac_fixed",fimg[0],
                    // Moving image
                    "sac_moving", mimg[0],
                    // Interpolation in resampling
                    "interpolate", true,
                    // Start registration with a 32x32 pixel image
                    "minPixSize", 32,
                    // 100 iteration steps
                    "maxIterationNumberPerScale",100,
                    // Do not write anything
                    "verbose", false,
                    // Do not center centers of mass of both images before starting registration
                    "automaticTransformInitialization", false,
                    // Atlas image : a single timepoint
                    "tpFixed", 0,
                    // Level 2 for the atlas
                    "levelFixedSource", 1,
                    // Timepoint moving source (normally 0)
                    "tpMoving", timePoint,
                    // Tries to be clever for the moving source sampling
                    "levelMovingSource", SourceAndConverterHelper.bestLevel(fimg[0], timePoint, 0.02),
                    // 40 microns per pixel for the initial registration
                    "pxSizeInCurrentUnit", 0.02
                    );

             task = context
                   .getService(CommandService.class)
                   .run(registrationCommandClass, false,
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
            /*if (zPosition!=null) {
                pt3d.setPosition(zPosition.get(), 2);
            }*/
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

}
