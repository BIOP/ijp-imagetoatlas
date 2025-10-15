package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.SliceToImagePlus;
import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvViewPrefs;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ij.ImagePlus;

import java.awt.*;

public class ExportAtlasSliceToImagePlusAction extends CancelableAction {

    final SliceSources slice;
    final SourcesProcessor preprocess;
    final double px, py, sx, sy, pixel_size_mm;
    final int timepoint;
    final boolean interpolate;

    ImagePlus resultImage = null;

    public ExportAtlasSliceToImagePlusAction(MultiSlicePositioner mp,
                                             SliceSources slice,
                                             SourcesProcessor preprocess,
                                             double px, double py, double sx, double sy,
                                             double pixel_size_millimeter, int timepoint,
                                             boolean interpolate) {
        super(mp);
        this.slice = slice;
        this.preprocess = preprocess;
        this.px = px;
        this.py = py;
        this.sx = sx;
        this.sy = sy;
        this.pixel_size_mm = pixel_size_millimeter;
        this.timepoint = timepoint;
        this.interpolate = interpolate;
    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    @Override
    protected boolean run() {
        try {
            resultImage = SliceToImagePlus.exportAtlas(getMP(), slice, preprocess, px, py, sx, sy, pixel_size_mm, timepoint, interpolate);
        } catch (UnsupportedOperationException exception) {
            getMP().errorMessageForUser.accept("Could not export to ImageJ", exception.getMessage());
            return false;
        }
        return resultImage!=null;
    }

    @Override
    protected boolean cancel() {
        clean(); // Allows GC
        return true;
    }

    public ImagePlus getImagePlus() {
        return resultImage;
    }

    public void clean() {
        resultImage = null;
    }

    public boolean isValid() {
        return resultImage!=null;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        switch (slice.getActionState(this)) {
            case "(done)":
                g.setColor(ABBABdvViewPrefs.done_export);
                break;
            case "(locked)":
                g.setColor(ABBABdvViewPrefs.locked);
                break;
            case "(pending)":
                g.setColor(ABBABdvViewPrefs.pending);
                break;
        }
        g.fillRect((int) (px - 7), (int) (py - 7), 14, 14);
        g.setColor(ABBABdvViewPrefs.text_action_export_atlas_to_image_plus);
        g.setFont(ABBABdvViewPrefs.action_font);
        g.drawString("E", (int) px - 4, (int) py + 3);
    }

}
