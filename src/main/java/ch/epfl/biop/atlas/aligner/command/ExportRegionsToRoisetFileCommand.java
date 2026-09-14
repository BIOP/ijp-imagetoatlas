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
        description = "Saves the atlas regions of each selected slice as an ImageJ ROI set zip file, named after the slice, "
                + "with ROIs in the pixel coordinates of the original (unregistered, full resolution) slice image.")
public class ExportRegionsToRoisetFileCommand extends DynamicCommand implements
        Initializable {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label="ROI naming",
            description = "Atlas ontology property used to name each region ROI, for instance its acronym, name or id. "
                    + "The available choices depend on the atlas.")
    String naming_choice; // Intellij claims it's not used. but it's wrong. It's use through scijava reflection

    @Parameter(label="Output folder", style = "directory",
            description = "Folder where one '<slice name>.zip' ROI set is written per selected slice.")
    File dir_output;

    @Parameter(label="Overwrite existing files",
            description = "If checked, existing ROI set files with the same name are replaced; otherwise these slices are skipped with a warning.")
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