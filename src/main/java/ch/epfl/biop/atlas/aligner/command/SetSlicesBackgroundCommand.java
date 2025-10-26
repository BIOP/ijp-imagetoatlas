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
        description = "Allow to work with white background images.")
public class SetSlicesBackgroundCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "White value (8-bit or rgb: 255, 16-bit:65535)")
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