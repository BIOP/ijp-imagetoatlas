package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Set Slices Min Max Display Range",
        description = "Change min max displayed value (for each selected slice).")
public class SliceMinMaxDisplaySetCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Channels to adjust, '*' for all channels, comma separated, 0-based")//choices = {"Structural Images", "Border only", "Coordinates", "Left / Right", "Labels % 65000" })
    String atlasStringChannel = "*";//String export_type;

    @Parameter(label = "Min displayed valued")
    double display_min;

    @Parameter(label = "Max displayed valued")
    double display_max;

    @Override
    public void run() {

        List<SliceSources> slicesToModify = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slicesToModify.size()==0) {
            mp.log.accept("No slice selected");
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to adjust");
            return;
        }

        if (!atlasStringChannel.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(atlasStringChannel.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
            slicesToModify.stream().forEach(slice -> {
                for (int iChannel:indices) {
                    slice.setDisplayRange(iChannel, display_min, display_max);
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