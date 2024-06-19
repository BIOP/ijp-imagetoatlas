package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import ch.epfl.biop.registration.sourceandconverter.mirror.MirrorXRegistration;
import ch.epfl.biop.sourceandconverter.processor.SourcesIdentity;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Mirror Slices",
        description = "Mirror a half section to create the other side.")

public class MirrorDoCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;
    @Parameter(choices = {"Left", "Right"})
    String mirror_side;

    @Override
    public void run() {

        List<SliceSources> slicesToMirror = mp.getSelectedSlices();

        if (slicesToMirror.size() == 0) {
            mp.log.accept("No slice selected");
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to mirror.");
            return;
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("mirror_side", mirror_side);

        new MarkActionSequenceBatchAction(mp).runRequest();
        mp.registerSelectedSlices(MirrorXRegistration.class,
                new SourcesIdentity(),
                new SourcesIdentity(),
                parameters);
        new MarkActionSequenceBatchAction(mp).runRequest();
    }
}
