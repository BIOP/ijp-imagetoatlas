package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.InPlaneTransform;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Rotate",
        description = "Rotates the selected slices around the center of the registration ROI. Z: in-plane rotation, clockwise on screen")
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
        List<SliceSources> slices = mp.getSelectedSlices();
        if (axis_string.equals("Z")) {
            double[] center = mp.getROICenter();
            mp.transformSlicesInPlane(slices, InPlaneTransform.rotation(angle_rad, center[0], center[1]).toAffine(), "Rotate");
        } else {
            int axis = axis_string.equals("X") ? 0 : 1;
            slices.forEach(slice -> {
                AffineTransform3D at3d = slice.getTransformSourceOrigin();
                at3d.rotate(axis, angle_rad);
                slice.transformSourceOrigin(at3d);
            });
        }
    }
}
