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
        logger.debug("UpdateInfoPanel called (No GUI)");
        sliceSortedActions.containsKey(slice);
        logger.debug("UpdateInfoPanel called (No GUI 0)");
        mp.getSlices().contains(slice);
        logger.debug("UpdateInfoPanel called (No GUI 1)");
        if (mp.getSlices().contains(slice)) {
            System.out.println(sliceSortedActions.get(slice).size() != 0);
            logger.debug("UpdateInfoPanel called (No GUI 2)");
        } else {
            logger.debug("UpdateInfoPanel called (No GUI 2a)");
        }

        if (sliceSortedActions.containsKey(slice)
                &&mp.getSlices().contains(slice)
                &&sliceSortedActions.get(slice).size()!=0) {

            logger.debug("UpdateInfoPanel called 1");
            String infoSlice = getTextSlice(slice);

            logger.debug("UpdateInfoPanel called 6");
            actionPerSlice.put(slice, infoSlice);

            logger.debug("UpdateInfoPanel called 7");

        } else {
            // Slice has been removed

            logger.debug("UpdateInfoPanel called 2");
            if (actionPerSlice.containsKey(slice)) {

                logger.debug("UpdateInfoPanel called 3");
                actionPerSlice.remove(slice);

                logger.debug("UpdateInfoPanel called 4");
                sliceSortedActions.remove(slice);

                logger.debug("UpdateInfoPanel called 5");
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
