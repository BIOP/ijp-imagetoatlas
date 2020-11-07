package ch.epfl.biop.atlastoimg2d.multislice;

import java.awt.*;
import java.text.DecimalFormat;

/**
 * Moves a slice to a new position along the slicing axis
 */
public class MoveSlice extends CancelableAction {

    private SliceSources sliceSource;
    private double oldSlicingAxisPosition;
    private double newSlicingAxisPosition;

    public MoveSlice(MultiSlicePositioner mp, SliceSources sliceSource, double slicingAxisPosition) {
        super(mp);
        this.sliceSource = sliceSource;
        this.oldSlicingAxisPosition = sliceSource.getSlicingAxisPosition();
        this.newSlicingAxisPosition = slicingAxisPosition;
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    // For serialization
    public double getSlicingAxisPosition() {
        return newSlicingAxisPosition;
    }

    public boolean run() {
        sliceSource.setSlicingAxisPosition(newSlicingAxisPosition);
        sliceSource.updatePosition();
        mp.log.accept("Moving slice to position " + new DecimalFormat("###.##").format(sliceSource.getSlicingAxisPosition()));
        mp.updateDisplay();
        return true;
    }

    public String toString() {
        return "Axis Position = " + new DecimalFormat("###.##").format(newSlicingAxisPosition);
    }

    public boolean cancel() {
        sliceSource.setSlicingAxisPosition(oldSlicingAxisPosition);
        sliceSource.updatePosition();
        mp.log.accept("Moving slice to position " + new DecimalFormat("###.##").format(sliceSource.getSlicingAxisPosition()));
        mp.updateDisplay();
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString("M", (int) px - 5, (int) py + 5);//+new DecimalFormat("###.##").format(newSlicingAxisPosition), (int) px-5, (int) py+5);
    }

}