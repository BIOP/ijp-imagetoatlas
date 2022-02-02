package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.CancelableAction;
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
        hide();
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    protected boolean run() {
        if (sliceSource.isKeySlice()) {
            sliceSource.keySliceOff();
            getMP().stateHasBeenChanged();
            return true;
        } else return true; // already NOT a key slice, but we don't care
    }

    public String toString() {
        return "Key slice Off";
    }

    protected boolean cancel() {
        if (!sliceSource.isKeySlice()) {
            sliceSource.keySliceOn();
            getMP().stateHasBeenChanged();
        }
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString("NK", (int) px - 5, (int) py + 5);//+new DecimalFormat("###.##").format(newSlicingAxisPosition), (int) px-5, (int) py+5);
    }


}