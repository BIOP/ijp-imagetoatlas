package ch.epfl.biop.atlas.aligner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Backbone of a cancelable action used for Bdv Slice Positioner
 * This is used in order to:
 * - store the sequence of actions (- for cancellation)
 * - executes actions asynchronously (- for long computations)
 * - keep track of which action is executed on which SliceSources
 * - serialization
 * Convention : an action acts on a single SliceSources object.
 * If multiple SliceSources should be acted on, create as many actions as SliceSources involved
 * (and mark the sequence with @see MarkActionSequenceBatch)
 *
 */

public abstract class CancelableAction {

    final MultiSlicePositioner mp;

    protected static Logger logger = LoggerFactory.getLogger(CancelableAction.class);

    /**
     * Provides the reference to the MultiSlicePositioner
     * @param mp multipositioner input
     */
    public CancelableAction(MultiSlicePositioner mp) {
        this.mp = mp;
    }

    public MultiSlicePositioner getMP() {
        return mp;
    }

    /**
     *
     * @return the SliceSources involved in this action
     */
    abstract public SliceSources getSliceSources();

    final public void runRequest() {
        //mp.runRequest(this);
        runRequest(false);
    }

    final public void runRequest(boolean launchInExtraThread) {
        mp.runRequest(this, launchInExtraThread);
    }

    final public void cancelRequest() {
        mp.cancelRequest(this);
    }

    protected abstract boolean run();

    protected abstract boolean cancel();

    private boolean isHidden = false;

    public void hide() {
        isHidden = true;
    }

    public void show() {
        isHidden = false;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public boolean isValid() {
        return true;
    }

    public String actionClassString() {
        return this.getClass().getSimpleName();
    }

    abstract public void drawAction(Graphics2D g, double px, double py, double scale);

}