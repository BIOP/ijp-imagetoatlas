package ch.epfl.biop.atlas.aligner.gui;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import org.scijava.ui.behaviour.ClickBehaviour;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public class MultiSliceContextMenuClickBehaviour implements ClickBehaviour {

    private MultiSlicePositioner mp;
    private Supplier<Collection<SliceSources>> slicesSupplier;
    private BdvMultislicePositionerView view;

    public MultiSliceContextMenuClickBehaviour(MultiSlicePositioner mp, BdvMultislicePositionerView view, Supplier<Collection<SliceSources>> sourcesSupplier ) {
        this.mp = mp;
        this.view = view;
        this.slicesSupplier = sourcesSupplier;
    }

    @Override
    public void click(int x, int y) {
        showPopupMenu( x, y );
    }

    public void clear() {
        this.mp = null;
        this.view = null;
        this.slicesSupplier = null;
    }

    private void showPopupMenu(int x, int y) {

        final SliceSourcesPopupMenu popupMenu = new SliceSourcesPopupMenu(mp, view, slicesSupplier);

        popupMenu.getPopup().show( view.getBdvh().getViewerPanel().getDisplay(), x, y );
    }

}
