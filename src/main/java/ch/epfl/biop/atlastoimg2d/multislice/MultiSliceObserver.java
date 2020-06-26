package ch.epfl.biop.atlastoimg2d.multislice;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiSliceObserver {

    final MultiSlicePositioner mp;

    Map<SliceSources, List<MultiSlicePositioner.CancelableAction>> sliceSortedActions = new ConcurrentHashMap<>();

    Map<SliceSources, JTextArea> actionPerSlice = new ConcurrentHashMap<>();

    JPanel innerPanel = new JPanel();

    public MultiSliceObserver(MultiSlicePositioner mp) {
        this.mp = mp;

        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
    }

    public JComponent getJPanel() {
        return innerPanel;
    }

    public synchronized String getTextSlice(SliceSources slice) {
        String log = "slice #";

        log+= mp.getSortedSlices().indexOf(slice)+"\n";

        for (int indexAction = 0; indexAction<sliceSortedActions.get(slice).size();indexAction++) {
            MultiSlicePositioner.CancelableAction action = sliceSortedActions.get(slice).get(indexAction);
            if (action instanceof MultiSlicePositioner.MoveSlice) {
                if (indexAction == sliceSortedActions.get(slice).size()-1) {
                    log += action.toString() + "\n";
                } else {
                    if (sliceSortedActions.get(slice).get(indexAction+1) instanceof MultiSlicePositioner.MoveSlice) {
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
        if (sliceSortedActions.containsKey(slice)&&sliceSortedActions.get(slice).size()!=0) {

            if (!actionPerSlice.containsKey(slice)) {
                JTextArea textArea = new JTextArea();
                textArea.setEditable(false);
                actionPerSlice.put(slice, textArea);
                innerPanel.add(actionPerSlice.get(slice));
            }

            actionPerSlice.get(slice).setText(getTextSlice(slice));

        } else {
            // Slice has been removed
            if (actionPerSlice.containsKey(slice)) {
                innerPanel.remove(actionPerSlice.get(slice));
                actionPerSlice.remove(slice);
                sliceSortedActions.remove(slice);
            }
            innerPanel.validate();
        }
    }

    public synchronized void sendInfo(MultiSlicePositioner.CancelableAction action) {
        if (action.getSliceSources()!=null) {
            if (!sliceSortedActions.containsKey(action.getSliceSources())) {
                sliceSortedActions.put(action.getSliceSources(), new ArrayList<>());
            }
            sliceSortedActions.get(action.getSliceSources()).add(action);
            updateInfoPanel(action.getSliceSources());
        }
    }

    public synchronized void cancelInfo(MultiSlicePositioner.CancelableAction action) {
        if (action.getSliceSources()!=null) {
            sliceSortedActions.get(action.getSliceSources()).remove(action);
            updateInfoPanel(action.getSliceSources());
        }
    }
}
