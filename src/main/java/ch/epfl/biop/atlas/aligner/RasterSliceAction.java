package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvViewPrefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        g.setColor(ABBABdvViewPrefs.raster_action_string_color);
        g.setFont(ABBABdvViewPrefs.action_font);
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