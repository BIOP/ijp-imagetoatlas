package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Set Slices Min Max Display Range",
        description = "Sets the display range (min and max displayed values) of channels of the selected slices. "
                + "Besides the display, it changes the images sent to DeepSlice: avoid saturated images.")
public class SetSlicesDisplayRangeCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Channels",
            description = "0-based indices of the channels to adjust, comma separated (e.g. '0,2'), or '*' for all channels. "
                    + "Channels missing in a slice are skipped.")
    String slice_channels_csv = "*";

    @Parameter(label = "Display min",
            description = "Pixel value displayed as black.")
    double display_min;

    @Parameter(label = "Display max",
            description = "Pixel value displayed at full brightness; higher values are saturated.")
    double display_max;

    @Override
    public void run() {

        List<SliceSources> slicesToModify = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slicesToModify.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to adjust");
            return;
        }

        if (!slice_channels_csv.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(slice_channels_csv.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
            slicesToModify.stream().forEach(slice -> {
                for (int iChannel:indices) {
                    if (iChannel<slice.nChannels) {
                        slice.setDisplayRange(iChannel, display_min, display_max);
                    }
                }
            });
        } else {
            slicesToModify.stream().forEach(slice -> {
                for (int iChannel=0;iChannel<slice.nChannels;iChannel++) {
                    slice.setDisplayRange(iChannel, display_min, display_max);
                }
            });
        }

    }

}