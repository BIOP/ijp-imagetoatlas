package ch.epfl.biop.atlastoimg2d.multislice;

import java.awt.*;
import java.util.function.Consumer;

/**
 * Backbone of a cancelable action used for Bdv Slice Positioner
 * This is used in order to:
 * - store the sequence of actions (-> for cancellation)
 * - executes actions asynchronously (-> for long computations)
 * - keep track of which action is executed on which SliceSources
 *
 * Convention : an action acts on a single SliceSources object.
 * If multiple SliceSources should be acted on, create as many actions as SliceSources involved
 * (and mark the sequence with @see MarkActionSequenceBatch)
 *
 */

public abstract class CancelableAction {

    final MultiSlicePositioner mp;

    public static Consumer<String> errlog = (str) ->  System.err.println(str);

    /**
     * Provides the reference to the MultiSlicePositioner
     * @param mp
     */
    public CancelableAction(MultiSlicePositioner mp) {
        this.mp = mp;
    }

    /**
     * Returns the SliceSources involved in this action
     * @return
     */
    abstract public SliceSources getSliceSources();

    final public void runRequest() {
        if ((getSliceSources()!=null)) {
            getSliceSources().enqueueRunAction(this, () -> {
                mp.mso.updateInfoPanel(getSliceSources());
            } );
        } else {
            // Not asynchronous
            run();
        }
        if (isValid()) {

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
    }

    final public void cancelRequest() {
        if (isValid()) {
            if ((getSliceSources() == null)) {
                // Not asynchronous
                System.out.println("Non Async cancel call : " + this.toString());
                cancel();
            } else {
                System.out.println("Async cancel call : " + this.toString());
                getSliceSources().enqueueCancelAction(this, () -> {
                });
            }
            if (mp.userActions.get(mp.userActions.size() - 1).equals(this)) {
                mp.userActions.remove(mp.userActions.size() - 1);
                mp.redoableUserActions.add(this);
            } else {
                errlog.accept("Error : cancel not called on the last action");
                return;
            }
            mp.mso.cancelInfo(this);
        }
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

    protected boolean isValid() {
        return true;
    }

}