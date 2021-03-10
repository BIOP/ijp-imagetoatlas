package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.command.register.Elastix2DAffineRegisterCommand;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Uses Elastix backend to perform an affine transform registration
 *
 */
public class Elastix2DAffineRegistration extends AffineTransformSourceAndConverterRegistration{

    Map<String, Object> parameters = new HashMap<>();

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
        super.setFixedImage(this.fimg);

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
             task = context
                    .getService(CommandService.class)
                    .run(registrationCommandClass, false,
                            parameters,
                            "sac_fixed", fimg[0],
                            "sac_moving", mimg[0]);

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
