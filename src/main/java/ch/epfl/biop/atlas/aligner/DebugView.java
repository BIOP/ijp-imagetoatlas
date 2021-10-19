package ch.epfl.biop.atlas.aligner;

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
        logger.accept("Deleted slice "+slice);
    }

    @Override
    public void sliceCreated(SliceSources slice) {
        logger.accept("Created slice "+slice);
    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {
        logger.accept("Z position changed slice "+slice);
    }

    @Override
    public void sliceVisibilityChanged(SliceSources slice) {

    }

    @Override
    public void sliceSelected(SliceSources slice) {

    }

    @Override
    public void sliceDeselected(SliceSources slice) {

    }

    @Override
    public void sliceSourcesChanged(SliceSources slice) {

    }

    @Override
    public void slicePretransformChanged(SliceSources sliceSources) {

    }

    @Override
    public void roiChanged() {

    }
}
