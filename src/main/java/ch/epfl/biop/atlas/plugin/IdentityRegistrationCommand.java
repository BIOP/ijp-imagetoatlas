package ch.epfl.biop.atlas.plugin;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessorHelper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Your Registration Here (dev)")
public class IdentityRegistrationCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Override
    public void run() {
        mp.register(this, SourcesProcessorHelper.Identity(),
                SourcesProcessorHelper.Identity());
    }
}
