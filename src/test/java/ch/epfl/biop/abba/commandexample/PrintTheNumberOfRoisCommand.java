package ch.epfl.biop.abba.commandexample;

import ch.epfl.biop.abba.actionexample.PrintTheNumberOfRoisAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import ch.epfl.biop.atlas.aligner.plugin.ABBACommand;
import org.scijava.Initializable;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

@Plugin(type = ABBACommand.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>(Experimental)>ABBA - Count the number of ROIS exported",
        description = "Export atlas regions nowhere.")
public class PrintTheNumberOfRoisCommand extends DynamicCommand implements ABBACommand, Initializable {

    @Parameter
    MultiSlicePositioner mp; // Compulsory, DO NOT CHANGE THE VARIABLE NAME: "mp"

    @Parameter(label="Roi Naming")
    String naming_choice;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSelectedSlices();
        if (slices.isEmpty()) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice.");
        } else {
            new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice : slices) {
                new PrintTheNumberOfRoisAction(mp, slice, naming_choice).runRequest();
            }
            new MarkActionSequenceBatchAction(mp).runRequest();
        }
    }
    
    @Override
    public void initialize() {
        final MutableModuleItem<String> naming_choice = getInfo().getMutableInput("naming_choice", String.class);
        List<String> names = new ArrayList<>(mp.getAtlas().getOntology().getRoot().data().keySet());
        naming_choice.setChoices(names);
    }
}
