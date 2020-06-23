package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import ch.epfl.biop.atlastoimg2d.multislice.ReslicedAtlas;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = InteractiveCommand.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Adjust Slicing")
public class SlicerAdjusterCommand extends InteractiveCommand {

    @Parameter(min = "1", max = "50", stepSize = "1", style = "slider")
    int zSamplingSteps = 1;

    @Parameter(min = "-20", max = "+20", stepSize = "1", style = "slider")
    int rotateX = 0;

    @Parameter(min = "-20", max = "+20", stepSize = "1", style = "slider")
    int rotateY = 0;

    @Parameter
    ReslicedAtlas reslicedAtlas;

    public void run() {
        reslicedAtlas.setStep(zSamplingSteps);
        reslicedAtlas.setRotateX(rotateX/180.0*Math.PI);
        reslicedAtlas.setRotateY(rotateY/180.0*Math.PI);
    }

}
