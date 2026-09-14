package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.CreateSliceAction;
import ch.epfl.biop.atlas.aligner.DeleteSliceAction;
import ch.epfl.biop.atlas.aligner.LockAndRunOnceSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Re-index channels of slices",
        description = "Reorders, removes or duplicates the channels of the selected slices. Each selected slice is replaced by a new slice "
                + "with the new channels, at the same position and with the same registrations. "
                + "The same channel order is applied to all selected slices.")
public class ReindexSlicesCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "New channel order",
            description = "Comma separated 0-based indices of the original channels, in their new order: "
                    + "'0,2' keeps the first and third channels, '1,0' swaps the first two channels.")
    String new_channel_order_csv;

    public void run() {
        List<SliceSources> selectedSlices = mp.getSelectedSlices();

        if (selectedSlices.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to re-index");
            return;
        }
        List<Integer> indices;

        try {
            indices = Arrays.stream(new_channel_order_csv.split(",")).map(Integer::parseInt).collect(Collectors.toList());
        } catch (NumberFormatException e) {
            mp.errorMessageForUser.accept("Invalid channels indices", "Could not parse indices");
            return;
        }

        if (indices.isEmpty()) {
            mp.errorMessageForUser.accept("Issue with re-indexing", "You need to use at least one channel.");
            return;
        }

        // Check that we are within bounds
        int maxChannels = mp.getSelectedSlices().stream().mapToInt(slice -> slice.nChannels).min().getAsInt()-1;
        int maxChannelsUser = indices.stream().reduce(Integer::max).get();
        if (maxChannelsUser>maxChannels) {
            mp.errorMessageForUser.accept("Issue with re-indexing",
                    "An index ["+(maxChannelsUser)+"] is above the maximal index allowed within the selected slices ["+maxChannels+"]");
            return;
        }

        // We duplicate the slice and remove the old one
        mp.addTask();
        new MarkActionSequenceBatchAction(mp).runRequest();
        try {
        for (SliceSources sliceSources: selectedSlices) {
            AtomicBoolean result = new AtomicBoolean();
            new LockAndRunOnceSliceAction(mp, sliceSources, new AtomicInteger(0), 1, () -> {
                List<SourceAndConverter<?>> newSources = new ArrayList<>();
                for (Integer index : indices) {
                    newSources.add(sliceSources.getOriginalSources()[index]);
                }
                CreateSliceAction cs = new CreateSliceAction(mp, newSources,sliceSources.getSlicingAxisPosition(), sliceSources.getZThicknessCorrection(), sliceSources.getZShiftCorrection());
                cs.runRequest();
                RegisterSlicesCopyAndApplyCommand.copyRegistration(mp, sliceSources, mp, cs.getSlice(), false);
                new DeleteSliceAction(mp, sliceSources).runRequest();
                return true;
            }, result).runRequest();
        }} finally {
            new MarkActionSequenceBatchAction(mp).runRequest();
            mp.removeTask();
        }
    }


}
