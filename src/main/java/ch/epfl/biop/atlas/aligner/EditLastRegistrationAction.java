package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.registration.plugin.RegistrationPluginHelper;
import ch.epfl.biop.source.processor.SourcesIdentity;
import ch.epfl.biop.source.processor.SourcesProcessComposer;
import ch.epfl.biop.source.processor.SourcesProcessor;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EditLastRegistrationAction extends CancelableAction {

    private final SliceSources slice;

    final RegisterSliceAction rs;

    private final boolean reuseOriginalChannels;

    private final SourcesProcessor preprocessSlice, preprocessAtlas;

    public EditLastRegistrationAction(MultiSlicePositioner mp, SliceSources slice, boolean reuseOriginalChannels, SourcesProcessor preprocessSlice, SourcesProcessor preprocessAtlas) {
        super(mp);
        this.reuseOriginalChannels = reuseOriginalChannels;
        this.slice = slice;
        this.preprocessAtlas = preprocessAtlas;
        this.preprocessSlice = preprocessSlice;
        List<CancelableAction> registrationActionsCompiled = new ArrayList<>();
        // One need to get the list of still active registrations i.e.
        // All registrations minus the one already cancelled by a DeleteLastRegistration action
        for (CancelableAction action : mp.getActionsFromSlice(slice)) {
            if (action instanceof RegisterSliceAction) {
                registrationActionsCompiled.add(action);
            }
            if (action instanceof DeleteLastRegistrationAction) {
                registrationActionsCompiled.remove(registrationActionsCompiled.size()-1);
            }
        }

        if (registrationActionsCompiled.isEmpty()) {
            rs = null;
        } else {
            rs = (RegisterSliceAction) registrationActionsCompiled.get(registrationActionsCompiled.size()-1);
        }
        hide();
    }

    public boolean isValid() {
        return ((rs!=null) && (RegistrationPluginHelper.isEditable(rs.registration)));
    }

    @Override
    public void drawAction(Graphics2D g, double px, double py, double scale) {

    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    // Transforms of the registration before and after the edition: undo and redo set them back without the editor
    private String transformBefore, transformAfter;

    @Override
    protected boolean run() {
        if (transformAfter != null) return setTransform(transformAfter); // redo
        transformBefore = rs.registration.getTransform();
        //mp.removeUserAction(this);
        // We may have changed the z location, so we need to remove the z zero location and put it back at the new location
        //SourcesProcessor aProcessor = SourcesProcessorHelper.removeChannelsSelect(rs.preprocessFixed)

        if (!reuseOriginalChannels) {
            // The z location may have changed between the registration and the edition
            // That's why I remove the previous z offset location and put back the new location
            SourcesProcessor pA = SourcesProcessorHelper.compose(
                    removeSourcesZOffset(rs.preprocessFixed),
                    new SourcesZOffset(slice));
            SourcesProcessor pS = SourcesProcessorHelper.compose(
                    removeSourcesZOffset(rs.preprocessMoving),
                    new SourcesZOffset(slice));

            slice.editLastRegistration(
                    SourcesProcessorHelper.compose(preprocessAtlas,
                            SourcesProcessorHelper.removeChannelsSelect(pA)),
                    SourcesProcessorHelper.compose(preprocessSlice,
                            SourcesProcessorHelper.removeChannelsSelect(pS)));
        } else {
            slice.editLastRegistration(rs.preprocessFixed, rs.preprocessMoving);
        }
        transformAfter = rs.registration.getTransform();
        slice.sourcesChanged();
        getMP().stateHasBeenChanged();
        return true;
    }

    @Override
    protected boolean cancel() {
        return (transformBefore == null) || setTransform(transformBefore);
    }

    private boolean setTransform(String transform) {
        boolean result = slice.setLastRegistrationTransform(rs.registration, transform);
        slice.sourcesChanged();
        getMP().stateHasBeenChanged();
        return result;
    }

    private static SourcesProcessor removeSourcesZOffset(SourcesProcessor processor) {
        if (processor instanceof SourcesZOffset) {
            return new SourcesIdentity();
        } else if (processor instanceof SourcesProcessComposer) {
            SourcesProcessComposer composer_in = (SourcesProcessComposer)processor;
            return new SourcesProcessComposer(removeSourcesZOffset(composer_in.f2), removeSourcesZOffset(composer_in.f1));
        } else {
            return processor;
        }
    }

}
