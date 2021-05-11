package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Creates a new Slice source
 */
public class CreateSlice extends CancelableAction {

    protected static Logger logger = LoggerFactory.getLogger(CreateSlice.class);

    final private List<SourceAndConverter<?>> sacs;
    private SliceSources sliceSource;
    final public double slicingAxisPosition;
    final public double zSliceThicknessCorrection;
    final public double zSliceShiftCorrection;

    public CreateSlice(MultiSlicePositioner mp, List<SourceAndConverter<?>> sacs, double slicingAxisPosition, double zSliceThicknessCorrection, double zSliceShiftCorrection) {
        super(mp);
        this.sacs = sacs;
        this.slicingAxisPosition = slicingAxisPosition;
        this.zSliceShiftCorrection = zSliceShiftCorrection;
        this.zSliceThicknessCorrection = zSliceThicknessCorrection;
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    protected void setSliceSource(SliceSources slice) {
        this.sliceSource = slice;
    }

    @Override
    public boolean run() {
        synchronized (CreateSlice.class) { // only one slice addition at a time
            boolean sacAlreadyPresent = false;
            for (SourceAndConverter sac : sacs) {
                for (SliceSources slice : mp.getSlices()) {
                    for (SourceAndConverter test : slice.original_sacs) {
                        if (test.equals(sac)) {
                            sacAlreadyPresent = true;
                            break;
                        }
                    }
                }
            }

            if (sacAlreadyPresent) {
                SliceSources zeSlice = null;

                // A source is already included :
                // If all sources match exactly what's in a single SliceSources -> that's a move operation

                boolean exactMatch = false;
                for (SliceSources ss : mp.getPrivateSlices()) {
                    if (ss.exactMatch(sacs)) {
                        exactMatch = true;
                        zeSlice = ss;
                    }
                }

                if (!exactMatch) {
                    logger.error("A source is already used in the positioner : slice not created.");
                    mp.log.accept("A source is already used in the positioner : slice not created.");
                    return false;
                } else {
                    // Move action:
                    new MoveSlice(mp, zeSlice, slicingAxisPosition).runRequest();
                    return false;
                }
            }

            if (sliceSource == null) {// for proper redo function
                sliceSource = new SliceSources(sacs.toArray(new SourceAndConverter[0]),
                        slicingAxisPosition, mp, zSliceThicknessCorrection, zSliceShiftCorrection);
            }

            mp.createSlice(sliceSource);//.getPrivateSlices().add(sliceSource);

            mp.updateDisplay();

            sliceSource.getGUIState().sliceDisplayModeChanged(); // Triggers redraw on cancel

            mp.log.accept("Slice added");
        }
        return true;
    }

    public SliceSources getSlice() {
        return sliceSource;
    }

    public String toString() {
        return "Slice Created " + new DecimalFormat("###.##").format(this.slicingAxisPosition);
    }

    @Override
    public boolean cancel() {
        mp.removeSlice(sliceSource);
        mp.log.accept("Slice "+sliceSource+" removed ");
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString("C", (int) px - 5, (int) py + 5);
    }

    @Override
    public boolean draw() {
        return false;
    }
}
