package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.bdv.gui.CircleGraphicalHandle;
import ch.epfl.biop.bdv.gui.GraphicalHandle;
import ch.epfl.biop.bdv.gui.GraphicalHandleToolTip;
import ch.epfl.biop.registration.sourceandconverter.affine.AffineTransformedSourceWrapperRegistration;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SliceGuiState {

    final SliceSources slice;

    final int nChannels;

    final Object lockChangeDisplay = new Object();

    // Visible to the user in slicing mode
    private SourceAndConverter<?>[] sources_displayed; // For Positioning mode

    AffineTransformedSourceWrapperRegistration slicePositioner;

    final boolean[] channelVisible;

    boolean sliceIsVisibleUser = true; // Takes precedence over channelIsVisible

    private boolean sliceIsVisibleMode = true; // Equal precedence with sliceIsVisibleUser

    final BdvHandle bdvh;

    final BdvMultislicePositionerView view;

    // Display name of slice
    GraphicalHandleToolTip tt;

    final List<GraphicalHandle> ghs = new ArrayList<>();

    public SliceGuiState(BdvMultislicePositionerView view, SliceSources slice, BdvHandle bdvh) {
        this.view = view;
        this.bdvh = bdvh;
        this.slice = slice;
        this.nChannels = slice.getRegisteredSources().length;
        channelVisible = new boolean[nChannels];
        for (int i = 0; i < nChannels; i++) {
            channelVisible[i] = true;
        }
        slicePositioner = new AffineTransformedSourceWrapperRegistration();
        sources_displayed = slicePositioner.getTransformedImageMovingToFixed(slice.getRegisteredSources());
        SourceAndConverterHelper.transferColorConverters(slice.getRegisteredSources(), sources_displayed);

        GraphicalHandle gh = new CircleGraphicalHandle(view,
                new Behaviours(new InputTriggerConfig()),
                view.bdvh.getTriggerbindings(),
                this.toString(), // pray for unicity ? TODO : do better than thoughts and prayers
                this::getSliceHandleCoords,
                this::getBdvHandleRadius,
                this::getBdvHandleColor
        );

        tt = new GraphicalHandleToolTip(gh, slice::toString, -20, -10);
        ghs.add(gh);
    }

    private void show() {
        //synchronized (lockChangeDisplay) {
        if (sliceIsVisibleMode) {
            List<SourceAndConverter<?>> sourcesToDisplay = IntStream.range(0,nChannels)
                    .filter(idx -> channelVisible[idx])
                    .mapToObj(idx -> sources_displayed[idx])
                    .collect(Collectors.toList());

            SourceAndConverterServices
                    .getBdvDisplayService()
                    .show(bdvh, sourcesToDisplay.toArray(new SourceAndConverter[sourcesToDisplay.size()]));
        }
    }

    private void hide() {
        bdvh.getViewerPanel().state()
                .removeSources(Arrays.asList(sources_displayed));
    }

    public void created() {
        show();
    }

    public void deleted() {
        hide();
    }

    public void sourcesChanged() {
        hide();
        sources_displayed = slicePositioner.getTransformedImageMovingToFixed(slice.getRegisteredSources());
        show();
    }

    public void slicePositionChanged() {
        if (view.mode == BdvMultislicePositionerView.POSITIONING_MODE_INT) {
            AffineTransform3D slicingModePositionAffineTransform = new AffineTransform3D();
            RealPoint center = view.getSliceCenterPosition(slice);

            slicingModePositionAffineTransform.translate(
                    center.getDoublePosition(0), center.getDoublePosition(1), -slice.getSlicingAxisPosition());
            slicePositioner.setAffineTransform(slicingModePositionAffineTransform);
        } else {
            assert view.mode == BdvMultislicePositionerView.REVIEW_MODE_INT;
            slicePositioner.setAffineTransform(new AffineTransform3D());
        }
    }

    public void isCurrent() {
    }

    public void isNotCurrent() {
    }

    public Integer[] getSliceHandleCoords() {
        AffineTransform3D bdvAt3D = new AffineTransform3D();
        view.bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);
        RealPoint sliceCenter;
        if (view.mode == BdvMultislicePositionerView.POSITIONING_MODE_INT) {
            sliceCenter = view.getSliceCenterPosition(slice);
            bdvAt3D.apply(sliceCenter, sliceCenter);
            return new Integer[]{(int) sliceCenter.getDoublePosition(0), (int) sliceCenter.getDoublePosition(1), (int) sliceCenter.getDoublePosition(2)};
        } else if (view.mode == BdvMultislicePositionerView.REVIEW_MODE_INT) {
            RealPoint zero = new RealPoint(3);
            zero.setPosition(0, 0);
            bdvAt3D.apply(zero, zero);
            return new Integer[]{35 * (slice.getIndex() - view.getCurrentSliceIndex()) + (int) zero.getDoublePosition(0), 80, 0};
        } else {
            return new Integer[]{0, 0, 0};
        }
    }

    public Integer[] getBdvHandleColor() {
        if (slice.isSelected()) {
            return new Integer[]{0, 255, 0, 200};
        } else {
            return new Integer[]{255, 255, 0, 64};
        }
    }

    public Integer getBdvHandleRadius() {
        return 12;
    }


    public void drawGraphicalHandles(Graphics2D g) {
        ghs.forEach(gh -> gh.draw(g));
        if (view.getCurrentSlice().equals(slice)) {
            tt.draw(g);
        }
    }

    double yShift_slicing_mode = 0;
    public void setYShift(double yShift) {
        yShift_slicing_mode = yShift;
        slicePositionChanged();
    }

    public double getYShift() {
        return yShift_slicing_mode;
    }

    public void sliceDisplayChanged(int mode) {
        switch (mode) {
            case BdvMultislicePositionerView.NO_SLICE_DISPLAY_MODE:
                sliceIsVisibleMode = false;
                hide();
                break;
            case BdvMultislicePositionerView.CURRENT_SLICE_DISPLAY_MODE:
                sliceIsVisibleMode = view.isCurrentSlice(slice);
                if (sliceIsVisibleMode) {
                    show();
                } else {
                    hide();
                }
                break;
            case BdvMultislicePositionerView.ALL_SLICES_DISPLAY_MODE:
                sliceIsVisibleMode = true;
                if (sliceIsVisibleUser) show();
                break;
        }
    }

    public boolean isVisible() {
        // TODO
        return sliceIsVisibleMode;
    }
}
