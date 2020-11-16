package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DeleteSlice extends CancelableAction {

    private final SliceSources sliceSource;
    //private final List<SourceAndConverter<?>> sacs;

    List<CancelableAction> savedActions;

    public DeleteSlice(MultiSlicePositioner mp, SliceSources slice) {
        super(mp);
        this.sliceSource = slice;
        //this.sacs = Arrays.asList(slice.original_sacs);
        savedActions = mp.mso.getActionsFromSlice(slice);
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    @Override
    protected boolean run() {
        synchronized (DeleteSlice.class) { // avoid screw up with batch cancel ?
            savedActions.remove(this);
            Collections.reverse(savedActions);
            savedActions.forEach(action ->  action.cancel());
            Collections.reverse(savedActions);
            return true;
        }
    }

    @Override
    protected boolean cancel() {
        synchronized (DeleteSlice.class) { // Better ordering
            savedActions.forEach(action -> {
                action.run();
                mp.mso.sendInfo(action);
            });
            return true;//cs.run();
        }
    }

}
