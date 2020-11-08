package ch.epfl.biop.atlas.aligner;


/**
 * Action Class : Marks begin and end of a series of actions
 * that should be cancelled together.
 */
public class MarkActionSequenceBatch extends CancelableAction {

    public MarkActionSequenceBatch(MultiSlicePositioner mp) {
        super(mp);
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
}
