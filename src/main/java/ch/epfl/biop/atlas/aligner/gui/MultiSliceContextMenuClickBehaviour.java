package ch.epfl.biop.atlas.aligner.gui;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.scijava.ui.behaviour.ClickBehaviour;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class MultiSliceContextMenuClickBehaviour implements ClickBehaviour {

    final MultiSlicePositioner mp;
    final Supplier<Collection<SliceSources>> slicesSupplier;

    public MultiSliceContextMenuClickBehaviour( MultiSlicePositioner mp, Supplier<Collection<SliceSources>> sourcesSupplier ) {
        this.mp = mp;
        this.slicesSupplier = sourcesSupplier;
    }

    @Override
    public void click(int x, int y) {
        showPopupMenu( x, y );
    }

    private void showPopupMenu(int x, int y) {
        final List<SliceSources> slices = new ArrayList<>(slicesSupplier.get());

        SliceSources[] sliceArray = new SliceSources[slices.size()];
        sliceArray = slices.toArray(sliceArray);

        final SliceSourcesPopupMenu popupMenu = new SliceSourcesPopupMenu(mp, sliceArray );

        popupMenu.getPopup().show( mp.getBdvh().getViewerPanel().getDisplay(), x, y );
    }

}
