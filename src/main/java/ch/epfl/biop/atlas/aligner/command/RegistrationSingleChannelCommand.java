package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

/**
 * Abstract class which can be extended for all registration uis which
 * support only a single channel for the registration.
 * Validation of the user inputs is performed before the registration is started
 */
abstract public class RegistrationSingleChannelCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Atlas channels", min = "0")
    int channel_atlas;

    @Parameter(label = "Slices channels", min = "0")
    int channel_slice;

    protected boolean validationError = false;

    @Override
    final public void run() {
        if (!validationError) {
            if (channel_atlas >=mp.getNumberOfAtlasChannels()) {
                mp.log.accept("The atlas has only "+mp.getNumberOfAtlasChannels()+" channels!");
                mp.errlog.accept("The atlas has only "+mp.getNumberOfAtlasChannels()+" channels !\n Maximum index : "+(mp.getNumberOfAtlasChannels()-1));
                return;
            }
            if (mp.getSelectedSlices().size()==0) {
                mp.log.accept("No slice selected");
                mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
                return;
            }
            if (channel_slice >=mp.getChannelBoundForSelectedSlices()) {
                mp.log.accept("Missing channel in selected slice(s).");
                mp.errlog.accept("Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }
            runValidated();
        }
    }

    abstract public void runValidated();

    public SourcesProcessor getFixedFilter() {
        return new SourcesChannelsSelect(channel_atlas);
    }

    public SourcesProcessor getMovingFilter() {
        return new SourcesChannelsSelect(channel_slice);
    }

}
