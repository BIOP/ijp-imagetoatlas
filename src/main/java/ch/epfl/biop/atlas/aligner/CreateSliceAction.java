package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
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
    protected boolean run() {
        getMP().addTask();
        boolean result = getMP().runCreateSlice(this);
        getMP().stateHasBeenChanged();
        getMP().removeTask();
        return result;
    }

    public SliceSources getSlice() {
        return sliceSource;
    }

    public String toString() {
        return "Slice Created " + new DecimalFormat("###.##").format(this.slicingAxisPosition);
    }

    @Override
    protected boolean cancel() {
        getMP().addTask();
        boolean result = getMP().cancelCreateSlice(this);
        getMP().stateHasBeenChanged();
        getMP().removeTask();
        return result;
    }

    @Override
    public void drawAction(Graphics2D g, double px, double py, double scale) {

    }

    public void setSlice(SliceSources sliceSources) {
        this.sliceSource = sliceSources;
    }
}
