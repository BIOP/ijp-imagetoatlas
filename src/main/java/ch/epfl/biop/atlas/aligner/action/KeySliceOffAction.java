package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;

import java.awt.*;

/**
 * Remove key slice : it is not automatically stretch by the user
 */
public class KeySliceOffAction extends CancelableAction {

    private final SliceSources sliceSource;

    public KeySliceOffAction(MultiSlicePositioner mp, SliceSources sliceSource) {
        super(mp);
        this.sliceSource = sliceSource;
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    public boolean run() {
        if (sliceSource.isKeySlice()) {
            sliceSource.keySliceOff();
            return true;
        } else return false; // already NOT a key slice
    }

    public String toString() {
        return "Key slice Off";
    }

    public boolean cancel() {
        sliceSource.keySliceOn();
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString("NK", (int) px - 5, (int) py + 5);//+new DecimalFormat("###.##").format(newSlicingAxisPosition), (int) px-5, (int) py+5);
    }


}