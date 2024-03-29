package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Rotate",
        description = "To use at the beginning of the registration process only! Rotates the original unregistered selected slices")
public class RotateSlicesCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Rotation axis", choices = {"Z","Y","X"})
    String axis_string;

    @Parameter(label = "Angle (degrees)", style="format:0.00")
    double angle_degrees;

    @Override
    public void run() {
        double angle_rad = angle_degrees/180.0*Math.PI;
        int axis = 0;
        switch (axis_string) {
            case "X":axis=0;
            break;
            case "Y":axis=1;
            break;
            case "Z":axis=2;
            break;
        }
        mp.rotateSlices(axis, angle_rad);
    }
}
