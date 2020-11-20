package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.registration.sourceandconverter.affine.AffineTransformedSourceWrapperRegistration;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import spimdata.util.Displaysettings;
import spimdata.util.DisplaysettingsHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Everything related to SliceSources display and its synchronization
 *
 *
 *
 */

public class SliceSourcesGUIState {

    final SliceSources slice;

    final int nChannels;

    List<GraphicalHandle> ghs = new ArrayList<>();

    MultiSlicePositioner mp;

    final Object lockChangeDisplay = new Object();

    Behaviours behavioursHandleSlice;

    boolean[] channelVisible;

    boolean sliceIsVisible = true; // Takes precedence over channelIsVisible

    AffineTransformedSourceWrapperRegistration slicingModePositioner;

    double yShift_slicing_mode = 0;

    // Visible to the user in slicing mode
    //private SourceAndConverter<?>[] relocated_sacs_positioning_mode; // For Positioning mode


    //volatile AffineTransformedSourceWrapperRegistration slicingModePositioner;

    public SliceSourcesGUIState(SliceSources slice, MultiSlicePositioner mp) {
        this.mp = mp;
        this.nChannels = slice.getRegisteredSources().length;
        this.slice = slice;
        channelVisible = new boolean[nChannels];
        System.out.println("nChannels = "+nChannels);
        for (int i=0;i<nChannels;i++) {
            channelVisible[i] = true;
        }
        sources_displayed_or_readyfordisplay = slice.original_sacs;
        SourceAndConverterUtils.transferColorConverters(slice.original_sacs, sources_displayed_or_readyfordisplay);

        behavioursHandleSlice = new Behaviours(new InputTriggerConfig());
        behavioursHandleSlice.behaviour(mp.getSelectedSourceDragBehaviour(slice), "dragSelectedSources" + this.toString(), "button1");
        behavioursHandleSlice.behaviour((ClickBehaviour) (x, y) -> {
            slice.deSelect();
            mp.bdvh.getViewerPanel().requestRepaint();
        }, "deselectedSources" + this.toString(), "button3", "ctrl button1");

        GraphicalHandle gh = new CircleGraphicalHandle(mp,
                behavioursHandleSlice,
                mp.bdvh.getTriggerbindings(),
                this.toString(), // pray for unicity ? TODO : do better than thoughts and prayers
                this::getBdvHandleCoords,
                this::getBdvHandleRadius,
                this::getBdvHandleColor
        );

        ghs.add(gh);
    }

    // Visible to the user in slicing mode
    private SourceAndConverter<?>[] sacs_registration_mode; // For Registration mode

    // Visible to the user in slicing mode
    private SourceAndConverter<?>[] relocated_sacs_positioning_mode; // For Positioning mode

    // Visible to the user in slicing mode
    private SourceAndConverter<?>[] sources_displayed_or_readyfordisplay; // For Positioning mode

    /*public synchronized SourceAndConverter<?>[] getSourcesRegistrationMode() {
        return sacs_registration_mode;
    }

    public synchronized SourceAndConverter<?>[] getSourcesPositioningMode() {
        return relocated_sacs_positioning_mode;
    }*/

    protected void sourcesChanged() {
        synchronized (slice) {

            synchronized (lockChangeDisplay) {

                mp.bdvh.getViewerPanel().state().removeSources(Arrays.asList(sources_displayed_or_readyfordisplay));

                sacs_registration_mode = slice.getRegisteredSources();

                slicingModePositioner = new AffineTransformedSourceWrapperRegistration();
                slicingModePositioner.setMovingImage(sacs_registration_mode);
                relocated_sacs_positioning_mode = slicingModePositioner.getTransformedImageMovingToFixed(sacs_registration_mode);

                positionChanged();

                SourceAndConverterUtils.transferColorConverters(sources_displayed_or_readyfordisplay, sacs_registration_mode);
                SourceAndConverterUtils.transferColorConverters(sources_displayed_or_readyfordisplay, relocated_sacs_positioning_mode);

                switch (mp.displayMode) {
                    case MultiSlicePositioner.POSITIONING_MODE_INT:
                        sources_displayed_or_readyfordisplay = relocated_sacs_positioning_mode;
                        break;
                    case MultiSlicePositioner.REGISTRATION_MODE_INT:
                        sources_displayed_or_readyfordisplay = sacs_registration_mode;
                        break;
                    default:
                        sources_displayed_or_readyfordisplay = sacs_registration_mode;
                        break;
                }

                updateDisplayedChannels();

            }

        }
    }

