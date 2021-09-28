package ch.epfl.biop.atlas.aligner;

import bdv.viewer.ViewerPanel;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import org.scijava.ui.behaviour.DragBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class SelectionLayer {

    private static final Logger logger = LoggerFactory.getLogger(SelectionLayer.class);

    final MultiSlicePositioner mp;

    boolean isCurrentlySelecting = false;

    int xCurrentSelectStart, yCurrentSelectStart, xCurrentSelectEnd, yCurrentSelectEnd;

    final ViewerPanel viewer;

    public SelectionLayer(MultiSlicePositioner mp) {
        this.mp = mp;
        viewer = mp.getBdvh().getViewerPanel();
    }

    protected void addSelectionBehaviours(Behaviours behaviours) {
        behaviours.behaviour( new SelectionLayer.RectangleSelectSourcesBehaviour( SourceSelectorBehaviour.SET ), "select-set-sources", "button1");
        behaviours.behaviour( new SelectionLayer.RectangleSelectSourcesBehaviour( SourceSelectorBehaviour.ADD ), "select-add-sources", "shift button1");
        behaviours.behaviour( new SelectionLayer.RectangleSelectSourcesBehaviour( SourceSelectorBehaviour.REMOVE ), "select-remove-sources", "ctrl button1");
        // Ctrl + A : select all sources
        // behaviours.behaviour((ClickBehaviour) (x, y) -> ssb.selectedSourceAdd(viewer.state().getVisibleSources()), "select-all-visible-sources", new String[] { "ctrl A" } );
    }


    synchronized void startCurrentSelection(int x, int y) {
        xCurrentSelectStart = x;
        yCurrentSelectStart = y;
    }

    synchronized void updateCurrentSelection(int xCurrent, int yCurrent) {
        xCurrentSelectEnd = xCurrent;
        yCurrentSelectEnd = yCurrent;
        isCurrentlySelecting = true;
    }

    synchronized void endCurrentSelection(int x, int y, String mode) {
        xCurrentSelectEnd = x;
        yCurrentSelectEnd = y;
        isCurrentlySelecting = false;

        processSelectionModificationEvent(getLastSelectedSources(), mode);
    }

    /**
     * Main function coordinating events : it is called by all the other functions to process the modifications
     * of the selected sources
     * @param currentSources set of sources involved in the current modification
     * @param mode  see SET ADD REMOVE
     */
    public void processSelectionModificationEvent(Set<SliceSources> currentSources, String mode) {
        List<SliceSources> slices = mp.getSlices();
        switch(mode) {
            case SourceSelectorBehaviour.SET :
                for (SliceSources slice : slices) {
                    slice.deSelect();
                    if (currentSources.contains(slice)&&!slice.isSelected()) {
                        slice.select();
                    }
                }
                break;
            case SourceSelectorBehaviour.ADD :
                for (SliceSources slice : slices) {
                    // Sanity check : only visible sources can be selected
                    if (currentSources.contains(slice) && !slice.isSelected()) {
                        slice.select();
                    }
                }
                break;
            case SourceSelectorBehaviour.REMOVE :
                for (SliceSources slice : slices) {
                    // Sanity check : only visible sources can be selected
                    if (currentSources.contains(slice) && slice.isSelected()) {
                        slice.deSelect();
                    }
                }
                break;
            default:
                System.err.println("Unhandled "+mode+" selected source modification event");
                break;
        }
    }

    Stroke normalStroke = new BasicStroke();
    Color backColor = new Color(0xF7BF18);

    public void draw(Graphics2D g) {
        if (isCurrentlySelecting) {
            g.setStroke( normalStroke );
            g.setPaint( backColor );
            g.draw(getCurrentSelectionRectangle());
        }
    }

    Rectangle getCurrentSelectionRectangle() {
        int x0, y0, w, h;
        if (xCurrentSelectStart>xCurrentSelectEnd) {
            x0 = xCurrentSelectEnd;
            w = xCurrentSelectStart-xCurrentSelectEnd;
        } else {
            x0 = xCurrentSelectStart;
            w = xCurrentSelectEnd-xCurrentSelectStart;
        }
        if (yCurrentSelectStart>yCurrentSelectEnd) {
            y0 = yCurrentSelectEnd;
            h = yCurrentSelectStart-yCurrentSelectEnd;
        } else {
            y0 = yCurrentSelectStart;
            h = yCurrentSelectEnd-yCurrentSelectStart;
        }
        // Hack : allows selection on double or single click
        if (w==0) w = 1;
        if (h==0) h = 1;
        return new Rectangle(x0, y0, w, h);
    }

    Set<SliceSources> getLastSelectedSources() {
        Set<SliceSources> lastSelected = new HashSet<>();
        Rectangle r = getCurrentSelectionRectangle();
        //synchronized (mp.slices) {
            for (SliceSources slice : mp.getSlices()) {
                Integer[] coords = slice.getGUIState().getBdvHandleCoords();
                Point p = new Point(coords[0], coords[1]);
                if (r.contains(p)) lastSelected.add(slice);
            }
        //}
        return lastSelected;
    }

    /**
     * Drag Selection Behaviour
     */
    class RectangleSelectSourcesBehaviour implements DragBehaviour {

        final String mode;

        public RectangleSelectSourcesBehaviour(String mode) {
            this.mode = mode;
        }

        boolean perform;

        @Override
        public void init(int x, int y) {
            logger.debug("Selection drag start ? Perform = "+perform);
            perform = mp.startDragAction(); // ensure unicity of drag action
            if (perform) {
                logger.debug("Selection drag start !");
                startCurrentSelection(x, y);
                viewer.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                switch (mode) {
                    case SourceSelectorBehaviour.SET:
                        viewer.showMessage("Set Selection");
                        break;
                    case SourceSelectorBehaviour.ADD:
                        viewer.showMessage("Add Selection");
                        break;
                    case SourceSelectorBehaviour.REMOVE:
                        viewer.showMessage("Remove Selection");
                        break;
                }
            } else {
                logger.debug("Selection drag failed !");
            }
        }

        @Override
        public void drag(int x, int y) {
            if (perform) {
                updateCurrentSelection(x, y);
                viewer.getDisplay().repaint();
            }
        }

        @Override
        public void end(int x, int y) {
            if (perform) {
                endCurrentSelection(x, y, mode);
                viewer.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                viewer.getDisplay().repaint();
                mp.stopDragAction();
                perform = false;
                logger.debug("Selection drag stopped.");
            }
        }
    }

}
