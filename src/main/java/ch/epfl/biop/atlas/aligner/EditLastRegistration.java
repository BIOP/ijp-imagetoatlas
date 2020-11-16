package ch.epfl.biop.atlas.aligner;

import java.util.ArrayList;
import java.util.List;

public class EditLastRegistration extends CancelableAction {

    private final SliceSources slice;

    final RegisterSlice rs;

    public EditLastRegistration(MultiSlicePositioner mp, SliceSources slice) {
        super(mp);
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
        return ((rs!=null) && (rs.registration.isEditable()));
    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    @Override
    protected boolean run() {
        mp.userActions.remove(this);
        mp.mso.cancelInfo(this);
        slice.editLastRegistration(rs.preprocessFixed, rs.preprocessMoving);
        return true;
    }

    @Override
    protected boolean cancel() { // it cannot be canceled. maybe we could but I don't know
        return false;
    }

}
