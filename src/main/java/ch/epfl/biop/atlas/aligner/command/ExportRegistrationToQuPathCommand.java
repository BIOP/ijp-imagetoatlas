package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.ExportSliceRegionsToQuPathProjectAction;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Registrations To QuPath Project",
        description = "Export atlas regions and transformations to QuPath project (for each selected slice)")
public class ExportRegistrationToQuPathCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Erase Previous ROIs")
    boolean erase_previous_file;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSelectedSlices();
        if (slices.size()==0) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice.");
        } else {
            new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice : slices) {
                new ExportSliceRegionsToQuPathProjectAction(mp, slice, erase_previous_file).runRequest();
            }
            new MarkActionSequenceBatchAction(mp).runRequest();
        }
    }

}
