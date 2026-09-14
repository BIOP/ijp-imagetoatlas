package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import org.scijava.Initializable;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@SuppressWarnings({"CanBeFinal", "unused"})
@Plugin(type = InteractiveCommand.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>ABBA - Adjust Slicing",
        description = "Tilts the atlas slicing plane to match the sectioning angle of the slices. "
                + "The angles apply to all slices.")
public class AtlasSlicingAdjusterCommand extends InteractiveCommand implements Initializable {

    @Parameter(label = "Lock rotations",
            description = "If checked, the angles cannot be changed, to avoid modifying them by mistake.")
    Boolean lockAngles = Boolean.FALSE;

    @Parameter(min = "-45", max = "+45", stepSize = "0.5", style = "slider,format:0.0", label = "X rotation (degrees)",
            description = "Rotation of the slicing plane around the X axis of the sections (horizontal on screen).",
            callback = "changeRotate", persist = false)
    double rotateX = 0;
    double oldRotateX = 0;

    @Parameter(min = "-45", max = "+45", stepSize = "0.5", style = "slider,format:0.0", label = "Y rotation (degrees)",
            description = "Rotation of the slicing plane around the Y axis of the sections (vertical on screen).",
    callback = "changeRotate", persist = false)
    double rotateY = 0;
    double oldRotateY = 0;

    @Parameter
    ReslicedAtlas reslicedAtlas;

    @Override
    public void initialize() {

        rotateX = reslicedAtlas.getRotateX() * 180.0 / Math.PI;
        oldRotateX = rotateX;

        rotateY = reslicedAtlas.getRotateY() * 180.0 / Math.PI;
        oldRotateY = rotateY;

        // TODO : check if this does not create a memory leak
        reslicedAtlas.addListener(() -> {
            if ((reslicedAtlas.getRotateX() != rotateX / 180.0 * Math.PI)||(reslicedAtlas.getRotateY()!= rotateY / 180.0 * Math.PI)) {
                this.setInput("rotateX", reslicedAtlas.getRotateX() * 180.0 / Math.PI);
                this.setInput("oldRotateX", reslicedAtlas.getRotateX() * 180.0 / Math.PI);
                this.setInput("rotateY", reslicedAtlas.getRotateY() * 180.0 / Math.PI);
                this.setInput("oldRotateY", reslicedAtlas.getRotateY() * 180.0 / Math.PI);
            }
        });
    }

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
