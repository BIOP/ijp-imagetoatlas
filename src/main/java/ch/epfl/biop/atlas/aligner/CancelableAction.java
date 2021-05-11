package ch.epfl.biop.atlas.aligner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.function.Consumer;

/**
 * Backbone of a cancelable action used for Bdv Slice Positioner
 * This is used in order to:
 * - store the sequence of actions (- for cancellation)
 * - executes actions asynchronously (- for long computations)
 * - keep track of which action is executed on which SliceSources
 * - serialization
 *
 * Convention : an action acts on a single SliceSources object.
 * If multiple SliceSources should be acted on, create as many actions as SliceSources involved
 * (and mark the sequence with @see MarkActionSequenceBatch)
 *
 */

public abstract class CancelableAction {

    final MultiSlicePositioner mp;

    private static Logger logger = LoggerFactory.getLogger(CancelableAction.class);

    public static Consumer<String> errlog = logger::error;

    /**
     * Provides the reference to the MultiSlicePositioner
     * @param mp multipositioner input
     */
    public CancelableAction(MultiSlicePositioner mp) {
        this.mp = mp;
    }

    /**
     *
     * @return the SliceSources involved in this action
     */
    abstract public SliceSources getSliceSources();

    final public void runRequest() {
        if ((getSliceSources()!=null)) {
            logger.debug("Action "+this +" on slice "+getSliceSources()+" requested (async).");
            getSliceSources().enqueueRunAction(this, () -> mp.mso.updateInfoPanel(getSliceSources()) );
        } else {
            // Not asynchronous
            logger.debug("Action "+this +" on slice "+getSliceSources()+" run (non async).");
            run();
            logger.debug("Action "+this +" on slice "+getSliceSources()+" done.");
        }
        if (isValid()) {
            logger.debug("Action "+this+" on slice "+getSliceSources()+" is valid.");
            mp.userActions.add(this);
            logger.debug("Action "+this+" on slice "+getSliceSources()+" added to userActions.");
            if (mp.redoableUserActions.size() > 0) {
                if (mp.redoableUserActions.get(mp.redoableUserActions.size() - 1).equals(this)) {
                    mp.redoableUserActions.remove(mp.redoableUserActions.size() - 1);
                } else {
                    // different branch : clear redoable actions
                    mp.redoableUserActions.clear();
                }
            }
            logger.debug("Action "+this+" on slice "+getSliceSources()+" info sending to MultiSliceObserver.");
            mp.mso.sendInfo(this);
            logger.debug("Action "+this+" on slice "+getSliceSources()+" info sent to MultiSliceObserver!");
        } else {
            logger.error("Invalid action "+this+" on slice "+getSliceSources());
        }
    }

    final public void cancelRequest() {
        if (isValid()) {
            if ((getSliceSources() == null)) {
                // Not asynchronous
                logger.debug("Non Async cancel call : " + this + " on slice "+getSliceSources());
                cancel();
            } else {
                logger.debug("Async cancel call : " + this + " on slice "+getSliceSources());
                getSliceSources().enqueueCancelAction(this, () -> { });
            }
            if (mp.userActions.get(mp.userActions.size() - 1).equals(this)) {

                logger.debug(this.toString() + " cancelled on slice "+getSliceSources()+", updating useractions and redoable actions");
                mp.userActions.remove(mp.userActions.size() - 1);
                mp.redoableUserActions.add(this);
            } else {
                logger.error("Error : cancel not called on the last action");
                return;
            }
            logger.debug("Updating mso after action cancelled");
            mp.mso.cancelInfo(this);
        }
    }

    abstract protected boolean run();

    abstract protected boolean cancel();

    public boolean draw() {
        return true;
    }

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

    public String actionClassString() {
        return this.getClass().getSimpleName();
    }

}