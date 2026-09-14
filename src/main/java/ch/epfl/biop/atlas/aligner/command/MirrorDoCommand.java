package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import ch.epfl.biop.registration.source.mirror.MirrorXRegistration;
import ch.epfl.biop.source.processor.SourcesIdentity;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Mirror Slices",
        description = "Turns hemi-sections into full sections, for instance before DeepSlice which needs full sections. "
                + "The vertical midline of the atlas acts as a mirror: the half of each selected slice on the chosen side is kept, "
                + "and its mirror image replaces the other half. Place the hemi-section against the atlas midline first. "
                + "Undo with 'Un-Mirror Slices'.")

public class MirrorDoCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Half to keep (as displayed)", choices = {"Left", "Right"},
            description = "Side of the atlas midline, as displayed on screen, where the tissue is. "
                    + "'Left': the left half is kept and mirrored onto the right half. 'Right': the right half is kept and mirrored onto the left half.")
    String mirror_side;

    @Override
    public void run() {

        List<SliceSources> slicesToMirror = mp.getSelectedSlices();

        if (slicesToMirror.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to mirror.");
            return;
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("mirror_side", mirror_side);

        new MarkActionSequenceBatchAction(mp).runRequest();
        mp.registerSlices(slicesToMirror, MirrorXRegistration.class,
                new SourcesIdentity(),
                new SourcesIdentity(),
                parameters);
        new MarkActionSequenceBatchAction(mp).runRequest();
    }
}
