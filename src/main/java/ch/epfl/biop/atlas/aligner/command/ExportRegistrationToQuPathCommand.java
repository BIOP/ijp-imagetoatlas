package ch.epfl.biop.atlas.aligner.command;

import bdv.util.QuPathBdvHelper;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.ExportSliceRegionsToQuPathProjectAction;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Registrations To QuPath Project",
        description = "For each selected slice imported from a QuPath project, saves its atlas regions and its registration "
                + "(ABBA-RoiSet and ABBA-Transform files) in the QuPath project folder of the image. "
                + "They are then imported in QuPath with the ABBA extension. Slices not imported from a QuPath project are not exported: "
                + "slices opened from files can be exported with 'ABBA - Export Slices To New QuPath Project'.",
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
            return;
        }

        // Reported once for all slices rather than in one blocking message per slice
        List<SliceSources> linkedSlices = new ArrayList<>();
        List<String> notLinked = new ArrayList<>();
        for (SliceSources slice : slices) {
            if (Arrays.stream(slice.getOriginalSources()).anyMatch(QuPathBdvHelper::isSourceLinkedToQuPath)) {
                linkedSlices.add(slice);
            } else {
                notLinked.add(slice.toString());
            }
        }

        if (!notLinked.isEmpty()) {
            String message = notLinked.size()+" selected slice(s) were not imported from a QuPath project and are not exported: "
                    + String.join(", ", notLinked)+".\n"+SliceSources.NOT_LINKED_TO_QUPATH_HINT;
            if (linkedSlices.isEmpty()) {
                mp.errorMessageForUser.accept("No slice linked to a QuPath project", message);
                return;
            }
            mp.warningMessageForUser.accept("Slices not linked to a QuPath project", message);
        }

        new MarkActionSequenceBatchAction(mp).runRequest();
        for (SliceSources slice : linkedSlices) {
            new ExportSliceRegionsToQuPathProjectAction(mp, slice, erase_previous_file).runRequest();
        }
        new MarkActionSequenceBatchAction(mp).runRequest();
    }

}
