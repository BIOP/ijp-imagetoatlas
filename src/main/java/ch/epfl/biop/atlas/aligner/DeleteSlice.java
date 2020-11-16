package ch.epfl.biop.atlas.aligner;

import java.util.Collections;
import java.util.List;

public class DeleteSlice extends CancelableAction {

    private final SliceSources sliceSource;

    List<CancelableAction> savedActions;

    public DeleteSlice(MultiSlicePositioner mp, SliceSources slice) {
        super(mp);
        this.sliceSource = slice;
        savedActions = mp.mso.getActionsFromSlice(slice);

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
            savedActions.remove(this);

            //System.out.println("Saved actions slice in run : " + sliceSource);
            /*savedActions.forEach(action ->  {
                System.out.println("\t"+action);
            });*/

            Collections.reverse(savedActions);
            savedActions.forEach(action ->  {
                if (action!=this) {
                    action.cancel();
                }
            });
            Collections.reverse(savedActions);
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
            return true;//cs.run();
        }
    }

}
