package ch.epfl.biop.atlas.aligner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class DeleteSliceAction extends CancelableAction {

    protected static final Logger logger = LoggerFactory.getLogger(DeleteSliceAction.class);

    private final SliceSources sliceSource;

    List<CancelableAction> savedActions;

    public DeleteSliceAction(MultiSlicePositioner mp, SliceSources slice) {
        super(mp);
        this.sliceSource = slice;
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    @Override
    public boolean run() {
        synchronized (DeleteSliceAction.class) { // avoid screw up with batch cancel ?
            sliceSource.setBeingDeleted(true);
            getMP().addTask();
            logger.debug("Deleting slice "+getSliceSources()+" ...");
            savedActions = getMP().getActionsFromSlice(sliceSource);
            savedActions.remove(this);
            logger.debug("Saved actions slice in run : " + sliceSource);

            Collections.reverse(savedActions);
            savedActions.forEach(action -> {
                if (action != this) {
                    action.cancel();
                }
            });
            Collections.reverse(savedActions);

            logger.debug("Slice "+getSliceSources()+" deleted !");
            getMP().stateHasBeenChanged();
            getMP().removeTask();
            sliceSource.setBeingDeleted(false);
            return true;
        }
    }

    @Override
    public boolean cancel() {
        synchronized (DeleteSliceAction.class) { // Better ordering
            logger.debug("Cancelling delete slice for slice "+getSliceSources());
            savedActions.forEach(action -> {
                if (action!=this) {
                    action.run();
                }
            });
            getMP().stateHasBeenChanged();
            return true;
        }
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        switch (sliceSource.getActionState(this)){
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
        g.drawString("Delete", (int) px - 4, (int) py + 5);
    }

}
