package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SetSliceBackgroundAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Set Slice Background",
        description = "For brightfield images with a white background: the area outside of the selected slices images "
                + "is filled with this value instead of black, which avoids dark edges during registrations and exports. "
                + "Works for 8-bit, 16-bit and RGB images.")
public class SetSlicesBackgroundCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Background value",
            description = "Pixel value used outside of the images: 255 for 8-bit or RGB images, 65535 for 16-bit images.")
    int white_background_value = 255;

    @Override
    public void run() {
        List<SliceSources> slicesToProcess = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slicesToProcess.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to operate on");
            return;
        }

        new MarkActionSequenceBatchAction(mp).runRequest();
        for (SliceSources slice : slicesToProcess) {
            SetSliceBackgroundAction setSliceBGAction = new SetSliceBackgroundAction(mp, slice, white_background_value);
            setSliceBGAction.runRequest();
        }
        new MarkActionSequenceBatchAction(mp).runRequest();
    }

}