package ch.epfl.biop.atlas.aligner;

import java.util.ArrayList;
import java.util.List;

public class DeleteLastRegistration extends CancelableAction {

    private final SliceSources sliceSource;

    final RegisterSlice rs;

    public DeleteLastRegistration(MultiSlicePositioner mp, SliceSources slice) {
        super(mp);
        this.sliceSource = slice;
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
        return rs != null;
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    @Override
    protected boolean run() {
        mp.mso.hide(rs);
        return rs.cancel();
    }

    @Override
    protected boolean cancel() {
        mp.mso.unhide(rs);
        return rs.run();
    }
}
