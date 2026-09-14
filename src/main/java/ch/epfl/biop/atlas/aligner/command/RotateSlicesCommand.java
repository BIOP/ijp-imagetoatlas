package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.InPlaneTransform;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Rotate",
        description = "Rotates the selected slices around the center of the registration ROI. Z: in-plane rotation, clockwise on screen. X or Y: 180 or -180 degrees only (flip)")
public class RotateSlicesCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Rotation axis", choices = {"Z","Y","X"},
            description = "'Z': in-plane rotation, any angle. 'X' or 'Y': flips the section upside down or left-right, "
                    + "only 180 or -180 degrees are accepted.")
    String axis_string;

    @Parameter(label = "Angle (degrees)", style="format:0.00",
            description = "Rotation angle. Around Z, positive is clockwise on screen.")
    double angle_degrees;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSelectedSlices();
        if (axis_string.equals("Z")) {
            double[] center = mp.getROICenter();
            mp.transformSlicesInPlane(slices, InPlaneTransform.rotation(angle_degrees/180.0*Math.PI, center[0], center[1]).toAffine(), "Rotate");
        } else if (Math.abs(angle_degrees) == 180.0) {
            mp.flipSlices(slices, axis_string.equals("X") ? 0 : 1);
        } else {
            mp.errorMessageForUser.accept("Rotation not supported",
                    "Around the X or Y axis, only 180 or -180 degrees are supported: other angles would tilt the section out of its plane.");
        }
    }
}
