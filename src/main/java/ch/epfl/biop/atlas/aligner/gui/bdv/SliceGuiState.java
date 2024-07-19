package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.BdvHandle;
import bdv.util.source.alpha.IAlphaSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.render.DefaultMipmapOrdering;
import bdv.viewer.render.MipmapOrdering;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.bdv.gui.graphicalhandle.CircleGraphicalHandle;
import ch.epfl.biop.bdv.gui.graphicalhandle.GraphicalHandle;
import ch.epfl.biop.bdv.gui.graphicalhandle.GraphicalHandleToolTip;
import ch.epfl.biop.bdv.gui.graphicalhandle.SquareGraphicalHandle;
import ch.epfl.biop.registration.sourceandconverter.affine.AffineTransformedSourceWrapperRegistration;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.NumericType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import spimdata.util.Displaysettings;

import java.awt.Graphics2D;
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

    private final SourceAndConverter<?>[] ini_sources; // For Positioning mode

    final AffineTransformedSourceWrapperRegistration slicePositioner;

    final BdvHandle bdvh;

    final BdvMultislicePositionerView view;

    // Display name of slice
    final GraphicalHandleToolTip tt;

    final List<GraphicalHandle> ghs = new ArrayList<>();

    final SquareGraphicalHandle keyHandle;

    final boolean[] channelVisible;

    final Displaysettings[] displaysettings;

    boolean sliceVisible = false;

    double yShift = 0;

    double xShift = 0;

    final List<FilterDisplay> displayFilters = new ArrayList<>();

    public SliceGuiState(BdvMultislicePositionerView view, SliceSources slice, BdvHandle bdvh) {
        this.view = view;
        this.bdvh = bdvh;
        this.slice = slice;
        SourceAndConverter[] iniSources = getRegisteredSourcesAtStep(stepBack);
        this.nChannels = iniSources.length;

        channelVisible = new boolean[nChannels];
        displaysettings = new Displaysettings[nChannels];
        ini_sources = iniSources;

        this.addDisplayFilters(iChannel -> channelVisible[iChannel]);
        this.addDisplayFilters(iChannel -> sliceVisible);

        slicePositioner = new AffineTransformedSourceWrapperRegistration();
        sources_displayed = slicePositioner.getTransformedImageMovingToFixed(iniSources);
        SourceAndConverterHelper.transferColorConverters(iniSources, sources_displayed);

        for (int i=0; i<nChannels; i++) {
            Displaysettings ds = new Displaysettings(-1);
            Displaysettings.GetDisplaySettingsFromCurrentConverter(sources_displayed[i], ds);
            displaysettings[i] = ds;
        }

        GraphicalHandle gh = new CircleGraphicalHandle(view,
                new Behaviours(new InputTriggerConfig()),
                view.bdvh.getTriggerbindings(),
                this+"_gh", // pray for unicity ? TODO : do better than thoughts and prayers
                this::getSliceHandleCoords,
                this::getBdvHandleRadius,
                this::getBdvHandleColor
        );

        tt = new GraphicalHandleToolTip(gh, slice::toString, -20, -10);
        ghs.add(gh);

        final Behaviours behavioursHandleSlice = new Behaviours(new InputTriggerConfig());
        behavioursHandleSlice.behaviour(new SliceDragBehaviour(view, slice),//mp.getSelectedSourceDragBehaviour(slice),
                "dragSelectedSources" + this, "button1");
        behavioursHandleSlice.behaviour((ClickBehaviour) (x, y) -> {
            slice.deSelect();
            view.getBdvh().getViewerPanel().requestRepaint();
        }, "deselectedSources" + this, "button3", "ctrl button1");


        keyHandle = new SquareGraphicalHandle(view,
                behavioursHandleSlice,
                view.getBdvh().getTriggerbindings(),
                this+"_keyHandle", // assumes unicity
                () -> {
                    AffineTransform3D bdvAt3D = new AffineTransform3D();
                    view.getBdvh().getViewerPanel().state().getViewerTransform(bdvAt3D);
                    RealPoint handlePoint = view.getDisplayedCenter(slice);
                    double yShift = getBdvHandleRadius();
                    handlePoint.setPosition(view.msp.sY / 2.0, 1);
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

        for (SourceAndConverter<?> source:sources) {
            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .register(source, "no tree");
        }

        if (sources.length>0) {
            SourceAndConverterServices
                    .getBdvDisplayService()
                    .show(bdvh, sources);
        }

        bdvh.getViewerPanel().state().addSources(sourcesToDisplay);
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
        ghs.forEach(GraphicalHandle::disable);
    }

    int stepBack = 0;

    public void setRegistrationStepBack(int stepBack) {
        if (stepBack<0) stepBack = 0;
        if (this.stepBack!=stepBack) {
            this.stepBack = stepBack;
            sourcesChanged();
        }
    }

    public int getRegistrationStepBack() {
        return stepBack;
    }

    private SourceAndConverter[] getRegisteredSourcesAtStep(int stepBack) {
        //slice.getNumberOfRegistrations()-stepBack
        return slice.getRegisteredSources(stepBack);
    }

    public void sourcesChanged() {
        hide();
        bdvh.getViewerPanel().repaint();
        SourceAndConverter[] displayedSources = alphaCulledSources(getRegisteredSourcesAtStep(stepBack), slice.getAlpha());
        //SourceAndConverter[] displayedSources = getRegisteredSourcesAtStep(stepBack);
        for (int idx = 0; idx<nChannels; idx++) {
            Displaysettings.applyDisplaysettings(ini_sources[idx], displaysettings[idx]);
            Displaysettings.applyDisplaysettings(displayedSources[idx], displaysettings[idx]);
        }
        sources_displayed = slicePositioner.getTransformedImageMovingToFixed(displayedSources);
        show();
        bdvh.getViewerPanel().repaint();
    }

    public void slicePositionChanged() {
        if (view.mode == BdvMultislicePositionerView.POSITIONING_MODE_INT) {
            AffineTransform3D slicingModePositionAffineTransform = new AffineTransform3D();
            RealPoint center = view.getDisplayedCenter(slice);

            slicingModePositionAffineTransform.translate(
                    center.getDoublePosition(0), center.getDoublePosition(1), -slice.getSlicingAxisPosition());
            slicePositioner.setAffineTransform(slicingModePositionAffineTransform); // Concurrent modification possible. TODO : fic
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
        // TODO : improve by not doing anything if the slices displayed are not changed
        hide();
        for (int idx = 0; idx<nChannels; idx++) {
            Displaysettings.applyDisplaysettings(ini_sources[idx], displaysettings[idx]);
            Displaysettings.applyDisplaysettings(getRegisteredSourcesAtStep(stepBack)[idx], displaysettings[idx]);
        }
        show();
    }

    public void removeDisplayFilters(FilterDisplay fd) {
        displayFilters.remove(fd);
    }

    public void setState(State state) {
        hide();
        if (state.channelVisible.length == nChannels) {
            for (int i = 0; i< nChannels; i++) {
                channelVisible[i] = state.channelVisible[i];
                displaysettings[i] = state.displaysettings[i];
            }
            sliceVisible = state.sliceVisible;
            //show();
            slice.setDisplaySettings(displaysettings);
        }
    }

    public void updateDisplaySettings() {

        for (int i=0; i<nChannels; i++) {
            Displaysettings ds = new Displaysettings(-1);
            Displaysettings.GetDisplaySettingsFromCurrentConverter(slice.getRegisteredSources()[i], ds);
            displaysettings[i] = ds;
        }

        this.sliceDisplayChanged();
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

    private static SourceAndConverter[] alphaCulledSources(SourceAndConverter[] sources, IAlphaSource alpha) {
        SourceAndConverter[] alphaCulled = new SourceAndConverter[sources.length];
        for (int i = 0; i<alphaCulled.length; i++) {
            SourceAndConverter ori = sources[i];
            if (ori.asVolatile()!=null) {
                SourceAndConverter sac = new SourceAndConverter(
                        new AlphaCulledSource(ori.getSpimSource(), alpha),
                        ori.getConverter(),
                        new SourceAndConverter(new AlphaCulledSource(ori.asVolatile().getSpimSource(), alpha),
                                ori.asVolatile().getConverter()));
                alphaCulled[i] = sac;
            } else {
                SourceAndConverter sac = new SourceAndConverter(
                        new AlphaCulledSource(ori.getSpimSource(), alpha),
                        ori.getConverter());
                alphaCulled[i] = sac;
            }
        }
        return alphaCulled;
    }

    /*
     * Trick to use the tiled rendering with bigdataviewer-core 10.4+ and warped sources
     * @param <T>
     */
    public static class AlphaCulledSource<T extends NumericType<T>> implements Source<T>, MipmapOrdering {

        final Source<T> origin;
        final IAlphaSource alpha;

        public AlphaCulledSource(Source<T> origin, IAlphaSource alpha) {
            this.origin = origin;
            this.alpha = alpha;
            sourceMipmapOrdering = MipmapOrdering.class.isInstance( origin ) ?
                    ( MipmapOrdering ) origin : new DefaultMipmapOrdering( origin );
        }

        @Override
        public boolean isPresent(int t) {
            return origin.isPresent(t);
        }

        @Override
        public RandomAccessibleInterval<T> getSource(int t, int level) {
            return (RandomAccessibleInterval<T>) alpha.getSource(t,level); // WRONG! But only the interval is used
        }

        @Override
        public RealRandomAccessible<T> getInterpolatedSource(int t, int level, Interpolation interpolation) {
            AffineTransform3D tOri = new AffineTransform3D();
            AffineTransform3D tAlpha = new AffineTransform3D();
            origin.getSourceTransform(t,level,tOri);
            alpha.getSourceTransform(t,level,tAlpha);
            tOri.preConcatenate(tAlpha.inverse());
            return RealViews.affine(origin.getInterpolatedSource(t,level,interpolation),tOri);
        }

        @Override
        public void getSourceTransform(int t, int level, AffineTransform3D affineTransform3D) {
            alpha.getSourceTransform(t,level,affineTransform3D);
        }

        @Override
        public T getType() {
            return origin.getType();
        }

        @Override
        public String getName() {
            return origin.getName();
        }

        @Override
        public VoxelDimensions getVoxelDimensions() {
            return origin.getVoxelDimensions();
        }

        @Override
        public int getNumMipmapLevels() {
            return origin.getNumMipmapLevels();
        }

        @Override
        public boolean doBoundingBoxCulling()
        {
            return true;
        }

        /**
         * This is either the itself, if it implements
         * {@link MipmapOrdering}, or a {@link DefaultMipmapOrdering}.
         */
        private final MipmapOrdering sourceMipmapOrdering;

        @Override
        public MipmapHints getMipmapHints(AffineTransform3D screenTransform, int timepoint, int previousTimepoint) {
            return sourceMipmapOrdering.getMipmapHints( screenTransform, timepoint, previousTimepoint );
        }
    }

}
