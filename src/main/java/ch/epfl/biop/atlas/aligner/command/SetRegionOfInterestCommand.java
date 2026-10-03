package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Locale;

/**
 * The region of interest of the session, without a view: 'ABBA - Define Rectangular ROI' is its interactive form.
 */
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Set Region Of Interest",
        description = "Sets the rectangular region of interest (ROI) of the session: registrations only consider this region, "
                + "and exports to images are cropped to it. It applies to all slices. For hemi-sections, restrict it to the "
                + "hemisphere, and restore the full size afterwards. 'ABBA - Get State' gives the current one (roi_mm).")
public class SetRegionOfInterestCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Region (mm)", required = false,
            description = "'x,y,width,height' in mm, x and y being the top left corner, in the coordinates of the rulers of "
                    + "'ABBA - Snapshot' (the full atlas section is centred on 0). Empty for the full atlas section.")
    String region_mm = "";

    @Parameter(type = ItemIO.OUTPUT, label = "Region of interest (mm)", description = "The region now in use: x,y,width,height in mm.")
    String roi_mm;

    @Override
    public void run() {
        if (region_mm == null || region_mm.trim().isEmpty()) {
            mp.setROI(-mp.sX / 2.0, -mp.sY / 2.0, mp.sX, mp.sY);
        } else {
            String[] parts = region_mm.trim().split("\\s*,\\s*");
            if (parts.length != 4) throw new IllegalArgumentException("region_mm: give 'x,y,width,height' in mm, not '" + region_mm + "'");
            double[] r = new double[4];
            for (int i = 0; i < 4; i++) r[i] = Double.parseDouble(parts[i]);
            if (r[2] <= 0 || r[3] <= 0) throw new IllegalArgumentException("region_mm: width and height must be positive");
            mp.setROI(r[0], r[1], r[2], r[3]);
        }
        roi_mm = format(mp.getROI());
    }

    static String format(double[] roi) {
        return String.format(Locale.ROOT, "%.3f,%.3f,%.3f,%.3f", roi[0], roi[1], roi[2], roi[3]);
    }
}
