package ch.epfl.biop.atlas.aligner;

import net.imglib2.RealPoint;
import spimdata.util.Displaysettings;

import java.awt.*;

/**
 * Everything related to SliceSources display and its synchronization
 *
 *
 *
 */

public class SliceSourcesNoGUIState  extends SliceSourcesGUIState {

    public SliceSourcesNoGUIState(SliceSources slice, MultiSlicePositioner mp) {
        super(slice, mp);
    }

    protected void sourcesChanged() {
    }

    public void sliceDisplayModeChanged() {
    }

    protected void displayModeChanged() {
    }

    protected Integer[] getBdvHandleCoords() {
        return new Integer[]{0, 0, 0};
    }

    public void setSliceVisible() { }

    public void setSliceInvisible() { }

    public Displaysettings[] getDisplaysettings() {
        Displaysettings[] ds = new Displaysettings[nChannels];
        for (int idx = 0; idx<nChannels;idx++) {
            ds[idx] = new Displaysettings(-1); // we don't care about the number
            Displaysettings.GetDisplaySettingsFromCurrentConverter(slice.original_sacs[idx], ds[idx]);
        }
        return ds;
    }

    public void setDisplaysettings(Displaysettings[] ds) {
        for (int idx = 0; idx<nChannels;idx++) {
            Displaysettings.applyDisplaysettings(slice.original_sacs[idx], ds[idx]);
        }
    }

    public Integer[] getBdvHandleColor() { return new Integer[3];}

    public Integer getBdvHandleRadius() {
        return 12;
    }

    public void drawGraphicalHandles(Graphics2D g) { }

    public void disableGraphicalHandles() { }

    public void enableGraphicalHandles() {  }

    protected void positionChanged() {  }

    public void setYShift(double yShift) { }

    public boolean[] getChannelsVisibility() {
        return new boolean[slice.nChannels];
    }

    public void setChannelsVisibility(boolean[] channelsVisibility) { }

    public void sliceDeleted() { }

    public void isNotCurrent() { }

    public void isCurrent() { }

    public boolean isSliceVisible() {
        return false;
    }

    public boolean isChannelVisible(int iChannel) {
        return false;
    }

    public void setChannelVisibility(int iChannel, boolean flag) { }

    public void select() { }

    public void deselect() { }
}
