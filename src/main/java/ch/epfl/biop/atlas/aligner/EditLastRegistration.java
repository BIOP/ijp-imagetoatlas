package ch.epfl.biop.atlas.aligner;

import java.awt.*;
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
        return true; //rs.registration.isEditable(); //rs != null;
    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    @Override
    protected boolean run() {
        slice.editLastRegistration(rs.preprocessFixed, rs.preprocessMoving);
        return true;
    }

    @Override
    protected boolean cancel() { // it cannot be canceled. maybe we could but I don't know
        return false;
    }

    /*public void drawAction(Graphics2D g, double px, double py, double scale) {
        switch (slice.getActionState(this)){//.getRegistrationState(registration)) {
            case "(done)":
                g.setColor(new Color(0, 255, 0, 200));
                break;
            case "(locked)":
                g.setColor(new Color(255, 0, 0, 200));
                break;
            case "(pending)":
                g.setColor(new Color(255, 255, 0, 200));
                break;
        }
        g.fillOval((int) (px - 7), (int) (py - 7), 14, 14);
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("R", (int) px - 4, (int) py + 5);
    }*/

}
