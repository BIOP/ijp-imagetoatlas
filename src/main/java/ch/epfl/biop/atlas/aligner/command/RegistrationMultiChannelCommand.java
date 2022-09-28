package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract class which can be extended for all registration uis which
 * support only a single channel for the registration.
 * Validation of the user inputs is performed before the registration is started
 */
abstract public class RegistrationMultiChannelCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Atlas channels (channels comma separated)")
    String atlas_channels_csv;

    @Parameter(label = "Slices channels (channels comma separated)")
    String slice_channels_csv;

    protected boolean validationError = false;

    List<Integer> atlas_channels;
    List<Integer> slice_channels;

    @Override
    final public void run() {

        try {
            atlas_channels = Arrays.asList(atlas_channels_csv.split(",")).stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            slice_channels = Arrays.asList(slice_channels_csv.split(",")).stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            mp.errlog.accept("Number parsing exception "+e.getMessage());
            return;
        }

        int maxIndexAtlas = Collections.max(atlas_channels);
        int minIndexAtlas = Collections.min(atlas_channels);

        int maxIndexSlices = Collections.max(slice_channels);
        int minIndexSlices = Collections.min(slice_channels);

        if (minIndexAtlas<0) {
            mp.errlog.accept("The atlas channels index should be positive");
            return;
        }

        if (minIndexSlices<0) {
            mp.errlog.accept("The slices channels index should be positive");
            return;
        }

        if (!validationError) {
            if (maxIndexAtlas >=mp.getNumberOfAtlasChannels()) {
                mp.log.accept("The atlas has only "+mp.getNumberOfAtlasChannels()+" channels!");
                mp.errlog.accept("The atlas has only "+mp.getNumberOfAtlasChannels()+" channels !\n Maximum index : "+(mp.getNumberOfAtlasChannels()-1));
                return;
            }
            if (mp.getSelectedSlices().size()==0) {
                mp.log.accept("No slice selected");
                mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
                return;
            }
            if (maxIndexSlices >=mp.getChannelBoundForSelectedSlices()) {
                mp.log.accept("Missing channel in selected slice(s).");
                mp.errlog.accept("Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }
            runValidated();
        }
    }

    abstract public void runValidated();

    public SourcesProcessor getFixedFilter() {
        return new SourcesChannelsSelect(atlas_channels);
    }

    public SourcesProcessor getMovingFilter() {
        return new SourcesChannelsSelect(slice_channels);
    }

}
