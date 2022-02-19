package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.bdv.gui.graphicalhandle.CircleGraphicalHandle;
import ch.epfl.biop.bdv.gui.graphicalhandle.GraphicalHandle;
import ch.epfl.biop.bdv.gui.graphicalhandle.GraphicalHandleToolTip;
import ch.epfl.biop.bdv.gui.graphicalhandle.SquareGraphicalHandle;
import ch.epfl.biop.registration.sourceandconverter.affine.AffineTransformedSourceWrapperRegistration;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import spimdata.util.Displaysettings;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SliceGuiState {

    final SliceSources slice;

    final int nChannels;

    // Visible to the user in slicing mode
    private SourceAndConverter<?>[] sources_displayed; // For Positioning mode

    private SourceAndConverter<?>[] ini_sources; // For Positioning mode

    AffineTransformedSourceWrapperRegistration slicePositioner;

    final BdvHandle bdvh;

    final BdvMultislicePositionerView view;

    // Display name of slice
    GraphicalHandleToolTip tt;

    final List<GraphicalHandle> ghs = new ArrayList<>();

    SquareGraphicalHandle keyHandle;

    final boolean[] channelVisible;

    final Displaysettings[] displaysettings;

    boolean sliceVisible = false;

    double yShift = 0;

    double xShift = 0;

    List<FilterDisplay> displayFilters = new ArrayList<>();

    public SliceGuiState(BdvMultislicePositionerView view, SliceSources slice, BdvHandle bdvh) {
        this.view = view;
        this.bdvh = bdvh;
        this.slice = slice;
        this.nChannels = slice.getRegisteredSources().length;

        channelVisible = new boolean[nChannels];
        displaysettings = new Displaysettings[nChannels];
        ini_sources = slice.getRegisteredSources();

        this.addDisplayFilters(iChannel -> channelVisible[iChannel]);
        this.addDisplayFilters(iChannel -> sliceVisible);

        slicePositioner = new AffineTransformedSourceWrapperRegistration();
        sources_displayed = slicePositioner.getTransformedImageMovingToFixed(slice.getRegisteredSources());
        SourceAndConverterHelper.transferColorConverters(slice.getRegisteredSources(), sources_displayed);

        for (int i=0; i<nChannels; i++) {
            Displaysettings ds = new Displaysettings(-1);
            Displaysettings.GetDisplaySettingsFromCurrentConverter(sources_displayed[i], ds);
            displaysettings[i] = ds;
        }

        GraphicalHandle gh = new CircleGraphicalHandle(view,
                new Behaviours(new InputTriggerConfig()),
                view.bdvh.getTriggerbindings(),
                this.toString()+"_gh", // pray for unicity ? TODO : do better than thoughts and prayers
                this::getSliceHandleCoords,
                this::getBdvHandleRadius,
                this::getBdvHandleColor
        );

        tt = new GraphicalHandleToolTip(gh, slice::toString, -20, -10);
        ghs.add(gh);

        final Behaviours behavioursHandleSlice = new Behaviours(new InputTriggerConfig());
        behavioursHandleSlice.behaviour(new SliceDragBehaviour(view, slice),//mp.getSelectedSourceDragBehaviour(slice),
                "dragSelectedSources" + this.toString(), "button1");
        behavioursHandleSlice.behaviour((ClickBehaviour) (x, y) -> {
            slice.deSelect();
            view.getBdvh().getViewerPanel().requestRepaint();
        }, "deselectedSources" + this.toString(), "button3", "ctrl button1");


        keyHandle = new SquareGraphicalHandle(view,
                behavioursHandleSlice,
                view.getBdvh().getTriggerbindings(),
                this.toString()+"_keyHandle", // assumes unicity
                () -> {
                    AffineTransform3D bdvAt3D = new AffineTransform3D();
                    view.getBdvh().getViewerPanel().state().getViewerTransform(bdvAt3D);
                    RealPoint handlePoint = view.getDisplayedCenter(slice);
                    double yShift = getBdvHandleRadius();
                    handlePoint.setPosition(+view.msp.sY / 2.0, 1);
                    bdvAt3D.apply(handlePoint, handlePoint);
                    return new Integer[]{(int) handlePoint.getDoublePosition(0), (int) (handlePoint.getDoublePosition(1)+(slice.isSelected()?-0.6:+0.6)*yShift), (int) handlePoint.getDoublePosition(2)};
                },
                () -> {
                    if (slice.isKeySlice()) {
                        return getBdvHandleRadius();
                    } else {
                        return getBdvHandleRadius() / 2;
                    }
                },
                () -> {
                    if (slice.isKeySlice() && slice.isSelected()) {
                        return new Integer[]{255, 0, 255, 200};
                    } else {
                        return getBdvHandleColor();
                    }
                }
        );

        ghs.add(keyHandle);

    }

    public void setSliceVisibility(boolean visible) {
        if (visible!=sliceVisible) {
            sliceVisible = visible;
            sliceDisplayChanged();
        }
    }

    public boolean getSliceVisibility() {
        return sliceVisible;
    }

    public void setChannelVisibility(int channel, boolean visible) {
        assert channel>=0;
        if (channel<nChannels) {
            if (channelVisible[channel]!=visible) {
                channelVisible[channel] = visible;
                sliceDisplayChanged();
            }
        }
    }

    public boolean getChannelVisibility(int channel) {
        assert channel>=0;
        if (channel<nChannels) {
            return channelVisible[channel];
        } else {
            return false;
        }
    }

    public void setDisplaySettings(int channel, Displaysettings ds) {
        if (channel<nChannels) {
            displaysettings[channel] = ds;
            sliceDisplayChanged();
        }
    }

    public Displaysettings getDisplaySettings(int channel) {
        if (channel<nChannels) {
            return displaysettings[channel];
        } else {
            return new Displaysettings(-1);
        }
    }

    private boolean currentlyVisible(int idxChannel) {
        for (FilterDisplay fd : displayFilters) {
            if (!fd.displayChannel(idxChannel)) {
                return false;
            }
        }
        return true;
    }

    private void show() {
        List<SourceAndConverter<?>> sourcesToDisplay = IntStream.range(0,nChannels)
                .filter(this::currentlyVisible)
                .mapToObj(idx -> {
                    Displaysettings.applyDisplaysettings(sources_displayed[idx], displaysettings[idx]);
                    return sources_displayed[idx];
                })
                .collect(Collectors.toList());

        SourceAndConverter[] sources = sourcesToDisplay.toArray(new SourceAndConverter[sourcesToDisplay.size()]);

        if (sources.length>0) {
            SourceAndConverterServices
                    .getBdvDisplayService()
                    .show(bdvh, sources);
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
        for (int idx = 0; idx<nChannels; idx++) {
            Displaysettings.applyDisplaysettings(ini_sources[idx], displaysettings[idx]);
            Displaysettings.applyDisplaysettings(slice.getRegisteredSources()[idx], displaysettings[idx]);
        }
        sources_displayed = slicePositioner.getTransformedImageMovingToFixed(slice.getRegisteredSources());
        show();
    }

    public void slicePositionChanged() {
        if (view.mode == BdvMultislicePositionerView.POSITIONING_MODE_INT) {
            AffineTransform3D slicingModePositionAffineTransform = new AffineTransform3D();
            RealPoint center = view.getDisplayedCenter(slice);

            slicingModePositionAffineTransform.translate(
                    center.getDoublePosition(0), center.getDoublePosition(1), -slice.getSlicingAxisPosition());
            slicePositioner.setAffineTransform(slicingModePositionAffineTransform);
        } else {
            assert view.mode == BdvMultislicePositionerView.REVIEW_MODE_INT;
            slicePositioner.setAffineTransform(new AffineTransform3D());
        }
    }

    public void isCurrent() {
        sliceDisplayChanged();
    }

    public void isNotCurrent() {
        sliceDisplayChanged();
    }

    public Integer[] getSliceHandleCoords() {
        AffineTransform3D bdvAt3D = new AffineTransform3D();
        view.bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);
        RealPoint sliceCenter;
        if (view.mode == BdvMultislicePositionerView.POSITIONING_MODE_INT) {
            sliceCenter = view.getDisplayedCenter(slice);
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
            if (slice.isKeySlice()) {
                return new Integer[]{0, 255, 0, 255};
            } else {
                return new Integer[]{0, 255, 0, 180};
            }
        } else {
            if (slice.isKeySlice()) {
                return new Integer[]{255, 255, 0, 128};
            } else {
                return new Integer[]{255, 255, 0, 64};
            }
        }
    }

    public Integer getBdvHandleRadius() {
        if (slice.isKeySlice()) {
            return 16;
        } else {
            return 12;
        }
    }

    public void drawGraphicalHandles(Graphics2D g) {
        ghs.forEach(gh -> gh.draw(g));
        if (view.getCurrentSlice().equals(slice)) {
            tt.draw(g);
        }
    }

    public void setYShift(double yShift) {
        this.yShift = yShift;
        slicePositionChanged();
    }

    public double getYShift() {
        return yShift;
    }

    public void setXShift(double xShift) {
        this.xShift = xShift;
        slicePositionChanged();
    }

    public double getXShift() {
        return xShift;
    }

    public void disableGraphicalHandles() {
        keyHandle.disable();
    }

    public void enableGraphicalHandles() {
        keyHandle.enable();
    }

    public void setDisplayedAxisPosition(double displayedAxisPosition) {
        setXShift(displayedAxisPosition-slice.getSlicingAxisPosition());
    }

    public void addDisplayFilters(FilterDisplay filterDisplay) {
        displayFilters.add(filterDisplay);
    }

    public void sliceDisplayChanged() {
        // TODO : improve by not doing anything is the slices displayed are not changed
        hide();
        for (int idx = 0; idx<nChannels; idx++) {
            Displaysettings.applyDisplaysettings(ini_sources[idx], displaysettings[idx]);
            Displaysettings.applyDisplaysettings(slice.getRegisteredSources()[idx], displaysettings[idx]);
        }
        show();
    }

    public void removeDisplayFilters(FilterDisplay fd) {
        displayFilters.remove(fd);
    }

    public void setState(State state) {
        hide();
        assert state.channelVisible.length == nChannels;
        for (int i = 0; i< nChannels; i++) {
            channelVisible[i] = state.channelVisible[i];
            displaysettings[i] = state.displaysettings[i];
        }
        sliceVisible = state.sliceVisible;
        show();
    }

    public interface FilterDisplay {
        boolean displayChannel(int iChannel);
    }

    public static class State {

        final boolean[] channelVisible;

        final Displaysettings[] displaysettings;

        boolean sliceVisible;

        public State(SliceGuiState state) {
            this.channelVisible = state.channelVisible;
            this.sliceVisible = state.sliceVisible;
            this.displaysettings = state.displaysettings;
        }
    }
}
