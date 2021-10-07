package ch.epfl.biop.atlas.aligner;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MultiSliceObserverNoGUI extends MultiSliceObserver{

    public MultiSliceObserverNoGUI(MultiSlicePositioner mp) {
        super(mp);
    }

    public void draw(Graphics2D g) {
    }

    public void end() {
    }

    public JComponent getJPanel() {
        return null;
    }

    public synchronized void updateInfoPanel(SliceSources slice) {
        if (sliceSortedActions.containsKey(slice)
                &&mp.getSlices().contains(slice)
                &&sliceSortedActions.get(slice).size()!=0) {
            String infoSlice = getTextSlice(slice);
            actionPerSlice.put(slice, infoSlice);
        } else {
            // Slice has been removed
            if (actionPerSlice.containsKey(slice)) {
                actionPerSlice.remove(slice);
                sliceSortedActions.remove(slice);
            }
        }
        logger.debug("UpdateInfoPanel ended (No GUI)");
    }

    public synchronized void sendInfo(CancelableAction action) {
        if (action.getSliceSources()!=null) {
            if (!sliceSortedActions.containsKey(action.getSliceSources())) {
                sliceSortedActions.put(action.getSliceSources(), new ArrayList<>());
            }
            sliceSortedActions.get(action.getSliceSources()).add(action);
            updateInfoPanel(action.getSliceSources());
        }
    }

    public synchronized List<CancelableAction> getActionsFromSlice(SliceSources slice) {
        return sliceSortedActions.get(slice);
    }

    public synchronized void cancelInfo(CancelableAction action) {
        if ((action.getSliceSources()!=null)&&(sliceSortedActions.get(action.getSliceSources())!=null)) {
            sliceSortedActions.get(action.getSliceSources()).remove(action);
            updateInfoPanel(action.getSliceSources());
        }
    }

}
