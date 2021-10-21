package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.sourcepreprocessor.SourcesProcessor;
import ch.epfl.biop.atlas.aligner.sourcepreprocessor.SourcesProcessorHelper;
import ch.epfl.biop.atlas.aligner.plugin.RegistrationPluginHelper;

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

        if (registrationActionsCompiled.size() == 0) {
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
    public SliceSources getSliceSources() {
        return slice;
    }

    @Override
    public boolean run() {
        mp.removeUserAction(this);
        if (!reuseOriginalChannels) {
            slice.editLastRegistration(
                    SourcesProcessorHelper.compose(preprocessAtlas,
                    SourcesProcessorHelper.removeChannelsSelect(rs.preprocessFixed)),
                    SourcesProcessorHelper.compose(preprocessSlice,
                    SourcesProcessorHelper.removeChannelsSelect(rs.preprocessMoving)));
        } else {
            slice.editLastRegistration(rs.preprocessFixed, rs.preprocessMoving);
        }
        slice.sourcesChanged();
        return true;
    }

    @Override
    public boolean cancel() { // it cannot be canceled. maybe we could but I don't know
        return true;
    }

}
