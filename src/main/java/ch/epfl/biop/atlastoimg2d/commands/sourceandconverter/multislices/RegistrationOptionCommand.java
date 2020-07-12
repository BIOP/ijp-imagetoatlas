package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import java.util.function.Function;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Registration Options")
public class RegistrationOptionCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(choices = {"Manual Affine", "Auto Elastix Affine", "Manual BigWarp", "Auto Elastix BigWarp"},
    callback = "registrationChanged")
    String registrationType;

    @Parameter(label = "Atlas channels")
    int atlasImageChannel;

    @Parameter(label = "Slices channels")
    int sliceImageChannel;

    @Parameter(callback = "clicked")
    Button exebutton;

    @Override
    public void run() {
        // Cannot be accessed
    }

    public void clicked() {
        mp.enqueueRegistration(registrationType,
               getFixedFilter(),
               getMovingFilter()
        );
    }

    public Function<SourceAndConverter[], SourceAndConverter[]> getFixedFilter() {
        final int atlasChannel = atlasImageChannel;
        return (sacs) -> new SourceAndConverter[]{sacs[atlasChannel]};
    }

    public Function<SourceAndConverter[], SourceAndConverter[]> getMovingFilter() {
        final int sliceChannel = sliceImageChannel;
        return (sacs) -> new SourceAndConverter[]{sacs[sliceChannel]};
    }

    public void registrationChanged() {
        System.out.println("Registration changed");
    }
}
