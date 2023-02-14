package ch.epfl.biop.atlas.aligner;

import javax.swing.*;
import java.awt.*;
import java.sql.Timestamp;
import java.util.function.BiConsumer;
public class DebugView implements MultiSlicePositioner.SliceChangeListener{

    final BiConsumer<SliceSources,String> logger = this::append;

    private synchronized void append(SliceSources slice, String s) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (slice==null) {
            area.append(timestamp+"\t-\t"+s+"\n");
        } else {
            area.append(timestamp+"\t"+slice.getName()+"\t"+s+"\n");
        }
    }

    final MultiSlicePositioner msp;

    final JTextArea area = new JTextArea();

    public DebugView(MultiSlicePositioner msp) {
        JFrame debug = new JFrame("Debug MultiSlicePositioner "+msp);
        this.msp = msp;
        JPanel debugActions = new JPanel(new FlowLayout());
        JButton writeUserActions = new JButton("Write User Actions");
        writeUserActions.addActionListener((e) -> {
            //logger.accept("User actions: ");
            msp.userActions.forEach(action -> logger.accept(action.getSliceSources(),"\t"+action));
        });

        JButton writeRedoableUserActions = new JButton("Write Redoable User Actions");
        writeRedoableUserActions.addActionListener((e) -> {
            //logger.accept("Redoable user actions: ");
            msp.redoableUserActions.forEach(action -> logger.accept(action.getSliceSources(), "\t"+action));
        });

        debugActions.add(writeUserActions);
        debugActions.add(writeRedoableUserActions);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        panel.add(debugActions, BorderLayout.SOUTH);
        debug.add(panel);
        debug.pack();
        debug.setVisible(true);
        msp.addSliceListener(this);
    }

    @Override
    public void sliceDeleted(SliceSources slice) {
        logger.accept(slice,"Deleted slice "+slice.getName());
    }

    @Override
    public void sliceCreated(SliceSources slice) {
        logger.accept(slice,"Created slice "+slice.getName());
    }

    @Override
    public void sliceZPositionChanged(SliceSources slice) {
        logger.accept(slice,"Z position changed slice "+slice.getName());
    }

    @Override
    public void sliceSelected(SliceSources slice) {
        logger.accept(slice,"Slice selected "+slice.getName());
    }

    @Override
    public void sliceDeselected(SliceSources slice) {
        logger.accept(slice,"Slice deselected "+slice.getName());
    }

    @Override
    public void sliceSourcesChanged(SliceSources slice) {
        logger.accept(slice,"Slice sources changed "+slice.getName());
    }

    @Override
    public void slicePretransformChanged(SliceSources slice) {
        logger.accept(slice,"Slice pretransform changed "+slice.getName());
    }

    @Override
    public void sliceKeyOn(SliceSources slice) {
        logger.accept(slice,"Slice is a key Slice "+slice.getName());
    }

    @Override
    public void sliceKeyOff(SliceSources slice) {
        logger.accept(slice,"Slice is not a key slice "+slice.getName());
    }

    @Override
    public void roiChanged() {
        logger.accept(null,"ROI changed");
    }

    @Override
    public void actionEnqueue(SliceSources slice, CancelableAction action) {
        logger.accept(slice,"Action ["+action.actionClassString()+"] Enqueued");
    }

    @Override
    public void actionStarted(SliceSources slice, CancelableAction action) {
        logger.accept(slice,"Action ["+action.actionClassString()+"] Started");
    }

    @Override
    public void actionFinished(SliceSources slice, CancelableAction action, boolean result) {
        String success = result?"Yes":"No";
        logger.accept(slice,"Action ["+action.actionClassString()+"] Finished. Success ? "+success);
    }

    @Override
    public void actionCancelEnqueue(SliceSources slice, CancelableAction action) {
        logger.accept(slice,"Action ["+action.actionClassString()+"] Cancelation enqueued.");
    }

    @Override
    public void actionCancelStarted(SliceSources slice, CancelableAction action) {
        logger.accept(slice,"Action ["+action.actionClassString()+"] Cancelation Started.");
    }

    @Override
    public void actionCancelFinished(SliceSources slice, CancelableAction action, boolean result) {
        String success = result?"Yes":"No";
        logger.accept(slice,"Action ["+action.actionClassString()+"] Cancelation Ended. Success ? "+success);
    }

    @Override
    public void converterChanged(SliceSources slice) {
        logger.accept(slice,"Slice is converter has changed "+slice.getName());
    }

}
