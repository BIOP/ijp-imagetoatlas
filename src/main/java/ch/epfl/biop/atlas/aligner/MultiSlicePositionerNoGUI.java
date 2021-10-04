package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.atlas.BiopAtlas;
import org.scijava.Context;

import java.awt.*;

/**
 * All specific methods and fields dedicated to the multislice positioner
 *
 * There is:
 *
 * - a positioning mode
 *      This is mosly useful at the beginning of the registration
 *      Slices can be moved along the axis / stretched and shrunk
 *      Only certain sections of the atlas are shown to improve global overview, based on the user need
 *
 * - a review mode
 *      This is mostly useful for reviewing the quality of registration
 *      Only one slice is visible at a time
 *      The atlas is fully displayed
 */

public class MultiSlicePositionerNoGUI extends MultiSlicePositioner {
    /**
     * Starts ABBA
     *
     * @param biopAtlas     an atlas
     * @param reslicedAtlas a resliced atlas
     * @param ctx           a scijava context
     */
    public MultiSlicePositionerNoGUI(BiopAtlas biopAtlas, ReslicedAtlas reslicedAtlas, Context ctx) {
        super(null, biopAtlas, reslicedAtlas, ctx);
        this.log = (message) -> {
            logger.info("Multipositioner : "+message);
        };
    }

    public void updateDisplay() {}

    public void recenterBdvh() {}

    public void centerBdvViewOn(SliceSources current_slice, boolean maintainoffset, SliceSources previous_slice) {}

    protected void draw(Graphics2D g) {}

}