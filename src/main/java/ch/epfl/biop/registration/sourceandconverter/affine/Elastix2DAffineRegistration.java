package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.command.register.Elastix2DAffineRegisterCommand;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class Elastix2DAffineRegistration extends AffineTransformSourceAndConverterRegistration{

    Context ctx;

    Map<String, Object> scijavaParameters = new HashMap<>();

    public void setScijavaContext(Context ctx) {
        this.ctx = ctx;
    }

    public void setScijavaParameters(Map<String, Object> scijavaParameters) {
        this.scijavaParameters.putAll(scijavaParameters);
    }

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        super.setFixedImage(this.fimg);
        assert fimg.length==1;
        scijavaParameters.put("sac_fixed", fimg[0]);
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        super.setMovingImage(mimg);
        assert mimg.length==1;
        scijavaParameters.put("sac_moving", mimg[0]);
    }

    Class<? extends Command> registrationCommandClass = Elastix2DAffineRegisterCommand.class;

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
                    .run(registrationCommandClass, false, scijavaParameters );

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
            //e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean parallelSupported() {
        return true;
    }

    @Override
    public boolean isManual() {
        return false;
    }

    private boolean isDone = false;

    @Override
    public boolean isRegistrationDone() {
        return isDone;
    }

    public void setDone() {
        this.isDone = true;
    }

    @Override
    public void resetRegistration() {
        super.resetRegistration();
        isDone = false;
    }

    @Override
    public void abort() {
        if (task!=null) {
            System.out.println("Cancel Attempt");
            task.cancel(true);
        }
    }
}
