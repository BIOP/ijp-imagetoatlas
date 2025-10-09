package ch.epfl.biop.abba.actionexample;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvViewPrefs;
import ij.IJ;
import ij.gui.Roi;
import net.imglib2.realtransform.RealTransform;

import java.awt.Graphics2D;
import java.awt.Color;
import java.util.List;

public class PrintTheNumberOfRoisAction extends CancelableAction {

    final SliceSources slice;
    final String namingChoice;

    /**
     * Provides the reference to the MultiSlicePositioner
     *
     * @param mp multipositioner input
     * @param slice the slice to work on
     */
    public PrintTheNumberOfRoisAction(MultiSlicePositioner mp,
                                      SliceSources slice,
                                      String namingChoice) {
        super(mp);
        this.slice = slice;
        this.namingChoice = namingChoice;
    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    @Override
    protected boolean run() {
        RealTransform realTransform = slice.getSlicePixToCCFRealTransform(); // This will be important to save somewhere
        List<Roi> rois = slice.getRois(namingChoice);
        IJ.log("The slice "+slice+" contains "+rois.size()+" rois.");
        return true; // Return true if everything works fine
    }

    @Override
    protected boolean cancel() {
        return true; // Return true if the cancellation worked fine, here there's nothing to cancel, so it returns true always
    }

    @Override
    public void drawAction(Graphics2D g, double px, double py, double scale) {
        // You can draw a little something to show the action in progress,
        //
        // For instance:
        switch (slice.getActionState(this)){ // Will change depending on the state of this action
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
        g.fillOval((int) (px - 7), (int) (py - 7), 14, 14);
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("E", (int) px - 4, (int) py + 5);
    }
}
