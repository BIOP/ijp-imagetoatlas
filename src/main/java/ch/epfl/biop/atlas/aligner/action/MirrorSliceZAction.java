package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;

import java.awt.*;

/**
 * Mirrors a slice along z, in its own frame: the z part of a flip around X or Y, whose in-plane part
 * is a registration. Hidden, and not serialized: the saved pre-transform already holds its result.
 */
public class MirrorSliceZAction extends CancelableAction {

    private final SliceSources sliceSource;

    public MirrorSliceZAction(MultiSlicePositioner mp, SliceSources sliceSource) {
        super(mp);
        this.sliceSource = sliceSource;
        hide();
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    protected boolean run() {
        sliceSource.mirrorZ();
        return true;
    }

    public String toString() {
        return "Mirror Z";
    }

    protected boolean cancel() {
        sliceSource.mirrorZ(); // a mirror is its own inverse
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {

    }

}
