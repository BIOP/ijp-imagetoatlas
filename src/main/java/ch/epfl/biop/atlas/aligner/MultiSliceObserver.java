package ch.epfl.biop.atlas.aligner;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MultiSliceObserver {

    MultiSlicePositioner mp;

    Map<SliceSources, List<CancelableAction>> sliceSortedActions = new ConcurrentHashMap<>();

    Map<SliceSources, JTextArea> actionPerSlice = new ConcurrentHashMap<>();

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

        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));

        animatorThread = new Thread(() -> {
            while (animate) {

                if (repaintNeeded) {
                    mp.bdvh.getViewerPanel().getDisplay().repaint();
                }
                repaintNeeded = false;
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
            //System.out.println("Animator thread stopped");
        });
        animatorThread.start();

    }

    public void hide(CancelableAction action) {
        hiddenActions.add(action);
    }

    public void unhide(CancelableAction action) {
        hiddenActions.remove(action);
    }

    public void draw(Graphics2D g) {
        int yInc = 20;
        g.setColor(new Color(255,255,255,200));
        synchronized (sliceSortedActions) {
            sliceSortedActions.forEach(((slice, actions) -> {
                if (mp.slices.contains(slice)) {
                    int xP = slice.getBdvHandleCoords()[0];
                    int yP = slice.getBdvHandleCoords()[1] + yInc;

                    int yP0 = yP;

                    for (int indexAction = 0; indexAction < actions.size(); indexAction++) {
                        CancelableAction action = sliceSortedActions.get(slice).get(indexAction);

                        if (hiddenActions.contains(action)) {
                            indexAction++;
                            continue;
                        }

                        if (action instanceof MoveSlice) {
                            if (indexAction == sliceSortedActions.get(slice).size() - 1) {

                                action.draw(g, xP, yP, 1);
                                yP += yInc;

                                if ((mp.iCurrentSlice >= 0) && (mp.iCurrentSlice < mp.slices.size()))
                                    if (mp.slices.get(mp.iCurrentSlice).equals(slice)) {
                                        action.draw(g, 50, yP - yP0 + 50, 1);
                                    }
                            } else {
                                if (sliceSortedActions.get(slice).get(indexAction + 1) instanceof MoveSlice) {
                                    // ignored action
                                } else {
                                    action.draw(g, xP, yP, 1);
                                    yP += yInc;

                                    if ((mp.iCurrentSlice >= 0) && (mp.iCurrentSlice < mp.slices.size()))
                                        if (mp.slices.get(mp.iCurrentSlice).equals(slice)) {
                                            action.draw(g, 50, yP - yP0 + 50, 1);
                                        }
                                }
                            }
                        } else {
                            action.draw(g, xP, yP, 1);
                            yP += yInc;
                            if ((mp.iCurrentSlice >= 0) && (mp.iCurrentSlice < mp.slices.size()))
                                if (mp.slices.get(mp.iCurrentSlice).equals(slice)) {
                                    action.draw(g, 50, yP - yP0, 1);
                                }
                        }
                    }
                }
            }));
        }

    }

    public void end() {
        this.animate = false;
        try {
            animatorThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public JComponent getJPanel() {
        return innerPanel;
    }

    public synchronized String getTextSlice(SliceSources slice) {
        String log = "slice #";

        log+= mp.getSortedSlices().indexOf(slice)+"\n";

        for (int indexAction = 0; indexAction<sliceSortedActions.get(slice).size();indexAction++) {
            CancelableAction action = sliceSortedActions.get(slice).get(indexAction);

            if (hiddenActions.contains(action)) {
                indexAction++;
                continue;
            }

            if (action instanceof MoveSlice) {
                if (indexAction == sliceSortedActions.get(slice).size()-1) {
                    log += action.toString() + "\n";
                } else {
                    if (sliceSortedActions.get(slice).get(indexAction+1) instanceof MoveSlice) {
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
        //System.out.println("updateInfoPanel called");
        if (sliceSortedActions.containsKey(slice)
                &&mp.slices.contains(slice)
                &&sliceSortedActions.get(slice).size()!=0) {

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
