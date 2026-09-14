package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.ExportSliceRegionsToRoiManagerAction;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

// TODO : make this command atlas agnostic
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Regions To Roi Manager",
        description = "Adds the atlas regions of each selected slice to the ImageJ ROI Manager, as ROIs in the pixel coordinates "
                + "of the original (unregistered, full resolution) slice image.")
public class ExportRegionsToRoiManagerCommand extends DynamicCommand implements
        Initializable {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label="ROI naming",
            description = "Atlas ontology property used to name each region ROI, for instance its acronym, name or id. "
                    + "The available choices depend on the atlas.")
    String naming_choice; // Intellij claims it's not used. but it's wrong. It's use through scijava reflection

    @Override
    public void run() {

        List<SliceSources> slices = mp.getSelectedSlices();
        if (slices.isEmpty()) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice.");
        } else {
            new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice : slices) {
                new ExportSliceRegionsToRoiManagerAction(mp, slice, naming_choice).runRequest();
            }
            new MarkActionSequenceBatchAction(mp).runRequest();
        }
    }

    @Override
    public void initialize() {
        final MutableModuleItem<String> naming_choice = //
                getInfo().getMutableInput("naming_choice", String.class);
        List<String> names = new ArrayList<>(mp.getAtlas().getOntology().getRoot().data().keySet());
        naming_choice.setChoices(names);
    }

}