    private void updateDisplayedChannels() {

        if ((sliceIsVisible)&&(mp.displayMode!=MultiSlicePositioner.NO_SLICE_DISPLAY_MODE)) {
            List<SourceAndConverter<?>> sourcesToDisplay = IntStream.of(nChannels-1)
                    .filter(idx -> channelVisible[idx])
                    .mapToObj(idx -> sources_displayed_or_readyfordisplay[idx])
                    .collect(Collectors.toList());

            SourceAndConverterServices
                    .getSourceAndConverterDisplayService()
                    .show(mp.bdvh, sourcesToDisplay.toArray(new SourceAndConverter[sourcesToDisplay.size()]));

            mp.bdvh.getViewerPanel().requestRepaint();
        }
    }

    protected void modeChanged() {

        synchronized (slice) {
            synchronized (lockChangeDisplay) {
                switch (mp.displayMode) {
                    case MultiSlicePositioner.NO_SLICE_DISPLAY_MODE:
                        mp.bdvh.getViewerPanel().state().removeSources(Arrays.asList(sources_displayed_or_readyfordisplay));
                        break;
                    case MultiSlicePositioner.POSITIONING_MODE_INT:
                        if (sources_displayed_or_readyfordisplay != relocated_sacs_positioning_mode) {
                            mp.bdvh.getViewerPanel().state().removeSources(Arrays.asList(sources_displayed_or_readyfordisplay));
                            SourceAndConverterUtils.transferColorConverters(sources_displayed_or_readyfordisplay, relocated_sacs_positioning_mode);
                            sources_displayed_or_readyfordisplay = relocated_sacs_positioning_mode;
                        }
                        break;
                    case MultiSlicePositioner.REGISTRATION_MODE_INT:
                        if (sources_displayed_or_readyfordisplay != sacs_registration_mode) {
                            mp.bdvh.getViewerPanel().state().removeSources(Arrays.asList(sources_displayed_or_readyfordisplay));
                            SourceAndConverterUtils.transferColorConverters(sources_displayed_or_readyfordisplay, sacs_registration_mode);
                            sources_displayed_or_readyfordisplay = sacs_registration_mode;
                        }
                        break;
                }

                updateDisplayedChannels();

            }
        }
    }

    public RealPoint getCenterPositionPMode() {
        double slicingAxisSnapped = (((int) ((slice.getSlicingAxisPosition()) / mp.sizePixX)) * mp.sizePixX);
        double posX = (slicingAxisSnapped / mp.sizePixX * mp.sX / mp.reslicedAtlas.getStep()) + 0.5 * (mp.sX);
        double posY = mp.sY * yShift_slicing_mode;
        return new RealPoint(posX, posY, 0);
    }

    public RealPoint getCenterPositionRMode() {
        return new RealPoint(0, 0, slice.getSlicingAxisPosition());
    }

