package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.DeleteLastRegistrationAction;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Remove Last Registration",
        description = "Removes the last registration of each selected slice (it can be undone).")
public class RegisterSlicesRemoveLastCommand implements Command {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Removes one registration per selected slice, run it again to remove more. A mirroring counts as a registration. Slice positions are not changed.";

    protected final static Logger logger = LoggerFactory.getLogger(RegisterSlicesRemoveLastCommand.class);

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Override
    public void run() {
        logger.info("Remove last registration command called.");

        if (mp.getSelectedSlices().isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
            return;
        }

        new MarkActionSequenceBatchAction(mp).runRequest();
        for (SliceSources slice : mp.getSelectedSlices()) {
            if (slice.isSelected()) {
                new DeleteLastRegistrationAction(mp, slice).runRequest();
            }
        }
        new MarkActionSequenceBatchAction(mp).runRequest();
    }
}
