package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.ExportSliceRegionsToFileAction;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// TODO: make this command atlas agnostic
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Regions To File",
        description = "Export the transformed atlas regions of currently selected slices as ImageJ roi zip files.")
public class ExportRegionsToRoisetFileCommand extends DynamicCommand implements
        Initializable {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Roi Naming")
    String naming_choice; // Intellij claims it's not used. but it's wrong. It's use through scijava reflection

    @Parameter(label="Directory for ROI Saving", style = "directory")
    File dir_output;

    @Parameter(label="Erase Previous ROIs")
    boolean erase_previous_file;

    @Override
    public void run() {
        // Cannot be accessed
        //mp.exportSelectedSlicesRegionsToFile(naming_choice, dir_output, erase_previous_file);

        List<SliceSources> slices = mp.getSelectedSlices();
        if (slices.isEmpty()) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice.");
        } else {
            new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice : slices) {
                new ExportSliceRegionsToFileAction(mp, slice, naming_choice, dir_output, erase_previous_file).runRequest();
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