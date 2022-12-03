package ch.epfl.biop.atlas.aligner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SliceActionObserver implements MultiSlicePositioner.SliceChangeListener {

    protected static Logger logger = LoggerFactory.getLogger(SliceActionObserver.class);

    MultiSlicePositioner mp;

    Map<SliceSources, List<CancelableAction>> sliceSortedActions = new ConcurrentHashMap<>();

    Map<SliceSources, String> actionPerSlice = new ConcurrentHashMap<>();

    Set<CancelableAction> hiddenActions = ConcurrentHashMap.newKeySet(); // For cancelled actions

    public void clear() {
        this.mp = null;
        sliceSortedActions.clear();
        sliceSortedActions = null;
        actionPerSlice.clear();
        actionPerSlice = null;
        hiddenActions.clear();
        hiddenActions = null;
    }

    public SliceActionObserver(MultiSlicePositioner mp) {
        this.mp = mp;
    }

    public void hide(CancelableAction action) {
        hiddenActions.add(action);
    }

    public void unhide(CancelableAction action) {
        hiddenActions.remove(action);
    }

    public synchronized List<CancelableAction> getActionsFromSlice(SliceSources slice) {
        return sliceSortedActions.get(slice); // TODO : return a copy instead ?
    }

    @Override
    public void sliceDeleted(SliceSources slice) {

    }

    @Override
    public void sliceCreated(SliceSources slice) {

    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {

    }

    @Override
    public void sliceSelected(SliceSources slice) {

    }

    @Override
    public void sliceDeselected(SliceSources slice) {

    }

    @Override
    public void sliceSourcesChanged(SliceSources slice) {

    }

    @Override
    public void slicePretransformChanged(SliceSources slice) {

    }

    @Override
    public void sliceKeyOn(SliceSources slice) {

    }

    @Override
    public void sliceKeyOff(SliceSources slice) {

    }

    @Override
    public void roiChanged() {

    }

    @Override
    public void actionEnqueue(SliceSources slice, CancelableAction action) {
        if (action.getSliceSources()!=null) {
            if (!sliceSortedActions.containsKey(action.getSliceSources())) {
                sliceSortedActions.put(action.getSliceSources(), new ArrayList<>());
            }
            sliceSortedActions.get(action.getSliceSources()).add(action);
            logger.debug("Action registered in observer: "+action);
        } else {
            assert action instanceof CreateSliceAction;
        }
    }

    @Override
    public void actionStarted(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionFinished(SliceSources slice, CancelableAction action, boolean result) {
        assert action.getSliceSources()!=null;
        // Specific case : the action has no reference slice until it is actually created
        if (action instanceof CreateSliceAction) {
            if (!sliceSortedActions.containsKey(action.getSliceSources())) {
                sliceSortedActions.put(action.getSliceSources(), new ArrayList<>());
            }
            logger.debug("Size of sliceSortedAction (normally 0) "+sliceSortedActions.get(action.getSliceSources()).size());
            if (!sliceSortedActions.get(action.getSliceSources()).contains(action)) {
                sliceSortedActions.get(action.getSliceSources()).add(action);
            } else {
                logger.debug("Ah, create slice was already contained, it's a redo!");
            }
            logger.debug("Action registered in observer: "+action);
        }
        if (!action.isValid()) {
            sliceSortedActions.get(action.getSliceSources()).remove(action); // Invalid = to remove ? TODO : check
        }
        if (action instanceof DeleteSliceAction) {
            sliceSortedActions.get(action.getSliceSources()).remove(action); // Causes issues otherwise
        }
    }

    @Override
    public void actionCancelEnqueue(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionCancelStarted(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionCancelFinished(SliceSources slice, CancelableAction action, boolean result) {
        if ((action.getSliceSources()!=null)&&(sliceSortedActions.get(action.getSliceSources())!=null)) {
            sliceSortedActions.get(action.getSliceSources()).remove(action);
            logger.debug("Action removed in observer: "+action);
        } else {
            logger.debug("Action not removed: "+action);
        }
    }

    @Override
    public void converterChanged(SliceSources slice) {

    }
}