    protected Integer[] getBdvHandleCoords() {
        AffineTransform3D bdvAt3D = new AffineTransform3D();
        mp.bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);
        RealPoint sliceCenter;
        if (mp.getDisplayMode() == MultiSlicePositioner.POSITIONING_MODE_INT) {
            sliceCenter = getCenterPositionPMode();
            bdvAt3D.apply(sliceCenter, sliceCenter);
            return new Integer[]{(int) sliceCenter.getDoublePosition(0), (int) sliceCenter.getDoublePosition(1), (int) sliceCenter.getDoublePosition(2)};
        } else if (mp.getDisplayMode() == MultiSlicePositioner.REGISTRATION_MODE_INT) {
            RealPoint zero = new RealPoint(3);
            zero.setPosition(0, 0);
            bdvAt3D.apply(zero, zero);
            return new Integer[]{35 * (slice.getIndex() - mp.slices.size() / 2) + (int) zero.getDoublePosition(0), 20, 0};
        } else {
            return new Integer[]{0, 0, 0};
        }
    }

    public void hide() {
        synchronized (lockChangeDisplay) {
            if (sliceIsVisible) {
                sliceIsVisible = false;
                mp.bdvh.getViewerPanel().state()
                        .removeSources(Arrays.asList(sources_displayed_or_readyfordisplay));
            }
        }
    }

    public void show() {
        synchronized (lockChangeDisplay) {
            if ((!sliceIsVisible)&&(mp.displayMode != MultiSlicePositioner.NO_SLICE_DISPLAY_MODE)) {
                sliceIsVisible = true;
                List<SourceAndConverter<?>> sourcesToDisplay = IntStream.of(nChannels-1)
                        .filter(idx -> channelVisible[idx])
                        .mapToObj(idx -> sources_displayed_or_readyfordisplay[idx])
                        .collect(Collectors.toList());

                SourceAndConverterServices
                        .getSourceAndConverterDisplayService()
                        .show(mp.bdvh, sourcesToDisplay.toArray(new SourceAndConverter[0]));
            }
        }
        /*if (mp.getDisplayMode() == MultiSlicePositioner.REGISTRATION_MODE_INT) {
            mp.getBdvh().getViewerPanel().state()
                    .setSourcesActive(Arrays.asList(registered_sacs), true);
        }

        if (mp.getDisplayMode() == MultiSlicePositioner.POSITIONING_MODE_INT) {
            mp.getBdvh().getViewerPanel().state()
                    .setSourcesActive(Arrays.asList(relocated_sacs_positioning_mode), true);
        }*/
    }

    /*public boolean[] isVisible() {

        return channelsVisibilityFlag;
        /*boolean[] visibleFlag = new boolean[registered_sacs.length];

        ViewerState state =  mp.getBdvh().getViewerPanel().state();

        if (mp.getDisplayMode() == MultiSlicePositioner.REGISTRATION_MODE_INT) {
            for (int idx = 0; idx<registered_sacs.length;idx++) {
                visibleFlag[idx] = state.isSourceActive(registered_sacs[idx]);
            }
        }

        if (mp.getDisplayMode() == MultiSlicePositioner.POSITIONING_MODE_INT) {
            for (int idx = 0; idx<registered_sacs.length;idx++) {
                visibleFlag[idx] = state.isSourceActive(relocated_sacs_positioning_mode[idx]);
            }
        }

        return visibleFlag;*/

    //}

    public void setChannelsVisibility(boolean[] visibleFlag) {
        // TODO
        /*synchronized (lockChangeDisplay) {
            if (sliceIsVisible) {
                sliceIsVisible = true;
                List<SourceAndConverter<?>> sourcesToDisplay = IntStream.of(nChannels-1)
                        .filter(idx -> channelIsVisible[idx])
                        .mapToObj(idx -> sources_displayed_or_readyfordisplay[idx])
                        .collect(Collectors.toList());

                //mp.bdvh.getViewerPanel().state().addSources(sourcesToDisplay);

                SourceAndConverterServices
                        .getSourceAndConverterDisplayService()
                        .show(mp.bdvh, sourcesToDisplay.toArray(new SourceAndConverter[0]));
            }
        }*/
        /*ViewerState state =  mp.getBdvh().getViewerPanel().state();

        if (mp.getDisplayMode() == MultiSlicePositioner.REGISTRATION_MODE_INT) {
            for (int idx = 0; idx<registered_sacs.length;idx++) {
                state.setSourceActive(registered_sacs[idx], visibleFlag[idx]);
            }
        }

        if (mp.getDisplayMode() == MultiSlicePositioner.POSITIONING_MODE_INT) {
            for (int idx = 0; idx<registered_sacs.length;idx++) {
                state.setSourceActive(relocated_sacs_positioning_mode[idx], visibleFlag[idx]);
            }
        }*/
    }

    public Displaysettings[] getDisplaysettings() {
        Displaysettings[] ds = new Displaysettings[nChannels];
        for (int idx = 0; idx<nChannels;idx++) {
            ds[idx] = new Displaysettings(-1); // we don't care about the number
            DisplaysettingsHelper.GetDisplaySettingsFromCurrentConverter(sources_displayed_or_readyfordisplay[idx], ds[idx]);
        }
        return ds;
    }

    public void setDisplaysettings(Displaysettings[] ds) {
        /*for (int idx = 0; idx<registered_sacs.length;idx++) {
            if (mp.getDisplayMode() == MultiSlicePositioner.REGISTRATION_MODE_INT) {
                DisplaysettingsHelper.applyDisplaysettings(registered_sacs[idx], ds[idx]);
            }
            if (mp.getDisplayMode() == MultiSlicePositioner.POSITIONING_MODE_INT) {
                DisplaysettingsHelper.applyDisplaysettings(relocated_sacs_positioning_mode[idx], ds[idx]);
            }
        }*/
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
    }

    public void disableGraphicalHandles() {
        ghs.forEach(gh -> gh.disable());
    }

    public void enableGraphicalHandles() {
        ghs.forEach(gh -> gh.enable());
    }

    protected void positionChanged() {
        AffineTransform3D slicingModePositionAffineTransform = new AffineTransform3D();
        RealPoint center = getCenterPositionPMode();
        slicingModePositionAffineTransform.translate(center.getDoublePosition(0), center.getDoublePosition(1), -slice.getSlicingAxisPosition());
        slicingModePositioner.setAffineTransform(slicingModePositionAffineTransform);
    }

    public void setYShift(double yShift) {
        yShift_slicing_mode = yShift;
        positionChanged();
    }

    public boolean[] getChannelsVisibility() {
        return channelVisible;
    }

    public void sliceDeleted() {
        synchronized (lockChangeDisplay) {
            if (sliceIsVisible) {
                mp.bdvh.getViewerPanel().state()
                        .removeSources(Arrays.asList(sources_displayed_or_readyfordisplay));
            }
        }
    }

}
