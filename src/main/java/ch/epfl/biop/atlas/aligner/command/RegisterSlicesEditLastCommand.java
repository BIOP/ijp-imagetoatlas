package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
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
        description = "Edit the last registration of the current selected slices, if possible.")
public class RegisterSlicesEditLastCommand implements Command {

    protected static Logger logger = LoggerFactory.getLogger(RegisterSlicesEditLastCommand.class);

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Reuse original channels of the registration")
    boolean reuse_original_channels;

    @Parameter(label = "Atlas channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String atlas_channels_csv = "*";

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String slices_channels_csv = "*";

    @Override
    public void run() {
        logger.info("Edit last registration command called.");

        SourcesProcessor preprocessSlice = SourcesProcessorHelper.Identity();

        SourcesProcessor preprocessAtlas = SourcesProcessorHelper.Identity();

        if (!slices_channels_csv.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(slices_channels_csv.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
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
