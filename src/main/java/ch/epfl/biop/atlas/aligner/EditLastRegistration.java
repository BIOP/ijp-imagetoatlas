package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessorHelper;
import ch.epfl.biop.atlas.plugin.RegistrationPluginHelper;

import java.util.ArrayList;
import java.util.List;

public class EditLastRegistration extends CancelableAction {

    private final SliceSources slice;

    final RegisterSlice rs;

    private final boolean editWithAllChannels;

    public EditLastRegistration(MultiSlicePositioner mp, SliceSources slice, boolean editWithAllChannels) {
        super(mp);
        this.editWithAllChannels = editWithAllChannels;
        this.slice = slice;
        List<CancelableAction> registrationActionsCompiled = new ArrayList<>();
        // One need to get the list of still active registrations i.e.
        // All registrations minus the one already cancelled by a DeleteLastRegistration action
        for (CancelableAction action : mp.mso.getActionsFromSlice(slice)) {
            if (action instanceof RegisterSlice) {
                registrationActionsCompiled.add(action);
            }
            if (action instanceof DeleteLastRegistration) {
                registrationActionsCompiled.remove(registrationActionsCompiled.size()-1);
            }
        }

        if (registrationActionsCompiled.size() == 0) {
            rs = null;
        } else {
            rs = (RegisterSlice) registrationActionsCompiled.get(registrationActionsCompiled.size()-1);
        }

        mp.mso.hide(this);
    }

    public boolean isValid() {
        return ((rs!=null) && (RegistrationPluginHelper.isEditable(rs.registration)));
    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    @Override
    protected boolean run() {
        mp.userActions.remove(this);
        mp.mso.cancelInfo(this);
        if (editWithAllChannels) {
            slice.editLastRegistration(
                    SourcesProcessorHelper.removeChannelsSelect(rs.preprocessFixed),
                    SourcesProcessorHelper.removeChannelsSelect(rs.preprocessMoving));
        } else {
            slice.editLastRegistration(rs.preprocessFixed, rs.preprocessMoving);
        }
        return true;
    }

    @Override
    protected boolean cancel() { // it cannot be canceled. maybe we could but I don't know
        return false;
    }

}
