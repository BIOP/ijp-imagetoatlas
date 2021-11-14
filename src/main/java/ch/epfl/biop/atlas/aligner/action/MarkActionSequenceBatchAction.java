package ch.epfl.biop.atlas.aligner.action;


import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;

import java.awt.*;

/**
 * Action Class : Marks begin and end of a series of actions
 * that should be cancelled together.
 */
public class MarkActionSequenceBatchAction extends CancelableAction {

    public MarkActionSequenceBatchAction(MultiSlicePositioner mp) {
        super(mp);
        hide();
    }

    @Override
    public SliceSources getSliceSources() {
        return null;
    }

    @Override
    public boolean run() {
        return true;
    }

    @Override
    public boolean cancel() {
        return true;
    }

    @Override
    public void drawAction(Graphics2D g, double px, double py, double scale) {

    }
}
