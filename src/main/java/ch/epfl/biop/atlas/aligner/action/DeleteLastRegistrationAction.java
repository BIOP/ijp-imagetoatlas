package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DeleteLastRegistrationAction extends CancelableAction {

    private final SliceSources sliceSource;

    final RegisterSliceAction rs;

    public DeleteLastRegistrationAction(MultiSlicePositioner mp, SliceSources slice) {
        super(mp);
        this.sliceSource = slice;
        // TODO : check whether this should be in the run method instead of in the constructor
        List<CancelableAction> registrationActionsCompiled = new ArrayList<>();
        // One need to get the list of still active registrations i.e.
        // All registrations minus the one already cancelled by a DeleteLastRegistration action
        for (CancelableAction action : mp.getActionsFromSlice(slice)) {
            if (action instanceof RegisterSliceAction) {
                registrationActionsCompiled.add(action);
            }
            if (action instanceof DeleteLastRegistrationAction) {
                if (registrationActionsCompiled.size()-1!=-1) {
                    registrationActionsCompiled.remove(registrationActionsCompiled.size() - 1);
                }
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
    public void drawAction(Graphics2D g, double px, double py, double scale) {

    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    @Override
    public boolean run() {
        if (rs!=null){
            mp.addTask();
            rs.hide();
            boolean result = rs.cancel();
            mp.stateHasBeenChanged();
            mp.removeTask();
            return result;
        } else {
            return false;
        }
    }

    @Override
    public boolean cancel() {
        if (rs!=null) {
            mp.addTask();
            rs.show();
            boolean result = rs.run();
            mp.stateHasBeenChanged();
            mp.removeTask();
            return result;
        } else {
            return true;
        }
    }
}
