package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class SliceSourcesPopupMenu {

    private JPopupMenu popup;
    private final SliceSources[] slices;
    MultiSlicePositioner mp;

    public SliceSourcesPopupMenu( MultiSlicePositioner mp, SliceSources[] slices ) {
        this.slices = slices;
        this.mp = mp;
        createPopupMenu();
    }

    private JPopupMenu createPopupMenu()
    {
        popup = new JPopupMenu();

        addPopupAction("Show all Slices", (slices) -> {
            List<SourceAndConverter<?>> sacsToAppend = new ArrayList<>();
            for (SliceSources slice : mp.getSortedSlices()) {
                if (slice.mp.currentMode == MultiSlicePositioner.POSITIONING_MODE_INT) {
                    sacsToAppend.addAll(Arrays.asList(slice.relocated_sacs_positioning_mode));
                }
                if (slice.mp.currentMode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
                    sacsToAppend.addAll(Arrays.asList(slice.registered_sacs));
                }
            }
            mp.getBdvh().getViewerPanel().state().setSourcesActive(sacsToAppend, true);
        });

        addPopupAction("Show current slice", (slices)-> {
            SliceSources ss = mp.getSortedSlices().get(mp.iCurrentSlice);
            for (SliceSources slice : mp.getSortedSlices()) {
                if (slice.mp.currentMode == MultiSlicePositioner.POSITIONING_MODE_INT) {
                    mp.getBdvh().getViewerPanel().state().setSourcesActive(Arrays.asList(ss.relocated_sacs_positioning_mode), true);
                }
                if (slice.mp.currentMode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
                    mp.getBdvh().getViewerPanel().state().setSourcesActive(Arrays.asList(ss.registered_sacs), true);
                }
            }
        });


        if (slices.length>0) {

            addPopupLine();

            addPopupAction("Hide Slices", (slices) -> {
                for (SliceSources slice : slices) {
                    slice.hide();
                }
            });

            addPopupAction("Delete Slices", (slices) -> {
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
                for (SliceSources slice : slices) {
                    new DeleteSlice(mp, slice).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            });

            addPopupLine();

            addPopupAction("Edit Last Registration", (slices) -> {
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
                for (SliceSources slice : slices) {
                    new EditLastRegistration(mp, slice).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            });

            addPopupAction("Remove Last Registration", (slices) -> {
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
                for (SliceSources slice : slices) {
                    new DeleteLastRegistration(mp, slice).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            });
        }

        addPopupLine();

        if (mp.userActions.size()>0) {
            addPopupAction("Undo "+mp.getUndoMessage(), (slices) -> {
                mp.cancelLastAction();
            });
        }

        if (mp.redoableUserActions.size()>0) {
            addPopupAction("Redo "+mp.getRedoMessage(), (slices) -> {
                mp.redoAction();
            });
        }

        addPopupLine();

        if (mp.currentMode != MultiSlicePositioner.POSITIONING_MODE_INT) {
            addPopupAction("Positioning mode", (slices) -> {
                mp.setPositioningMode();
            });
        }

        if (mp.currentMode != MultiSlicePositioner.REGISTRATION_MODE_INT) {
            addPopupAction("Registration mode", (slices) -> {
                mp.setRegistrationMode();
            });
        }

        addPopupAction("Change overlap mode", (slices) -> {
            mp.toggleOverlap();
        });

        return popup;
    }

    /**
     * Adds a separator in the popup menu
     */
    public void addPopupLine() {
        popup.addSeparator();
    }

    /**
     * Adds a line and an action which consumes all the selected SourceAndConverter objects
     * in the popup Menu
     * @param action
     * @param actionName
     */
    public void addPopupAction( String actionName, Consumer<SliceSources[]> action ) {
        if (action==null) {
            System.err.println("No action defined for action named "+actionName);
        }
        JMenuItem menuItem = new JMenuItem(actionName);
        menuItem.addActionListener(e -> action.accept(slices));
        popup.add(menuItem);
    }

    public JPopupMenu getPopup()
    {
        return popup;
    }
}
