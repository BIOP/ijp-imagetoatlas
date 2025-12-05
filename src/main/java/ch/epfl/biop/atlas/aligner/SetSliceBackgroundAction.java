package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvViewPrefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;

/**
 * Redefine out of bounds color for slices
 */
public class SetSliceBackgroundAction extends CancelableAction {

    protected static Logger logger = LoggerFactory.getLogger(SetSliceBackgroundAction.class);

    final SliceSources slice;

    final int bgValue;

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    public SetSliceBackgroundAction(MultiSlicePositioner mp,
                                    SliceSources slice,
                                    int bgValue) {
        super(mp);
        this.slice = slice;
        this.bgValue = bgValue;
    }

    @Override
    protected boolean run() {
        slice.pushSliceBg(bgValue);
        slice.sourcesChanged();
        getMP().stateHasBeenChanged();
        return true;
    }

    public String toString() {
        return "Set White Background (v="+this.bgValue+") " + slice.getActionState(this);
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        double size = 7.0*scale;
        if (scale<0.9) {
            g.setColor(ABBABdvViewPrefs.raster_small_scale);
        } else {
            switch (slice.getActionState(this)) {
                case "(done)":
                    g.setColor(ABBABdvViewPrefs.done);
                    break;
                case "(locked)":
                    g.setColor(ABBABdvViewPrefs.locked);
                    break;
                case "(pending)":
                    g.setColor(ABBABdvViewPrefs.pending);
                    break;
            }
        }
        g.fillOval((int) (px - size), (int) (py - size), (int) (2.0*size), (int) (2.0*size));
        g.setColor(ABBABdvViewPrefs.text_action_register);
        g.setFont(ABBABdvViewPrefs.action_font);
        g.drawString("W", (int) px - 4, (int) py + 5);
    }

    @Override
    protected boolean cancel() {
        slice.popSliceBg();
        slice.sourcesChanged();
        getMP().stateHasBeenChanged();
        return true;
    }

    public int getBgValue() {
        return this.bgValue;
    }
}