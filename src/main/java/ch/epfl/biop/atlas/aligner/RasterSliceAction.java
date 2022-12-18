package ch.epfl.biop.atlas.aligner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Performs registration set in registration tab to
 * a slice
 */
public class RasterSliceAction extends CancelableAction {

    protected static Logger logger = LoggerFactory.getLogger(RasterSliceAction.class);

    final SliceSources slice;

    final double voxelSpacingInMicrometer;
    final boolean interpolate;

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    public RasterSliceAction(MultiSlicePositioner mp,
                             SliceSources slice,
                             double voxelSpacingInMicrometer, boolean interpolate) {
        super(mp);
        this.slice = slice;
        this.voxelSpacingInMicrometer = voxelSpacingInMicrometer;
        this.interpolate = interpolate;
    }

    @Override
    protected boolean run() {
        slice.pushRasterSlice(voxelSpacingInMicrometer, interpolate);
        slice.sourcesChanged();
        getMP().stateHasBeenChanged();
        return true;
    }

    public boolean isValid() {
        return true;
    }

    public String toString() {
        return "Raster slice ("+voxelSpacingInMicrometer+" um)" + slice.getActionState(this);
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        double size = 7.0*scale;
        if (scale<0.9) {
            g.setColor(new Color(128, 128, 128, 200));
        } else
        switch (slice.getActionState(this)) {
            case "(done)":
                g.setColor(new Color(0, 255, 140, 200));
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
        slice.popRasterSlice();
        slice.sourcesChanged();
        getMP().stateHasBeenChanged();
        return true;
    }

    public double getVoxelSpacingInMicrometer() {
        return voxelSpacingInMicrometer;
    }
}