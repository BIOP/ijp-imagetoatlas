package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Set Atlas Slicing Angles",
        description = "Sets the angles at which the atlas is sliced, for all slices of the session: the correction for a "
                + "cutting plane that was not perfectly orthogonal. DeepSlice sets them when 'Adjust atlas slicing angle' is "
                + "checked. Not undoable: note the current angles first (ABBA - Get State, rotationX and rotationY in radians).")
public class SetAtlasSlicingAnglesCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Rotation around X (degrees)", description = "Rotation of the slicing plane around the horizontal axis of the sections.")
    double x_degrees = 0;

    @Parameter(label = "Rotation around Y (degrees)", description = "Rotation of the slicing plane around the vertical axis of the sections.")
    double y_degrees = 0;

    @Parameter(type = ItemIO.OUTPUT, label = "Angles", description = "The angles now in use, in degrees.")
    String angles;

    @Override
    public void run() {
        mp.waitForTasks();
        ReslicedAtlas atlas = mp.getReslicedAtlas();
        atlas.setRotateX(Math.toRadians(x_degrees));
        atlas.setRotateY(Math.toRadians(y_degrees));
        double x = Math.toDegrees(atlas.getRotateX()), y = Math.toDegrees(atlas.getRotateY());
        angles = String.format("x=%.3f y=%.3f", x, y);
        if (Math.abs(x - x_degrees) > 1e-6 || Math.abs(y - y_degrees) > 1e-6) {
            throw new IllegalStateException("The atlas slicing is locked; angles are still " + angles);
        }
    }
}
