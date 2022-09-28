package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.DeleteLastRegistrationAction;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Remove Last Registration",
        description = "Remove the last registration of the current selected slices, if possible.")
public class RegisterSlicesRemoveLastCommand implements Command {

    protected static Logger logger = LoggerFactory.getLogger(RegisterSlicesRemoveLastCommand.class);

    @Parameter
    MultiSlicePositioner mp;

    @Override
    public void run() {
        logger.info("Remove last registration command called.");

        if (mp.getSelectedSlices().size()==0) {
            mp.log.accept("No slice selected");
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
