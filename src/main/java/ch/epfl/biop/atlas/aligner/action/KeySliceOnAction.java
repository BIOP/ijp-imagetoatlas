package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;

import java.awt.*;

/**
 * Set a slice as a key slice : it is not automatically stretch by the user
 */
public class KeySliceOnAction extends CancelableAction {

    private final SliceSources sliceSource;

    public KeySliceOnAction(MultiSlicePositioner mp, SliceSources sliceSource) {
        super(mp);
        this.sliceSource = sliceSource;
        hide();
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    public boolean run() {
        if (!sliceSource.isKeySlice()) {
            sliceSource.keySliceOn();
            return true;
        } else return false; // already a key slice
    }

    public String toString() {
        return "Key slice";
    }

    public boolean cancel() {
        sliceSource.keySliceOff();
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString("K", (int) px - 5, (int) py + 5);//+new DecimalFormat("###.##").format(newSlicingAxisPosition), (int) px-5, (int) py+5);
    }

}