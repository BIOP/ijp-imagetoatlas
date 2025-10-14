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
        description = "Creates a new slice with reindexed channels.")
public class ReindexSlicesCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "New indices in csv", description = "For instance `0,2` to keep the first and third channels")
    String new_indices;

    public void run() {
        List<SliceSources> selectedSlices = mp.getSelectedSlices();

        if (selectedSlices.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to re-index");
            return;
        }
        List<Integer> indices;

        try {
            indices = Arrays.asList(new_indices.split(",")).stream().map(str -> new Integer(Integer.parseInt(str))).collect(Collectors.toList());
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
                for (int i = 0; i<indices.size(); i++) {
                    newSources.add(sliceSources.getOriginalSources()[indices.get(i)]);
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
