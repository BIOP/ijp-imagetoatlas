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
        description = "Starts ABBA from an Atlas")
public class ABBAStartCommand implements Command {

    // The presence of this parameter button will trigger MessageResolverProcessor
    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Select the atlas slicing orientation";

    @Parameter(callback = "coronalCB")
    Button coronal;

    @Parameter(callback = "sagittalCB")
    Button sagittal;

    @Parameter(callback = "horizontalCB")
    Button horizontal;

    @Parameter(choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String x_axis;

    @Parameter(choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String y_axis;

    @Parameter(choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String z_axis;

    @Parameter
    public Atlas ba;

    @Parameter
    Context context;

    @Parameter
    ObjectService os;

    @Parameter(type = ItemIO.OUTPUT)
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
