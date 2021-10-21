package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;

import java.util.ArrayList;
import java.util.List;

public class DeleteLastRegistrationAction extends CancelableAction {

    private final SliceSources sliceSource;

    final RegisterSliceAction rs;

    public DeleteLastRegistrationAction(MultiSlicePositioner mp, SliceSources slice) {
        super(mp);
        this.sliceSource = slice;
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
        return rs != null;
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    @Override
    public boolean run() {
        rs.hide();
        return rs.cancel();
    }

    @Override
    public boolean cancel() {
        rs.show();
        return rs.run();
    }
}
