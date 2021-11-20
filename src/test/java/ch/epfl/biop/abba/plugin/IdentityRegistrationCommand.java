package ch.epfl.biop.abba.plugin;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>[ABBA Dev] - Your Registration Here!")
public class IdentityRegistrationCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Override
    public void run() {
        mp.registerSelectedSlices(this, SourcesProcessorHelper.Identity(),
                SourcesProcessorHelper.Identity());
    }
}
