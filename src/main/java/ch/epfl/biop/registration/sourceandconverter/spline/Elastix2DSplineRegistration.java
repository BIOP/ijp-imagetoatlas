package ch.epfl.biop.registration.sourceandconverter.spline;

import bdv.tools.brightness.ConverterSetup;
import bdv.util.BdvHandle;
import bdv.viewer.DisplayMode;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixSplineCommand;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixSplineRemoteCommand;
import ch.epfl.biop.atlas.aligner.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.aligner.plugin.RegistrationTypeProperties;
import ch.epfl.biop.bdv.command.register.Elastix2DSplineRegisterCommand;
import ch.epfl.biop.bdv.command.register.Elastix2DSplineRegisterServerCommand;
import com.google.gson.Gson;
import ij.gui.WaitForUserDialog;
import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.*;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.register.BigWarpLauncher;

import java.util.*;
import java.util.concurrent.Future;
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

    protected static Logger logger = LoggerFactory.getLogger(Elastix2DSplineRegistration.class);

    Future<CommandModule> task;

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        if (fimg.length==0) {
            logger.error("Error, no fixed image set in class "+this.getClass().getSimpleName());
        }
        if (fimg.length>1) {
            logger.warn("Multichannel image registration not supported for class "+this.getClass().getSimpleName());
        }
        super.setFixedImage(fimg);
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        if (mimg.length==0) {
            logger.error("Error, no fixed image set in class "+this.getClass().getSimpleName());
        }
        if (mimg.length>1) {
            logger.warn("Multichannel image registration not supported for class "+this.getClass().getSimpleName());
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

            double voxSizeInMm = Double.parseDouble(parameters.get("pxSizeInCurrentUnit"));

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
                    "levelFixedSource", SourceAndConverterHelper.bestLevel(fimg[0], timePoint, voxSizeInMm),
                    // Timepoint moving source (normally 0)
                    "tpMoving", timePoint,
                    // Tries to be clever for the moving source sampling
                    "levelMovingSource", SourceAndConverterHelper.bestLevel(mimg[0], timePoint, voxSizeInMm)
                    );

             task = context
                   .getService(CommandService.class)
                   .run(registrationCommandClass, false,
                           flatParameters.toArray(new Object[0]));

            CommandModule module = task.get();

            if (module.getOutputs().containsKey("success")) {
                success = (boolean) module.getOutput("success");
            }

            if (success) {
                rt = (RealTransform) module.getOutput("rt");
                rt = pruneLandMarksOutsideAtlas(rt);
            } else {
                if (module.getOutputs().containsKey("error")) {
                    errorMessage = (String) module.getOutput("error");
                }
            }


            isDone = true;
            return success;
        } catch (Exception e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    String errorMessage = "";

    @Override
    public String getExceptionMessage() {
        return errorMessage;
    }

    /**
     * This function removes the landmarks located outside the atlas,
     * meaning where the atlas 3d image value is zero
     * 4 landmarks are kept
     * @param rt_in
     * @return same transform with landmarks pruned
     */
    private RealTransform pruneLandMarksOutsideAtlas(RealTransform rt_in) {

        RealTransform input = rt_in;
        boolean wrapped2d3d = false;
        boolean wrappedInvertible = false;
        if (rt_in instanceof Wrapped2DTransformAs3D) {
            rt_in = ((Wrapped2DTransformAs3D)rt_in).transform;
            wrapped2d3d = true;
        }

        if (rt_in instanceof WrappedIterativeInvertibleRealTransform) {
            rt_in = ((WrappedIterativeInvertibleRealTransform)rt_in).getTransform();
            wrappedInvertible = true;
        }

        /*if (rt_in instanceof BoundedRealTransform) {
            rt_in = ((BoundedRealTransform)rt_in).getTransform();
            if (rt_in instanceof Wrapped2DTransformAs3D) {
                rt_in = ((Wrapped2DTransformAs3D)rt_in).transform;
            }

            if (rt_in instanceof WrappedIterativeInvertibleRealTransform) {
                rt_in = ((WrappedIterativeInvertibleRealTransform)rt_in).getTransform();
            }
        }*/

        if (!(rt_in instanceof ThinplateSplineTransform)) {
            System.err.println("Cannot edit the transform : it's not of class thinplatesplinetransform");
            return input;
        } else {
            if (fimg_mask !=null) {
                ThinplateSplineTransform tst = (ThinplateSplineTransform) rt_in;
                ThinPlateR2LogRSplineKernelTransform kernel = ThinPlateSplineTransformAdapter.getKernel(tst);
                double[][] srcPts = ThinPlateSplineTransformAdapter.getSrcPts(kernel);
                double[][] tgtPts = ThinPlateSplineTransformAdapter.getTgtPts(kernel);
                int nbLandmarks = kernel.getNumLandmarks();
                int nbDimensions = kernel.getNumDims();

                List<RealPoint> ptsSource = new ArrayList<>();
                List<RealPoint> ptsTarget = new ArrayList<>();

                for (int i = 0; i < nbLandmarks; ++i) {
                    RealPoint ptSource = new RealPoint(3);
                    RealPoint ptTarget = new RealPoint(3);
                    int d;
                    for (d = 0; d < nbDimensions; ++d) {
                        ptTarget.setPosition(tgtPts[d][i], d);
                    }
                    ptTarget.setPosition(0,2); // 0 position in z

                    for (d = 0; d < nbDimensions; ++d) {
                        ptSource.setPosition(srcPts[d][i], d);
                    }
                    ptSource.setPosition(0,2); // 0 position in z

                    ptsSource.add(ptSource);
                    ptsTarget.add(ptTarget);
                }

                RealRandomAccessible<IntegerType> mask = fimg_mask[0].getSpimSource().getInterpolatedSource(timePoint,0, Interpolation.NEARESTNEIGHBOR);

                AffineTransform3D at3D = new AffineTransform3D();
                fimg_mask[0].getSpimSource().getSourceTransform(timePoint,0,at3D);

                List<Integer> landMarksToKeep = new ArrayList<>();
                for (int i = 0; i < nbLandmarks; ++i) {
                    at3D.inverse().apply(ptsTarget.get(i), ptsTarget.get(i));
                    at3D.inverse().apply(ptsSource.get(i), ptsSource.get(i));

                    if ((mask.getAt(ptsSource.get(i)).getInteger() == 0) && (mask.getAt(ptsTarget.get(i)).getInteger() == 0)) {

                    } else {
                        landMarksToKeep.add(i);
                    }
                }

                if (landMarksToKeep.size()<4) {
                    // Too many landmarks removed
                    System.out.println("Too few landmarks after pruning - skip pruning");
                    return input;
                }

                // Ok, now let's reconstruct the transform

                double[][] srcPtsKept = new double[nbDimensions][landMarksToKeep.size()];
                double[][] tgtPtsKept = new double[nbDimensions][landMarksToKeep.size()];

                for (int i = 0;i<landMarksToKeep.size();i++) {
                    for (int d = 0; d < nbDimensions; ++d) {
                        srcPtsKept[d][i] = srcPts[d][landMarksToKeep.get(i)];
                        tgtPtsKept[d][i] = tgtPts[d][landMarksToKeep.get(i)];
                    }
                }

                RealTransform pruned = new ThinplateSplineTransform(srcPtsKept, tgtPtsKept);

                if (wrappedInvertible) {
                    pruned = new WrappedIterativeInvertibleRealTransform<>(pruned);
                }

                if (wrapped2d3d) {
                    pruned = new Wrapped2DTransformAs3D((InvertibleRealTransform) pruned);
                }

                return pruned;

            } else return input;
        }
    }

    Runnable waitForUser = () -> {
        WaitForUserDialog dialog = new WaitForUserDialog("Choose slice","Please perform carefully your registration then press ok.");
        dialog.show();
    };

    @Override
    public boolean edit() {

        List<SourceAndConverter> movingSacs = Arrays.stream(mimg).collect(Collectors.toList());

        List<SourceAndConverter> fixedSacs = Arrays.stream(fimg).collect(Collectors.toList());

        List<ConverterSetup> converterSetups = Arrays.stream(mimg).map(src -> SourceAndConverterServices.getBdvDisplayService().getConverterSetup(src)).collect(Collectors.toList());

        converterSetups.addAll(Arrays.stream(fimg).map(src -> SourceAndConverterServices.getBdvDisplayService().getConverterSetup(src)).collect(Collectors.toList()));

        // Launch BigWarp
        BigWarpLauncher bwl = new BigWarpLauncher(movingSacs, fixedSacs, "Big Warp", converterSetups);
        bwl.set2d();
        bwl.run();

        // Output bdvh handles -> will be put in the object service
        BdvHandle bdvhQ = bwl.getBdvHandleQ();
        BdvHandle bdvhP = bwl.getBdvHandleP();

        bdvhP.getViewerPanel().state().setViewerTransform(BdvHandleHelper.getViewerTransformWithNewCenter(bdvhP, new double[]{0,0,0}));
        bdvhQ.getViewerPanel().state().setViewerTransform(BdvHandleHelper.getViewerTransformWithNewCenter(bdvhQ, new double[]{0,0,0}));

        bdvhQ.getViewerPanel().state().setDisplayMode(DisplayMode.FUSED);
        bdvhP.getViewerPanel().state().setDisplayMode(DisplayMode.FUSED);

        SourceAndConverterServices.getBdvDisplayService().pairClosing(bdvhQ,bdvhP);

        bdvhP.getViewerPanel().requestRepaint();
        bdvhQ.getViewerPanel().requestRepaint();

        bwl.getBigWarp().getLandmarkFrame().repaint();

        if (rt!=null) {
            bwl.getBigWarp().loadLandmarks(BigWarpFileFromRealTransform(rt));
            bwl.getBigWarp().setIsMovingDisplayTransformed(true);
        }

        waitForUser.run();

        rt = bwl.getBigWarp().getBwTransform().getTransformation();

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

    public String toString() {
        return "Elastix 2D Spline";
    }

}
