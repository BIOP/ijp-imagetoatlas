package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.DeformationFieldToImagePlus;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvViewPrefs;
import ij.ImagePlus;

import java.awt.*;

public class ExportDeformationFieldToImagePlusAction extends CancelableAction {

    final SliceSources slice;
    final int timepoint, resolutionLevel, maxIteration, downsample;
    final double toleranceInMm;

    ImagePlus resultImage = null;

    public ExportDeformationFieldToImagePlusAction(MultiSlicePositioner mp,
                                                   SliceSources slice,
                                                   int resolutionLevel,
                                                   int downsample,
                                                   int timepoint,
                                                   double toleranceInMm,
                                                   int maxIteration) {
        super(mp);
        this.slice = slice;
        this.timepoint = timepoint;
        this.resolutionLevel = resolutionLevel;
        this.toleranceInMm = toleranceInMm;
        this.maxIteration = maxIteration;
        this.downsample = downsample;
    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

    @Override
    protected boolean run() {
        resultImage = DeformationFieldToImagePlus.export(slice,resolutionLevel, downsample, timepoint,toleranceInMm, maxIteration);
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
        g.setColor(ABBABdvViewPrefs.text_action_export_deformation_field_action);
        g.setFont(ABBABdvViewPrefs.action_font);
        g.drawString("E", (int) px - 4, (int) py + 3);
    }

}
