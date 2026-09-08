package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.source.processor.SourcesChannelsSelect;
import ch.epfl.biop.source.processor.SourcesProcessor;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract class which can be extended for all registration uis which
 * support only a single channel for the registration.
 * Validation of the user inputs is performed before the registration is started
 */
abstract public class RegistrationMultiChannelCommand implements Command {

    protected static final Logger logger = LoggerFactory.getLogger(RegistrationMultiChannelCommand.class);

    @Parameter(style = "message", visibility = ItemVisibility.MESSAGE)
    String message = getMessage();

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Atlas channels (channels comma separated)")
    String channels_atlas_csv;

    @Parameter(label = "Slices channels (channels comma separated)")
    String channels_slice_csv;

    protected boolean validationError = false;

    List<Integer> atlas_channels;
    List<Integer> slice_channels;

    @Override
    final public void run() {

        try {
            atlas_channels = Arrays.stream(channels_atlas_csv.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            slice_channels = Arrays.stream(channels_slice_csv.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            mp.errorMessageForUser.accept("Error in numeric input", "Number parsing exception "+e.getMessage());
            return;
        }

        if (atlas_channels.isEmpty()) {
            mp.errorMessageForUser.accept("No Atlas channel", "Error, you did not specify any atlas channel.");
            return;
        }

        if (slice_channels.isEmpty()) {
            mp.errorMessageForUser.accept("No Slice channel", "Error, you did not specify any slice channel.");
            return;
        }

        int maxIndexAtlas = Collections.max(atlas_channels);
        int minIndexAtlas = Collections.min(atlas_channels);

        int maxIndexSlices = Collections.max(slice_channels);
        int minIndexSlices = Collections.min(slice_channels);

        if (minIndexAtlas<0) {
            mp.errorMessageForUser.accept("Negative index!", "The atlas channels index should be positive");
            return;
        }

        if (minIndexSlices<0) {
            mp.errorMessageForUser.accept("Negative index!", "The slices channels index should be positive");
            return;
        }

        if (!validationError) {
            if (maxIndexAtlas >=mp.getNumberOfAtlasChannels()) {
                mp.errorMessageForUser.accept("Issue with channels numbers","The atlas has only "+mp.getNumberOfAtlasChannels()+" channels !\n Maximum index : "+(mp.getNumberOfAtlasChannels()-1));
                return;
            }
            if (mp.getSelectedSlices().isEmpty()) {
                mp.errorMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
                return;
            }
            if (maxIndexSlices >=mp.getChannelBoundForSelectedSlices()) {
                mp.errorMessageForUser.accept("Issue with channels numbers","Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }
            if (requiresConsistentPixelTypes() && !checkPixelTypesConsistency()) return;
            runValidated();
        }
    }

    /**
     * Registrations which build a single multichannel ImagePlus out of the selected channels -
     * Elastix - need all channels of a stack to share the same pixel type. Registrations which
     * work directly on the sources - BigWarp - do not care.
     *
     * @return true if the selected channels should be checked for pixel type consistency before
     * the registration is started
     */
    protected boolean requiresConsistentPixelTypes() {
        return false;
    }

    /**
     * Mixing, for instance, an 8 bit channel with a 16 bit one in the same stack otherwise fails
     * deep in the image export with an unhelpful ClassCastException. This check reports the issue
     * to the user, or to the log only when no error subscriber is registered, which is the case
     * in headless mode.
     *
     * @return true if all selected atlas channels share the same pixel type and if, for each
     * selected slice, all selected slice channels share the same pixel type
     */
    protected boolean checkPixelTypesConsistency() {
        String atlasTypes = getInconsistentPixelTypes(atlas_channels, mp.getReslicedAtlas().nonExtendedSlicedSources);
        if (atlasTypes!=null) {
            mp.errorMessageForUser.accept("Inconsistent atlas pixel types",
                    "The selected atlas channels do not share the same pixel type ("+atlasTypes+").\n"
                            +"All channels used for this type of registration should have the same pixel type.\n"
                            +"Please select atlas channels which share the same pixel type.");
            return false;
        }
        for (SliceSources slice : mp.getSelectedSlices()) {
            String sliceTypes = getInconsistentPixelTypes(slice_channels, slice.getRegisteredSources());
            if (sliceTypes!=null) {
                mp.errorMessageForUser.accept("Inconsistent slice pixel types",
                        "The selected channels of the slice '"+slice.getName()+"' do not share the same pixel type ("+sliceTypes+").\n"
                                +"All channels used for this type of registration should have the same pixel type.\n"
                                +"Please select slice channels which share the same pixel type.");
                return false;
            }
        }
        return true;
    }

    /**
     * @param channels the channel indices which are used for the registration
     * @param sources all sources of the atlas or of a slice
     * @return null if all selected channels have the same pixel type - or if the pixel types
     * could not be probed -, and a human readable description of the pixel types otherwise
     */
    private static String getInconsistentPixelTypes(List<Integer> channels, SourceAndConverter<?>[] sources) {
        List<String> types = new ArrayList<>(channels.size());
        for (int channel : channels) {
            try {
                types.add(sources[channel].getSpimSource().getType().getClass().getSimpleName());
            } catch (Exception e) {
                // The pixel type can't be probed: let the registration go on, it may well work
                logger.debug("Could not get the pixel type of channel {}", channel, e);
                return null;
            }
        }
        if (new HashSet<>(types).size()<2) return null;
        StringBuilder description = new StringBuilder();
        for (int i = 0; i<channels.size(); i++) {
            if (i>0) description.append(", ");
            description.append("channel ").append(channels.get(i)).append(": ").append(types.get(i));
        }
        return description.toString();
    }

    abstract public void runValidated();

    public SourcesProcessor getFixedFilter() {
        return new SourcesChannelsSelect(atlas_channels);
    }

    public SourcesProcessor getMovingFilter() {
        return new SourcesChannelsSelect(slice_channels);
    }

    abstract protected String getMessage();
}
