package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.command.RegistrationElastixAffineCommand;
import ch.epfl.biop.atlas.aligner.command.RegistrationElastixAffineRemoteCommand;
import ch.epfl.biop.atlas.aligner.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.aligner.plugin.RegistrationTypeProperties;
import ch.epfl.biop.scijava.command.source.register.Elastix2DAffineRegisterCommand;
import ch.epfl.biop.scijava.command.source.register.Elastix2DAffineRegisterServerCommand;
import com.google.gson.Gson;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Uses Elastix backend to perform an affine transform registration
 *
 */
@Plugin(type = IABBARegistrationPlugin.class)
@RegistrationTypeProperties(
        isManual = false,
        isEditable = false,
        userInterface = {
                RegistrationElastixAffineCommand.class,
                RegistrationElastixAffineRemoteCommand.class
        })

public class Elastix2DAffineRegistration extends AffineTransformSourceAndConverterRegistration{


    protected static Logger logger = LoggerFactory.getLogger(Elastix2DAffineRegistration.class);

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

    Future<CommandModule> task;

    @Override
    public boolean register() {
        try {
            Class<? extends Command> registrationCommandClass;
            // Is it supposed to be done on a server ?
            if (parameters.containsKey("serverURL")) {
                // Yes -> changes command class name to the server one
                registrationCommandClass = Elastix2DAffineRegisterServerCommand.class;
                if (parameters.get("userConsentForServerKeepingData").equals("true")) {
                    String taskInfo = new Gson().toJson(sliceInfo);
                    parameters.put("taskInfo", taskInfo);
                }
                parameters.remove("userConsentForServerKeepingData");
            } else {
                registrationCommandClass = Elastix2DAffineRegisterCommand.class;
            }

            // Registration success flag
            boolean success = true;

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
                // No interpolation in resampling
                "interpolate", false,
                // Start registration with a 4x4 pixel image
                "minPixSize",4,
                // 100 iteration steps
                "maxIterationNumberPerScale",100,
                 // Do not write anything
                 "verbose", false,
                 // Centers centers of mass of both images before starting registration
                 "automaticTransformInitialization", true,
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
                            flatParameters.toArray(new Object[0])
                    );

             CommandModule module = task.get();

             if (module.getOutputs().containsKey("success")) {
                 success = (boolean) module.getOutput("success");
             }
             if (success) {
                at3d = (AffineTransform3D) module.getOutput("at3D");
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

    @Override
    public void abort() {
        if (task!=null) {
            logger.info(this.getClass().getSimpleName()+": Attempt to interrupt registration...");
            task.cancel(true);
        }
    }

    public String toString() {
        return "Elastix 2D Affine";
    }

}
