package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvViewPrefs;

import java.awt.Graphics2D;

/**
 * Moves a slice to a new position along the slicing axis
 */
public class UnMirrorSliceAction extends CancelableAction {

    private final SliceSources sliceSource;

    public UnMirrorSliceAction(MultiSlicePositioner mp, SliceSources sliceSource) {
        super(mp);
        this.sliceSource = sliceSource;
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    boolean isValid = true;
    // For serialization

    protected boolean run() {
        isValid = sliceSource.hideLastMirrorRegistration();
        mp.slicePreTransformChanged(sliceSource); // Should update GUI
        return isValid;
    }

    public String toString() {
        return "UnMirror";
    }

    protected boolean cancel() {
        boolean success = sliceSource.restoreLastMirrorRegistration();
        mp.slicePreTransformChanged(sliceSource); // Should update GUI
        return success;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        //g.drawString("UnMirror", (int) px - 5, (int) py + 5);//+new DecimalFormat("###.##").format(newSlicingAxisPosition), (int) px-5, (int) py+5);
        double size = 7.0*scale;
        if (isValid) {
            if (scale<0.9) {
                g.setColor(ABBABdvViewPrefs.unmirror_small_scale);
            } else
                switch (sliceSource.getActionState(this)) {
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

            g.fillRect((int) (px - size), (int) (py - size), (int) (2.0*size), (int) (2.0*size));
            g.setColor(ABBABdvViewPrefs.text_action_register);
            g.drawString("R", (int) px - 4, (int) py + 5);
        } else {
            g.setColor(ABBABdvViewPrefs.invalid_action_string_color);
            g.drawString("X", (int) px - 4, (int) py + 5);
        }


    }

}