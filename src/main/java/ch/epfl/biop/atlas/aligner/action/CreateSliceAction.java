package ch.epfl.biop.atlas.aligner.action;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Creates a new Slice source
 */
public class CreateSliceAction extends CancelableAction {

    protected static Logger logger = LoggerFactory.getLogger(CreateSliceAction.class);

    final private List<SourceAndConverter<?>> sacs;
    private SliceSources sliceSource;
    final public double slicingAxisPosition;
    final public double zSliceThicknessCorrection;
    final public double zSliceShiftCorrection;

    public CreateSliceAction(MultiSlicePositioner mp, List<SourceAndConverter<?>> sacs, double slicingAxisPosition, double zSliceThicknessCorrection, double zSliceShiftCorrection) {
        super(mp);
        this.sacs = sacs;
        this.slicingAxisPosition = slicingAxisPosition;
        this.zSliceShiftCorrection = zSliceShiftCorrection;
        this.zSliceThicknessCorrection = zSliceThicknessCorrection;
        hide();
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    public List<SourceAndConverter<?>> getSacs() {
        return sacs;
    }

    @Override
    public boolean run() {
        return mp.runCreateSlice(this);
    }

    public SliceSources getSlice() {
        return sliceSource;
    }

    public String toString() {
        return "Slice Created " + new DecimalFormat("###.##").format(this.slicingAxisPosition);
    }

    @Override
    public boolean cancel() {
        return mp.cancelCreateSlice(this);
    }

    @Override
    public void drawAction(Graphics2D g, double px, double py, double scale) {

    }

    public void setSlice(SliceSources sliceSources) {
        this.sliceSource = sliceSources;
    }
}
