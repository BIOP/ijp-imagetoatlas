package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixAffineCommand;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixAffineRemoteCommand;
import ch.epfl.biop.atlas.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.plugin.RegistrationTypeProperties;
import ch.epfl.biop.bdv.command.register.Elastix2DAffineRegisterCommand;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.command.DefaultCommandService;
import org.scijava.module.DefaultModuleService;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    Map<String, String> parameters = new HashMap<>();

    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {
        this.parameters.putAll(parameters);
    }

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

    Class<? extends Command> registrationCommandClass = Elastix2DAffineRegisterCommand.class;

    // TODO : set command name in parameters instead...
    public void setRegistrationCommand(Class<? extends Command> registrationCommandClass) {
        this.registrationCommandClass = registrationCommandClass;
    }

    Future<CommandModule> task = null;

    @Override
    public boolean register() {
        try {
            boolean success = true;

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
