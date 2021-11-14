package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.atlas.aligner.action.CancelableAction;
import ij.IJ;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class DebugView implements MultiSlicePositioner.SliceChangeListener{

    Consumer<String> logger = IJ::log;
    final MultiSlicePositioner msp;

    public DebugView(MultiSlicePositioner msp) {
        JFrame debug = new JFrame("Debug MultiSlicePositioner "+msp);
        this.msp = msp;
        JPanel debugActions = new JPanel(new FlowLayout());
        JButton writeUserActions = new JButton("Write User Actions");
        writeUserActions.addActionListener((e) -> {
            logger.accept("User actions: ");
            msp.userActions.forEach(action -> logger.accept("\t"+action.toString()));
        });

        JButton writeRedoableUserActions = new JButton("Write Redoable User Actions");
        writeRedoableUserActions.addActionListener((e) -> {
            logger.accept("Redoable user actions: ");
            msp.redoableUserActions.forEach(action -> logger.accept("\t"+action.toString()));
        });

        debugActions.add(writeUserActions);
        debugActions.add(writeRedoableUserActions);
        debug.add(debugActions);
        debug.pack();
        debug.setVisible(true);
        msp.addSliceListener(this);
    }

    @Override
    public void sliceDeleted(SliceSources slice) {
        logger.accept("Deleted slice "+slice.getName());
    }

    @Override
    public void sliceCreated(SliceSources slice) {
        logger.accept("Created slice "+slice.getName());
    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {
        logger.accept("Z position changed slice "+slice.getName());
    }

    @Override
    public void sliceSelected(SliceSources slice) {
        logger.accept("Slice selected "+slice.getName());
    }

    @Override
    public void sliceDeselected(SliceSources slice) {
        logger.accept("Slice deselected "+slice.getName());
    }

    @Override
    public void sliceSourcesChanged(SliceSources slice) {
        logger.accept("Slice sources changed "+slice.getName());
    }

    @Override
    public void slicePretransformChanged(SliceSources slice) {
        logger.accept("Slice pretransform changed "+slice.getName());
    }

    @Override
    public void sliceKeyOn(SliceSources slice) {
        logger.accept("Slice is a key Slice "+slice.getName());
    }

    @Override
    public void sliceKeyOff(SliceSources slice) {
        logger.accept("Slice is not a key slice "+slice.getName());
    }

    @Override
    public void roiChanged() {
        logger.accept("ROI changed");
    }

    @Override
    public void actionEnqueue(SliceSources slice, CancelableAction action) {
        logger.accept("Action ["+action.actionClassString()+"] Enqueued");
    }

    @Override
    public void actionStarted(SliceSources slice, CancelableAction action) {
        logger.accept("Action ["+action.actionClassString()+"] Started");
    }

    @Override
    public void actionFinished(SliceSources slice, CancelableAction action, boolean result) {
        String success = result?"Yes":"No";
        logger.accept("Action ["+action.actionClassString()+"] Finished. Success ? "+success);
    }

    @Override
    public void actionCancelEnqueue(SliceSources slice, CancelableAction action) {
        logger.accept("Action ["+action.actionClassString()+"] Cancelation enqueued.");
    }

    @Override
    public void actionCancelStarted(SliceSources slice, CancelableAction action) {
        logger.accept("Action ["+action.actionClassString()+"] Cancelation Started.");
    }

    @Override
    public void actionCancelFinished(SliceSources slice, CancelableAction action, boolean result) {
        String success = result?"Yes":"No";
        logger.accept("Action ["+action.actionClassString()+"] Cancelation Ended. Success ? "+success);
    }

}
