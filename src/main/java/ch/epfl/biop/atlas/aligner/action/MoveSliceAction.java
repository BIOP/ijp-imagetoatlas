package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;

import java.awt.*;
import java.text.DecimalFormat;

/**
 * Moves a slice to a new position along the slicing axis
 */
public class MoveSliceAction extends CancelableAction {

    private final SliceSources sliceSource;
    private final double oldSlicingAxisPosition;
    private final double newSlicingAxisPosition;

    public MoveSliceAction(MultiSlicePositioner mp, SliceSources sliceSource, double slicingAxisPosition) {
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
        return true;
    }

    public String toString() {
        return "Axis Position = " + new DecimalFormat("###.##").format(newSlicingAxisPosition);
    }

    public boolean cancel() {
        sliceSource.setSlicingAxisPosition(oldSlicingAxisPosition);
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString("M", (int) px - 5, (int) py + 5);//+new DecimalFormat("###.##").format(newSlicingAxisPosition), (int) px-5, (int) py+5);
    }

}