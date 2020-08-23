package ch.epfl.biop.atlastoimg2d.multislice;

import java.awt.*;

/**
 * Backbone of a cancelable action
 */
public abstract class CancelableAction {

    final MultiSlicePositioner mp;

    public CancelableAction(MultiSlicePositioner mp) {
        this.mp = mp;
    }

    abstract public SliceSources getSliceSources();

    final public void runRequest() {
        if (getSliceSources()!=null) {
            getSliceSources().enqueueRunAction(this);
        } else {
            // Not asynchronous
            run();
        }
        mp.userActions.add(this);
        if (mp.redoableUserActions.size() > 0) {
            if (mp.redoableUserActions.get(mp.redoableUserActions.size() - 1).equals(this)) {
                mp.redoableUserActions.remove(mp.redoableUserActions.size() - 1);
            } else {
                // different branch : clear redoable actions
                mp.redoableUserActions.clear();
            }
        }
        mp.mso.sendInfo(this);
    }

    final public void cancelRequest() {
        if (getSliceSources()!=null) {
            // Not asynchronous
            getSliceSources().enqueueCancelAction(this);
        } else {
            cancel();
        }
        if (mp.userActions.get(mp.userActions.size() - 1).equals(this)) {
            mp.userActions.remove(mp.userActions.size() - 1);
            mp.redoableUserActions.add(this);
        } else {
            System.err.println("Error : cancel not called on the last action");
            return;
        }
        mp.mso.cancelInfo(this);
    }

    abstract protected boolean run();

    abstract protected boolean cancel();

    public void draw(Graphics2D g, double px, double py, double scale) {
        if (mp.drawActions) {
            drawAction(g, px, py, scale);
        }
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString(toString(), (int) px, (int) py);
    }

}