package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.collections.CollectionUtils;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// TODO : makes bounds atlas agnostic

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Interactive Transform",
        description = "To use at the beginning of the registration process only! Rotates, scales, translate the original unregistered selected slices")
public class TransformSourcesInteractiveCommand extends InteractiveCommand {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Rotation axis", choices = {"Z","Y","X"})
    String axis_string;

    @Parameter(label = "Angle (degrees)", style = "slider,format:0.00,", min = "-180.0", max = "180.0", stepSize = "0.25", persist = false)
    double angle_degrees = Double.MIN_VALUE; // otherwise the slider is not properly initialized...

    @Parameter(label = "Scale X", style = "slider,format:0.00", min = "0.5", max = "2.0", stepSize = "0.01", persist = false)
    double scale_X = 1.0f;

    @Parameter(label = "Scale Y", style = "slider,format:0.00", min = "0.5", max = "2.0", stepSize = "0.01", persist = false)
    double scale_Y = 1.0f;

    @Parameter(label = "Translation X (mm)", style = "slider,format:0.00", min = "-5.0", max = "5.0", stepSize = "0.025", persist = false)
    double translate_X = Double.MIN_VALUE; // otherwise the slider is not properly initialized...

    @Parameter(label = "Translation Y (mm)", style = "slider,format:0.00", min = "-5.0", max = "5.0", stepSize = "0.025", persist = false)
    double translate_Y = Double.MIN_VALUE; // otherwise the slider is not properly initialized...

    @Parameter(label = "Restore initial settings", callback = "reset")
    Button reset;

    // To save the initial state
    Map<SliceSources, AffineTransform3D> originalTransforms = null;
    Set<SliceSources> selectedSlices = null;

    @Override
    public void run() {
        if (mp!=null) {
            if ((selectedSlices == null)) {
                selectedSlices = new HashSet<>();
                selectedSlices.addAll(mp.getSelectedSources());
                originalTransforms = new HashMap<>();
                boolean oneRegistrationOccured = false;
                for (SliceSources slice : selectedSlices) {
                    originalTransforms.put(slice, slice.getTransformSourceOrigin());
                    System.out.println(slice.getNumberOfRegistrations());
                    if (slice.getNumberOfRegistrations() != 0) {
                        oneRegistrationOccured = true;
                    }
                }
                if (oneRegistrationOccured) {
                    mp.warningMessageForUser.accept("Warning", "These transformations should be applied before any registration is performed!");
                }
            }

            if (selectedSlices.size()==0) {
                mp.errorMessageForUser.accept("No selected slice", "Please select a slice and rerun the function");
            } else {
                if (!CollectionUtils.isEqualCollection(selectedSlices, mp.getSelectedSources())) {
                    mp.warningMessageForUser.accept("Warning: selected slices have changed!","Please close and restart this command if you want to transform different slices. Otherwise restore the original selection");
                } else {
                    double angle_rad = angle_degrees / 180.0 * Math.PI;
                    int axis = 0;
                    switch (axis_string) {
                        case "X":
                            axis = 0;
                            break;
                        case "Y":
                            axis = 1;
                            break;
                        case "Z":
                            axis = 2;
                            break;
                    }
                    for (SliceSources slice : selectedSlices) {
                        AffineTransform3D at3d = originalTransforms.get(slice).copy();
                        at3d.rotate(axis, angle_rad);
                        at3d.scale(scale_X, scale_Y, 1.0);
                        at3d.translate(translate_X, translate_Y, 0);
                        slice.transformSourceOrigin(at3d);
                    }
                    if (mp.getBdvh() != null) {
                        mp.getBdvh().getViewerPanel().requestRepaint();
                    }
                }
            }
        }
    }

    public void reset() {
        if (mp!=null) {
            if (selectedSlices != null) {
                selectedSlices.forEach(slice -> {
                    AffineTransform3D at3d = originalTransforms.get(slice);
                    slice.transformSourceOrigin(at3d);
                });
            }
            if (mp.getBdvh() != null) {
                mp.getBdvh().getViewerPanel().requestRepaint();
            }
        }
        angle_degrees = 0;
        scale_X = 1;
        scale_Y = 1;
        translate_X = 0;
        translate_Y = 0;
    }
}
