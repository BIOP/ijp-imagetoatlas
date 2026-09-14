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
        description = "For each selected slice imported from a QuPath project, saves its atlas regions and its registration "
                + "(ABBA-RoiSet and ABBA-Transform files) in the QuPath project folder of the image. "
                + "They are then imported in QuPath with the ABBA extension. Slices not imported from a QuPath project are not exported.",
        iconPath = "/graphics/ExportRegistrationToQuPath.png")
public class ExportRegistrationToQuPathCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label="Overwrite previous export",
            description = "If checked, a previous ABBA export of the same image is replaced; otherwise an error is reported for this image.")
    boolean erase_previous_file;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSelectedSlices();
        if (slices.isEmpty()) {
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
