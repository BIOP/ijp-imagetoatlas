package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.source.processor.SourcesChannelsSelect;
import ch.epfl.biop.source.processor.SourcesProcessor;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Edit Last Registration",
        description = "Manually edits the last registration of each selected slice, if it is editable: "
                + "spline registrations (Elastix spline, BigWarp) open in BigWarp, affine registrations (Elastix affine, manual, DeepSlice) "
                + "open in the affine editor.")
public class RegisterSlicesEditLastCommand implements Command {

    protected static Logger logger = LoggerFactory.getLogger(RegisterSlicesEditLastCommand.class);

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Display the channels used by the registration",
            description = "If checked, the editor displays the channels used when the registration was made, "
                    + "and the two channel fields below are ignored.")
    boolean reuse_original_channels;

    @Parameter(label = "Atlas channels displayed",
            description = "0-based indices of the atlas channels displayed in the editor, comma separated (e.g. '0,2'), or '*' for all channels. "
                    + "Ignored if the channels used by the registration are displayed.")
    String atlas_channels_csv = "*";

    @Parameter(label = "Slice channels displayed",
            description = "0-based indices of the slice channels displayed in the editor, comma separated (e.g. '0,2'), or '*' for all channels. "
                    + "Ignored if the channels used by the registration are displayed.")
    String slice_channels_csv = "*";

    @Override
    public void run() {
        logger.info("Edit last registration command called.");

        SourcesProcessor preprocessSlice = SourcesProcessorHelper.Identity();

        SourcesProcessor preprocessAtlas = SourcesProcessorHelper.Identity();

        if (!slice_channels_csv.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(slice_channels_csv.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();
            if (maxIndex>=mp.getChannelBoundForSelectedSlices()) {
                mp.errorMessageForUser.accept("Missing channel in selected slice(s).",
                        "Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }
            preprocessSlice = new SourcesChannelsSelect(indices);
        }

        if (!atlas_channels_csv.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(atlas_channels_csv.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();
            int maxChannelInAtlas = mp.getReslicedAtlas().nonExtendedSlicedSources.length;
            if (maxIndex>=maxChannelInAtlas) {
                mp.errorMessageForUser.accept("Missing channels in atlas.",
                        "The atlas only has "+maxChannelInAtlas+" channel(s).\n Maximum index : "+(maxChannelInAtlas-1) );
                return;
            }

            preprocessAtlas = new SourcesChannelsSelect(indices);
        }

        mp.editLastRegistrationSelectedSlices(reuse_original_channels, preprocessSlice, preprocessAtlas);
    }
}
