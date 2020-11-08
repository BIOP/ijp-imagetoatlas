package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeleteSlice extends CancelableAction {

    private final SliceSources sliceSource;
    private final List<SourceAndConverter<?>> sacs;

    final CreateSlice cs;
    List<CancelableAction> savedActions = new ArrayList<>();

    public DeleteSlice(MultiSlicePositioner mp, SliceSources slice) {
        super(mp);
        this.sliceSource = slice;
        this.sacs = Arrays.asList(slice.original_sacs);
        cs = new CreateSlice(mp, sacs, slice.slicingAxisPosition);
        cs.setSliceSource(sliceSource);
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    @Override
    protected boolean run() {
        return cs.cancel();
    }

    @Override
    protected boolean cancel() {
        return cs.run();
    }

}
