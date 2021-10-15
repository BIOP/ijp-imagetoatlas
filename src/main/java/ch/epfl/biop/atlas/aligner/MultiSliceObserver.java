package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.atlas.aligner.action.CancelableAction;
import ch.epfl.biop.atlas.aligner.action.MoveSliceAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MultiSliceObserver {

    protected static Logger logger = LoggerFactory.getLogger(MultiSliceObserver.class);

    MultiSlicePositioner mp;

    Map<SliceSources, List<CancelableAction>> sliceSortedActions = new ConcurrentHashMap<>();

    Map<SliceSources, String> actionPerSlice = new ConcurrentHashMap<>();

    Map<SliceSources, JTextArea> textAreaPerSlice = new ConcurrentHashMap<>();

    Set<CancelableAction> hiddenActions = ConcurrentHashMap.newKeySet(); // For cancelled actions

    JPanel innerPanel = new JPanel();

    Thread animatorThread;

    volatile boolean animate = true;

    boolean repaintNeeded = false;

    public void clear() {
        animate = false;
        this.mp = null;
        sliceSortedActions.clear();
        sliceSortedActions = null;
        actionPerSlice.clear();
        actionPerSlice = null;
        hiddenActions.clear();
        hiddenActions = null;
        innerPanel = null;
    }

    public MultiSliceObserver(MultiSlicePositioner mp) {
        this.mp = mp;
    }

    public void hide(CancelableAction action) {
        hiddenActions.add(action);
    }

    public void unhide(CancelableAction action) {
        hiddenActions.remove(action);
    }

    public synchronized String getTextSlice(SliceSources slice) {
        String log = "slice #";

        log+= slice.getName()+"\n";

        for (int indexAction = 0; indexAction<sliceSortedActions.get(slice).size();indexAction++) {
            CancelableAction action = sliceSortedActions.get(slice).get(indexAction);

            if (hiddenActions.contains(action)) {
                indexAction++;
                continue;
            }

            if (action instanceof MoveSliceAction) {
                if (indexAction == sliceSortedActions.get(slice).size()-1) {
                    log += action.toString() + "\n";
                } else {
                    if (sliceSortedActions.get(slice).get(indexAction+1) instanceof MoveSliceAction) {
                        // ignored action
                    } else {
                        log += action.toString() + "\n";
                    }
                }
            } else {
                log += action.toString() + "\n";
            }
        }

        return log;
    }

    public synchronized void updateInfoPanel(SliceSources slice) {
        logger.debug("UpdateInfoPanel called");
        if (sliceSortedActions.containsKey(slice)
                &&mp.getSlices().contains(slice)
                &&sliceSortedActions.get(slice).size()!=0) {

            logger.debug("UpdateInfoPanel called 0");
            if (!actionPerSlice.containsKey(slice)) {

                logger.debug("UpdateInfoPanel called a");
                JTextArea textArea = new JTextArea();
                textArea.setEditable(false);
                innerPanel.add(textArea);
                textAreaPerSlice.put(slice, textArea);
            }

            logger.debug("UpdateInfoPanel called 1");
            String infoSlice = getTextSlice(slice);

            actionPerSlice.put(slice, infoSlice);

            logger.debug("UpdateInfoPanel called 2");
            textAreaPerSlice.get(slice).setText(getTextSlice(slice));

            logger.debug("UpdateInfoPanel called 3");

        } else {
            // Slice has been removed

            logger.debug("UpdateInfoPanel called 4");
            if (actionPerSlice.containsKey(slice)) {
                innerPanel.remove(textAreaPerSlice.get(slice));
                actionPerSlice.remove(slice);
                sliceSortedActions.remove(slice);
            }
            innerPanel.validate();
        }
        repaintNeeded = true;
    }

    public synchronized void sendInfo(CancelableAction action) {
        if (action.getSliceSources()!=null) {
            if (!sliceSortedActions.containsKey(action.getSliceSources())) {
                sliceSortedActions.put(action.getSliceSources(), new ArrayList<>());
            }
            sliceSortedActions.get(action.getSliceSources()).add(action);
            updateInfoPanel(action.getSliceSources());
        }
        repaintNeeded = true;
    }

    public synchronized List<CancelableAction> getActionsFromSlice(SliceSources slice) {
        return sliceSortedActions.get(slice);
    }

    public synchronized void cancelInfo(CancelableAction action) {
        if ((action.getSliceSources()!=null)&&(sliceSortedActions.get(action.getSliceSources())!=null)) {
            sliceSortedActions.get(action.getSliceSources()).remove(action);
            updateInfoPanel(action.getSliceSources());
        }
        repaintNeeded = true;
    }

}
