package ch.epfl.biop.atlas.aligner;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class DeleteSlice extends CancelableAction {

    private final SliceSources sliceSource;

    List<CancelableAction> savedActions;

    public DeleteSlice(MultiSlicePositioner mp, SliceSources slice) {
        super(mp);
        this.sliceSource = slice;
        //savedActions = mp.mso.getActionsFromSlice(slice);

        /*synchronized (DeleteSlice.class) { // avoid screw up with batch cancel ?
            /*System.out.println("Saved actions slice : " + sliceSource);
            savedActions.forEach(action ->  {
                System.out.println("\t"+action);
            });*/
        //}
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    @Override
    protected boolean run() {
        synchronized (DeleteSlice.class) { // avoid screw up with batch cancel ?
            savedActions = mp.mso.getActionsFromSlice(sliceSource);
            savedActions.remove(this);savedActions.remove(this);savedActions.remove(this);
            System.out.println(sliceSource);
            //System.out.println("Saved actions slice in run : " + sliceSource);

            //synchronized (savedActions) {
                Collections.reverse(savedActions);
                savedActions.forEach(action -> {
                    if (action != this) {
                        action.cancel();
                    }
                });
                Collections.reverse(savedActions);
            //}
            return true;
        }
    }

    @Override
    protected boolean cancel() {
        synchronized (DeleteSlice.class) { // Better ordering
            savedActions.forEach(action -> {
                if (action!=this) {
                    action.run();
                    mp.mso.sendInfo(action);
                }
            });
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
