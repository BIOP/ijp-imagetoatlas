package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.atlas.aligner.commands.EditLastRegistrationCommand;
import org.scijava.command.CommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.function.Consumer;

public class SliceSourcesPopupMenu {

    protected static Logger logger = LoggerFactory.getLogger(SliceSourcesPopupMenu.class);

    private JPopupMenu popup;
    private final SliceSources[] slices;
    MultiSlicePositioner mp;

    public SliceSourcesPopupMenu( MultiSlicePositioner mp, SliceSources[] slices ) {
        this.slices = slices;
        this.mp = mp;
        createPopupMenu();
    }

    /**
     * Because SliceDisplayPanel does not allow to create conveniently a popupmenu on the fly
     * @param mp multipositioner
     * @return a popup menu with many actions
     */
    public static JPopupMenu createFinalPopupMenu(MultiSlicePositioner mp) {
        JPopupMenu popup = new JPopupMenu();

        addPopupAction(popup,"Show all Slices", mp::showAllSlices);

        addPopupAction(popup, "Show current slice", mp::showCurrentSlice);

        popup.addSeparator();

        addPopupAction(popup,"Set as Key Slice(s)", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            for (SliceSources slice : slices) {
                new KeySliceOn(mp, slice).runRequest();
            }
            if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
        });

        addPopupAction(popup,"Remove Key Slice(s)", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            for (SliceSources slice : slices) {
                new KeySliceOff(mp, slice).runRequest();
            }
            if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
        });

        addPopupAction(popup, "Hide Slices", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            for (SliceSources slice : slices) {
                slice.getGUIState().setSliceInvisible();
            }
        });

        addPopupAction(popup, "Show Slices", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            for (SliceSources slice : slices) {
                slice.getGUIState().setSliceVisible();
            }
        });

        addPopupAction(popup, "Remove Selected Slices ", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            for (SliceSources slice : slices) {
                System.out.println("Slice delete +"+slice);
                new DeleteSlice(mp, slice).runRequest();
            }
            if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
        });

        popup.addSeparator();

        addPopupAction(popup, "Edit Last Registration", () -> {
            //SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            //Object[] options = { "Yes", "No" };
            /*int resp = JOptionPane.showOptionDialog(null,
                    "Edit with all channels ?", "Edit options",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);*/
            /*if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            for (SliceSources slice : slices) {
                new EditLastRegistration(mp, slice, resp==0).runRequest();
            }
            if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();*/
            mp.scijavaCtx.getService(CommandService.class).run(EditLastRegistrationCommand.class,true, "mp", mp);
        });

        addPopupAction(popup, "Remove Last Registration", () -> {
            SliceSources[] slices = mp.getSelectedSources().toArray(new SliceSources[0]);
            if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            for (SliceSources slice : slices) {
                new DeleteLastRegistration(mp, slice).runRequest();
            }
            if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
        });

        popup.addSeparator();

        addPopupAction(popup, "Undo Last Action", mp::cancelLastAction);

        addPopupAction(popup,"Redo Last Action", mp::redoAction);

        popup.addSeparator();

        addPopupAction(popup,"Positioning mode", mp::setPositioningMode);

        addPopupAction(popup,"Registration mode", mp::setReviewMode);

        addPopupAction(popup, "Change overlap mode", mp::toggleOverlap);

        return popup;
    }

    private void createPopupMenu()
    {
        popup = new JPopupMenu();

        addPopupAction("Show all Slices", (slices) -> mp.showAllSlices());

        addPopupAction("Show current slice", (slices)-> mp.showCurrentSlice());

        if (slices.length>0) {

            addPopupLine();

            addPopupAction("Set as Key Slice(s)", (slices) -> {
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
                for (SliceSources slice : slices) {
                    new KeySliceOn(mp, slice).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            });

            addPopupAction("Remove Key Slice(s)", (slices) -> {
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
                for (SliceSources slice : slices) {
                    new KeySliceOff(mp, slice).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            });

            addPopupAction("Hide Slices", (slices) -> {
                for (SliceSources slice : slices) {
                    slice.getGUIState().setSliceInvisible();
                }
            });

            addPopupAction("Show Slices", (slices) -> {
                for (SliceSources slice : slices) {
                    slice.getGUIState().setSliceVisible();
                }
            });

            addPopupAction("Remove Selected Slices ("+slices.length+")", (slices) -> {
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
                for (SliceSources slice : slices) {
                    System.out.println("Slice delete +"+slice);
                    new DeleteSlice(mp, slice).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
            });

            addPopupLine();

            addPopupAction("Edit Last Registration", (slices) -> {
                /*Object[] options = { "Yes", "No" };
                int resp = JOptionPane.showOptionDialog(null,
                        "Edit with all channels ?", "Edit options",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();
                for (SliceSources slice : slices) {
                    new EditLastRegistration(mp, slice, resp==0).runRequest();
                }
                if (slices.length>1) new MarkActionSequenceBatch(mp).runRequest();*/
                mp.scijavaCtx.getService(CommandService.class).run(EditLastRegistrationCommand.class,true, "mp", mp);
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
            addPopupAction("Undo "+mp.getUndoMessage(), (slices) -> mp.cancelLastAction());
        }

        if (mp.redoableUserActions.size()>0) {
            addPopupAction("Redo "+mp.getRedoMessage(), (slices) -> mp.redoAction());
        }

        addPopupLine();

        if (mp.getDisplayMode() != MultiSlicePositioner.POSITIONING_MODE_INT) {
            addPopupAction("Positioning mode", (slices) -> mp.setPositioningMode());
        }

        if (mp.getDisplayMode() != MultiSlicePositioner.REVIEW_MODE_INT) {
            addPopupAction("Registration mode", (slices) -> mp.setReviewMode());
        }

        addPopupAction("Change overlap mode", (slices) -> mp.toggleOverlap());

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
