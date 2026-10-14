package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.source.processor.SourcesChannelsSelect;
import ch.epfl.biop.source.processor.SourcesProcessor;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

/**
 * Abstract class which can be extended for all registration uis which
 * support only a single channel for the registration.
 * Validation of the user inputs is performed before the registration is started
 */
abstract public class RegistrationSingleChannelCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Atlas channel", min = "0",
            description = "0-based index of the atlas channel used for the registration.")
    int atlas_channel;

    @Parameter(label = "Slice channel", min = "0",
            description = "0-based index of the slice channel used for the registration.")
    int slice_channel;

    protected boolean validationError = false;

    @Override
    final public void run() {
        if (!validationError) {
            if (atlas_channel >= mp.getNumberOfAtlasChannels()) {
                fail("The atlas has only "+mp.getNumberOfAtlasChannels()+" channels!",
                        "The atlas has only "+mp.getNumberOfAtlasChannels()+" channels !\n Maximum index : "+(mp.getNumberOfAtlasChannels()-1));
            }
            if (mp.getSelectedSlices().isEmpty()) {
                fail("No slice selected","Please select the slice(s) you want to register");
            }
            if (slice_channel >=mp.getChannelBoundForSelectedSlices()) {
                fail("Missing channel in selected slice(s).",
                        "Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
            }
            runValidated();
        }
    }

    abstract public void runValidated();

    /**
     * Shows the error to the user and fails the command, see {@link RegistrationMultiChannelCommand#fail(String, String)}
     */
    protected void fail(String title, String message) {
        mp.errorMessageForUser.accept(title, message);
        throw new IllegalArgumentException(title + ": " + message);
    }

    public SourcesProcessor getFixedFilter() {
        return new SourcesChannelsSelect(atlas_channel);
    }

    public SourcesProcessor getMovingFilter() {
        return new SourcesChannelsSelect(slice_channel);
    }

}
