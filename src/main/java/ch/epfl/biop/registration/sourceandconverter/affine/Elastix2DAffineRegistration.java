package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixAffineCommand;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixAffineRemoteCommand;
import ch.epfl.biop.atlas.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.plugin.RegistrationTypeProperties;
import ch.epfl.biop.bdv.command.register.Elastix2DAffineRegisterCommand;
import ch.epfl.biop.bdv.command.register.Elastix2DAffineRegisterServerCommand;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;
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

    Future<CommandModule> task;

    @Override
    public boolean register() {
        try {
            Class<? extends Command> registrationCommandClass;
            // Is it supposed to be done on a server ?
            if (parameters.containsKey("serverURL")) {
                // Yes -> changes command class name
                registrationCommandClass = Elastix2DAffineRegisterServerCommand.class;
            } else {
                registrationCommandClass = Elastix2DAffineRegisterCommand.class;
            }

            // Registration success flag
            boolean success = true;

            // Transforms map into flat String : key1, value1, key2, value2, etc.
            // Necessary for CommandService
            List<Object> flatParameters = new ArrayList<>(parameters.size()*2+4);

            // Start registration with a 4x4 pixel iamge
            parameters.put("minPixSize","4");
            // 150 iteration steps
            parameters.put("maxIterationNumberPerScale","150");
            // Do not write anything
            parameters.put("verbose", "false");
            // Centers centers of mass of both images before starting registration
            parameters.put("automaticTransformInitialization", "true");
            // Atlas image : a single timepoint
            parameters.put("tpFixed", "0");
            // Level 2 for the atlas
            parameters.put("levelFixedSource", "2");
            // Timepoint moving source (normally 0)
            parameters.put("tpMoving", Integer.toString(timePoint));
            // Tries to be clever for the moving source sampling
            parameters.put("levelMovingSource", Integer.toString(SourceAndConverterHelper.bestLevel(fimg[0], timePoint, 0.04)));
            // 40 microns per pixel for the initial registration
            parameters.put("pxSizeInCurrentUnit", "0.04");
            // No interpolation in resampling
            parameters.put("interpolate", "false");

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
                    .run(registrationCommandClass, false,
                            flatParameters.toArray(new Object[0])
                    );

             CommandModule module = task.get();

             if (module.getOutputs().keySet().contains("success")) {
                 success = (boolean) module.getOutput("success");
             }
             if (success) {
                at3d = (AffineTransform3D) module.getOutput("at3D");
             }

             isDone = true;
             return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void abort() {
        if (task!=null) {
            log.accept(this.getClass().getSimpleName()+": Attempt to interrupt registration...");
            task.cancel(true);
        }
    }

}
