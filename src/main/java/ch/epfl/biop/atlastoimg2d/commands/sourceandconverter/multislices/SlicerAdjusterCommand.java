package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import ch.epfl.biop.atlastoimg2d.multislice.ReslicedAtlas;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = InteractiveCommand.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Adjust Slicing")
public class SlicerAdjusterCommand extends InteractiveCommand {

    @Parameter(min = "10", max = "500", stepSize = "10", style = "slider", label = "microns")
    int zSamplingSteps = 10;

    @Parameter(min = "-20", max = "+20", stepSize = "1", style = "slider", label = "half degrees (X)" )
    int rotateX = 0;

    @Parameter(min = "-20", max = "+20", stepSize = "1", style = "slider", label = "half degrees (Y)")
    int rotateY = 0;

    @Parameter
    ReslicedAtlas reslicedAtlas;

    public void run() {
        reslicedAtlas.setStep(zSamplingSteps/10);
        reslicedAtlas.setRotateX(rotateX/360.0*Math.PI);
        reslicedAtlas.setRotateY(rotateY/360.0*Math.PI);
    }

}
