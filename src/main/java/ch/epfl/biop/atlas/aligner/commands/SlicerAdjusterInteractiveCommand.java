package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = InteractiveCommand.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Adjust Slicing")
public class SlicerAdjusterInteractiveCommand extends InteractiveCommand {

    @Parameter(min = "10", max = "500", stepSize = "10", style = "slider", label = "microns")
    int zSamplingSteps = 10;

    @Parameter(label = "Lock XY angles")
    Boolean lockAngles = Boolean.FALSE;

    @Parameter(min = "-90", max = "+90", stepSize = "1", style = "slider", label = "HALF degrees (X)",
            callback = "changeRotate")
    int rotateX = 0;
    int oldRotateX = 0;

    @Parameter(min = "-90", max = "+90", stepSize = "1", style = "slider", label = "HALF degrees (Y)",
    callback = "changeRotate")
    int rotateY = 0;
    int oldRotateY = 0;

    @Parameter
    ReslicedAtlas reslicedAtlas;

    public void run() {
        reslicedAtlas.setStep(zSamplingSteps/10);
        if (!lockAngles) {
            reslicedAtlas.setRotateX(rotateX / 360.0 * Math.PI);
            reslicedAtlas.setRotateY(rotateY / 360.0 * Math.PI);
            oldRotateX = rotateX;
            oldRotateY = rotateY;
        } else {
            rotateX = oldRotateX;
            rotateY = oldRotateY;
        }
    }

    public void changeRotate() {
        if (lockAngles) {
            rotateX = oldRotateX;
            rotateY = oldRotateY;
        }
    }

}
