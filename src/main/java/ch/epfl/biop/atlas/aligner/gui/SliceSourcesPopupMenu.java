package ch.epfl.biop.atlas.aligner.gui;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.*;
import ch.epfl.biop.atlas.aligner.command.RegistrationEditLastCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import org.scijava.command.CommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.function.Consumer;

public class SliceSourcesPopupMenu {

    protected static Logger logger = LoggerFactory.getLogger(SliceSourcesPopupMenu.class);

    private JPopupMenu popup;
    private final SliceSources[] slices;
    final MultiSlicePositioner mp;
    final BdvMultislicePositionerView view;

    public SliceSourcesPopupMenu(MultiSlicePositioner mp, BdvMultislicePositionerView view, SliceSources[] slices ) {
        this.slices = slices;
        this.mp = mp;
        this.view = view;
        createPopupMenu();
    }

    /**
     * Because SliceDisplayPanel does not allow to create conveniently a popupmenu on the fly
     * @param mp multipositioner
     * @return a popup menu with many actions
     */
    public static JPopupMenu createFinalPopupMenu(MultiSlicePositioner mp, BdvMultislicePositionerView view) {
        JPopupMenu popup = new JPopupMenu();

        addPopupAction(popup,"Set as Key Slice(s)", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice : slices) {
                new KeySliceOnAction(mp, slice).runRequest();
            }
            if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
        });

        addPopupAction(popup,"Remove Key Slice(s)", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice : slices) {
                new KeySliceOffAction(mp, slice).runRequest();
            }
            if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
        });

        /* TODO : restore ? or not ?
        addPopupAction(popup, "Hide Slices", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            for (SliceSources slice : slices) {
                view.setSliceInvisible(slice);
            }
        });

        addPopupAction(popup, "Show Slices", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            for (SliceSources slice : slices) {
                view.setSliceVisible(slice);
            }
        });
        */

        addPopupAction(popup, "Remove Selected Slices ", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice : slices) {
                new DeleteSliceAction(mp, slice).runRequest();
            }
            if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
        });

        popup.addSeparator();

        addPopupAction(popup, "Edit Last Registration", () -> {
            mp.getContext().getService(CommandService.class).run(RegistrationEditLastCommand.class,true, "mp", mp);
        });

        addPopupAction(popup, "Remove Last Registration", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice : slices) {
                new DeleteLastRegistrationAction(mp, slice).runRequest();
            }
            if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
        });

        popup.addSeparator();

        addPopupAction(popup, "Undo Last Action", mp::cancelLastAction);

        addPopupAction(popup,"Redo Last Action", mp::redoAction);

        popup.addSeparator();

        addPopupAction(popup,"Positioning mode", () -> view.setDisplayMode(BdvMultislicePositionerView.POSITIONING_MODE_INT));

        addPopupAction(popup,"Registration mode", () -> view.setDisplayMode(BdvMultislicePositionerView.REVIEW_MODE_INT));

        addPopupAction(popup, "Change overlap mode", view::toggleOverlap);

        return popup;
    }

    private void createPopupMenu()
    {
        popup = new JPopupMenu();

        if (slices.length>0) {

            addPopupAction("Set as Key Slice(s)", (slices) -> {
                if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
                for (SliceSources slice : slices) {
                    new KeySliceOnAction(mp, slice).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
            });

            addPopupAction("Remove Key Slice(s)", (slices) -> {
                if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
                for (SliceSources slice : slices) {
                    new KeySliceOffAction(mp, slice).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
            });

            /* TODO : restore ? or not ?
            addPopupAction("Hide Slices", (slices) -> {
                for (SliceSources slice : slices) {
                    view.setSliceInvisible(slice);
                }
            });

            addPopupAction("Show Slices", (slices) -> {
                for (SliceSources slice : slices) {
                    view.setSliceVisible(slice);
                }
            });
            */

            addPopupAction("Remove Selected Slices ("+slices.length+")", (slices) -> {
                if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
                for (SliceSources slice : slices) {
                    new DeleteSliceAction(mp, slice).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
            });

            addPopupLine();

            addPopupAction("Edit Last Registration", (slices) -> {
                mp.getContext().getService(CommandService.class).run(RegistrationEditLastCommand.class,true, "mp", mp);
            });

            addPopupAction("Remove Last Registration", (slices) -> {
                if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
                for (SliceSources slice : slices) {
                    new DeleteLastRegistrationAction(mp, slice).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatchAction(mp).runRequest();
            });
        }

        addPopupLine();

        if (mp.userActionsSize()>0) {
            addPopupAction("Undo "+mp.getUndoMessage(), (slices) -> mp.cancelLastAction());
        }

        if (mp.redoableUserActionsSize()>0) {
            addPopupAction("Redo "+mp.getRedoMessage(), (slices) -> mp.redoAction());
        }

        addPopupLine();

        if (view.getDisplayMode() != BdvMultislicePositionerView.POSITIONING_MODE_INT) {
            addPopupAction("Positioning mode", (slices) -> view.setDisplayMode(BdvMultislicePositionerView.POSITIONING_MODE_INT));
        }

        if (view.getDisplayMode() != BdvMultislicePositionerView.REVIEW_MODE_INT) {
            addPopupAction("Review mode", (slices) -> view.setDisplayMode(BdvMultislicePositionerView.REVIEW_MODE_INT));
        }

        addPopupAction("Change overlap mode", (slices) -> view.toggleOverlap());

    }

    /**
     * Adds a separator in the popup menu
     */
    public void addPopupLine() {
        popup.addSeparator();
    }

    /*
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

    public static void addPopupAction( JPopupMenu popup, String actionName, Runnable runnable ) {
        JMenuItem menuItem = new JMenuItem(actionName);
        menuItem.addActionListener(e -> runnable.run());
        popup.add(menuItem);
    }

    public JPopupMenu getPopup()
    {
        return popup;
    }
}
