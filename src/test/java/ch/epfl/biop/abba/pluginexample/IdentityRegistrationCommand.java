package ch.epfl.biop.abba.pluginexample;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>[ABBA Dev] - Your Registration Here!")
public class IdentityRegistrationCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Override
    public void run() {
        mp.registerSelectedSlices(IdentityRegistrationPluginExample.class, SourcesProcessorHelper.Identity(),
                SourcesProcessorHelper.Identity(), new HashMap<>());
    }
}
