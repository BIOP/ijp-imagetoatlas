package ch.epfl.biop.atlastoimg2d.multislice;

import java.util.List;
import java.util.stream.Collectors;

public class DeleteLastRegistration extends CancelableAction {

    private final SliceSources sliceSource;

    final RegisterSlice rs;

    public DeleteLastRegistration(MultiSlicePositioner mp, SliceSources slice) {
        super(mp);
        this.sliceSource = slice;
        List<CancelableAction> actions = mp.userActions.stream()
                .filter(action -> action.getSliceSources() == slice)
                .filter(action -> action instanceof RegisterSlice)
                .collect(Collectors.toList());

        if (actions.size() == 0) {
            rs = null;
        } else {
            rs = (RegisterSlice) actions.get(actions.size()-1);
        }

        mp.mso.hide(this);
    }

    public boolean isValid() {
        return rs != null;
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    @Override
    protected boolean run() {
        mp.mso.hide(rs);
        return rs.cancel();
    }

    @Override
    protected boolean cancel() {
        mp.mso.unhide(rs);
        return rs.run();
    }

}
