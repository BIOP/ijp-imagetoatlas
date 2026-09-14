package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - Align Big Brains and Atlases (no GUI)",
        description = "Starts an ABBA session without graphical user interface (for scripting), with an atlas "
                + "sliced along the chosen orientation. Outputs the aligner object used by the other ABBA commands.")
public class ABBAStartCommand implements Command {

    // The presence of this parameter button will trigger MessageResolverProcessor
    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "<html>Select the atlas slicing orientation:<br>"
            + "click a preset or set the anatomical direction of each axis.</html>";

    @Parameter(callback = "coronalCB", label = "Coronal",
            description = "Sets the axes for coronal sections: X = RL, Y = SI, Z = AP.")
    Button coronal;

    @Parameter(callback = "sagittalCB", label = "Sagittal",
            description = "Sets the axes for sagittal sections: X = AP, Y = SI, Z = LR.")
    Button sagittal;

    @Parameter(callback = "horizontalCB", label = "Horizontal",
            description = "Sets the axes for horizontal sections: X = LR, Y = AP, Z = SI.")
    Button horizontal;

    @Parameter(label = "X axis (sections, left to right)",
            description = "Anatomical direction of the horizontal axis of the displayed sections, from left to right on screen. "
                    + "Coronal: RL, sagittal: AP, horizontal: LR.",
            choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String x_axis;

    @Parameter(label = "Y axis (sections, top to bottom)",
            description = "Anatomical direction of the vertical axis of the displayed sections, from top to bottom on screen. "
                    + "Coronal: SI, sagittal: SI, horizontal: AP.",
            choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String y_axis;

    @Parameter(label = "Z axis (slicing axis)",
            description = "Anatomical direction of the slicing axis, along which the sections are positioned, in increasing position. "
                    + "Coronal: AP, sagittal: LR, horizontal: SI. The three axes must be different.",
            choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String z_axis;

    @Parameter(label = "Atlas",
            description = "Atlas to register the slices to, as output by an atlas opening command.")
    public Atlas ba;

    @Parameter
    Context context;

    @Parameter
    ObjectService os;

    @Parameter(type = ItemIO.OUTPUT, label = "ABBA session",
            description = "The ABBA aligner (MultiSlicePositioner) holding the atlas and the slices.")
    MultiSlicePositioner mp;

    @Override
    public void run() {
        AffineTransform3D orientation;
        try {
            orientation = ReslicedAtlas.getTransformFromCoronal(
                        x_axis.substring(0,2),
                        y_axis.substring(0,2),
                        z_axis.substring(0,2)
                    );
        } catch (IllegalArgumentException exception) {
            System.err.println("Incorrect arguments, you need to use all three axes.");
            return;
        }

        AffineTransform3D slicingTransform = new AffineTransform3D();
        slicingTransform.set(ba.getMap().getCoronalTransform());
        slicingTransform.concatenate(orientation);

        ReslicedAtlas ra = new ReslicedAtlas(ba);
        ra.setResolution(ba.getMap().getAtlasPrecisionInMillimeter());
        ra.setSlicingTransform(slicingTransform);

        mp = new MultiSlicePositioner(ba, ra, context);
        os.addObject(mp);

    }

    void coronalCB() {
        this.x_axis = "RL (Right-Left)";
        this.y_axis = "SI (Superior-Inferior)";
        this.z_axis = "AP (Anterior-Posterior)";
    }

    void horizontalCB() {
        this.x_axis = "LR (Left-Right)";
        this.y_axis = "AP (Anterior-Posterior)";
        this.z_axis = "SI (Superior-Inferior)";
    }

    void sagittalCB() {
        this.x_axis = "AP (Anterior-Posterior)";
        this.y_axis = "SI (Superior-Inferior)";
        this.z_axis = "LR (Left-Right)";
    }

}
