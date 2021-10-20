package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.atlas.aligner.action.CancelableAction;
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
        return sliceSortedActions.get(slice);
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
    public void roiChanged() {

    }

    @Override
    public void actionEnqueue(SliceSources slice, CancelableAction action) {
        if (action.getSliceSources()!=null) {
            if (!sliceSortedActions.containsKey(action.getSliceSources())) {
                sliceSortedActions.put(action.getSliceSources(), new ArrayList<>());
            }
            sliceSortedActions.get(action.getSliceSources()).add(action);
        }
    }

    @Override
    public void actionStarted(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionFinished(SliceSources slice, CancelableAction action, boolean result) {

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
        }
    }
}
