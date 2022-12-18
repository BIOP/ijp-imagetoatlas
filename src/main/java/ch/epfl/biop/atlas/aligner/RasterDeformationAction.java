package ch.epfl.biop.atlas.aligner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Performs registration set in registration tab to
 * a slice
 */
public class RasterDeformationAction extends CancelableAction {

    protected static Logger logger = LoggerFactory.getLogger(RasterDeformationAction.class);

    final SliceSources slice;

    final double gridSpacingInMicrometer;

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    public RasterDeformationAction(MultiSlicePositioner mp,
                                   SliceSources slice,
                                   double gridSpacingInMicrometer) {
        super(mp);
        this.slice = slice;
        this.gridSpacingInMicrometer = gridSpacingInMicrometer;
    }

    @Override
    protected boolean run() {
        slice.pushRasterDeformation(gridSpacingInMicrometer);
        slice.sourcesChanged();
        getMP().stateHasBeenChanged();
        return true;
    }

    public boolean isValid() {
        return true;
    }

    public String toString() {
        return "Raster transformation ("+gridSpacingInMicrometer+" um)" + slice.getActionState(this);
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        double size = 7.0*scale;
        if (scale<0.9) {
            g.setColor(new Color(128, 128, 128, 200));
        } else
        switch (slice.getActionState(this)) {
            case "(done)":
                g.setColor(new Color(217, 0, 255, 200));
                break;
            case "(locked)":
                g.setColor(new Color(205, 1, 106, 200));
                break;
            case "(pending)":
                g.setColor(new Color(255, 255, 0, 200));
                break;
        }
        {
            g.fillOval((int) (px - size), (int) (py - size), (int) (2.0*size), (int) (2.0*size));
        }
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("R", (int) px - 4, (int) py + 5);
    }

    @Override
    protected boolean cancel() {
        slice.popRasterDeformation();
        slice.sourcesChanged();
        getMP().stateHasBeenChanged();
        return true;
    }

    public double getGridSpacingInMicrometer() {
        return gridSpacingInMicrometer;
    }
}