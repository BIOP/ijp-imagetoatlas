package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.UnMirrorSliceAction;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Un-Mirror Slices",
        description = "Removes the mirroring of the selected slices, made with 'Mirror Slices', while keeping the registrations done afterwards.")

public class MirrorUndoCommand implements Command {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Removes the last visible mirroring of each selected slice. A slice without mirroring gets an invalid (X) action in its timeline.";

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSelectedSlices();
        if (slices.isEmpty()) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice.");
        } else {
            new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice : slices) {
                new UnMirrorSliceAction(mp, slice).runRequest();
            }
            new MarkActionSequenceBatchAction(mp).runRequest();
        }

    }
}
