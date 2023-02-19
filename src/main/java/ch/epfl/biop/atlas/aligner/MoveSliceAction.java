package ch.epfl.biop.atlas.aligner;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.function.Supplier;

/**
 * Moves a slice to a new position along the slicing axis
 */
public class MoveSliceAction extends CancelableAction {

    private final SliceSources sliceSource;
    private double oldSlicingAxisPosition;
    private double newSlicingAxisPosition;
    Supplier<Double> slicingAxisPositionSupplier = null;

    public MoveSliceAction(MultiSlicePositioner mp, SliceSources sliceSource, double slicingAxisPosition) {
        super(mp);
        this.sliceSource = sliceSource;
        this.oldSlicingAxisPosition = sliceSource.getSlicingAxisPosition();
        this.newSlicingAxisPosition = slicingAxisPosition;
        hide();
    }

    // Delayed position - only known when run is started, this is for DeepSliceRegistration
    public MoveSliceAction(MultiSlicePositioner mp, SliceSources sliceSource, Supplier<Double> slicingAxisPositionSupplier) {
        super(mp);
        this.sliceSource = sliceSource;
        this.slicingAxisPositionSupplier = slicingAxisPositionSupplier;
        hide();
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    // For serialization
    public double getSlicingAxisPosition() {
        return newSlicingAxisPosition;
    }

    protected boolean run() {
        oldSlicingAxisPosition = sliceSource.getSlicingAxisPosition();
        if (slicingAxisPositionSupplier!=null) {
            newSlicingAxisPosition = slicingAxisPositionSupplier.get();
        }
        sliceSource.setSlicingAxisPosition(newSlicingAxisPosition);
        return true;
    }

    public String toString() {
        return "Axis Position = " + new DecimalFormat("###.##").format(newSlicingAxisPosition);
    }

    protected boolean cancel() {
        sliceSource.setSlicingAxisPosition(oldSlicingAxisPosition);
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString("M", (int) px - 5, (int) py + 5);//+new DecimalFormat("###.##").format(newSlicingAxisPosition), (int) px-5, (int) py+5);
    }

}