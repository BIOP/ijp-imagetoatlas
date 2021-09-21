package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = InteractiveCommand.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>ABBA - Adjust Slicing")
public class SlicerAdjusterInteractiveCommand extends InteractiveCommand {

    @Parameter(label = "Lock rotations")
    Boolean lockAngles = Boolean.FALSE;

    @Parameter(min = "-45", max = "+45", stepSize = "0.5", style = "slider", label = "X Rotation [deg]",
            callback = "changeRotate")
    double rotateX = 0;
    double oldRotateX = 0;

    @Parameter(min = "-45", max = "+45", stepSize = "0.5", style = "slider", label = "Y Rotation [deg]",
    callback = "changeRotate")
    double rotateY = 0;
    double oldRotateY = 0;

    @Parameter
    ReslicedAtlas reslicedAtlas;

    public void run() {

        if (!lockAngles) {
            reslicedAtlas.setRotateX(rotateX / 180.0 * Math.PI);
            reslicedAtlas.setRotateY(rotateY / 180.0 * Math.PI);
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
