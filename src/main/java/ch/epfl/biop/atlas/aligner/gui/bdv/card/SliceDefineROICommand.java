package ch.epfl.biop.atlas.aligner.gui.bdv.card;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.command.display.bdv.region.UserRectangleGetCommand;
import net.imglib2.RealPoint;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.command.InteractiveCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>ABBA - Define Rectangular ROI",
        description = "Defines the rectangular region of interest (ROI) of the sections: registrations only consider this region, "
                + "and exports to images are cropped to it. It applies to all slices.")
public class SliceDefineROICommand extends InteractiveCommand implements Initializable {

    protected static final Logger logger = LoggerFactory.getLogger(SliceDefineROICommand.class);

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter
    BdvMultislicePositionerView view;

    @Parameter(persist = false, style = "format:0.000", label = "X (mm)",
            description = "X coordinate of the top left corner of the region, in mm. The full atlas section is centered on 0.")
    Double x_mm;

    @Parameter(persist = false, style = "format:0.000", label = "Y (mm)",
            description = "Y coordinate of the top left corner of the region, in mm. The full atlas section is centered on 0.")
    Double y_mm;

    @Parameter(persist = false, style = "format:0.000", label = "Width (mm)",
            description = "Width of the region, in mm.")
    Double width_mm;

    @Parameter(persist = false, style = "format:0.000", label = "Height (mm)",
            description = "Height of the region, in mm.")
    Double height_mm;

    @Parameter(label = "Define Interactively", callback = "defineClicked",
            description = "Draw the rectangle in the viewer.")
    Button defineRegionInteractively;

    @Parameter(label = "Full Size", callback = "fullSizeClicked",
            description = "Sets the region to the full atlas section.")
    Button setRegionFullSize;

    @Override
    public void run() {
        mp.setROI(x_mm,y_mm,width_mm,height_mm);
    }

    @Parameter
    CommandService cs;

    @Override
    public void initialize() {
        double[] roi = view.msp.getROI();

        final MutableModuleItem<Double> x_mm =
                getInfo().getMutableInput("x_mm", Double.class);
        x_mm.setValue(this, roi[0]);

        final MutableModuleItem<Double> y_mm =
                getInfo().getMutableInput("y_mm", Double.class);
        y_mm.setValue(this, roi[1]);

        final MutableModuleItem<Double> width_mm =
                getInfo().getMutableInput("width_mm", Double.class);
        width_mm.setValue(this, roi[2]);

        final MutableModuleItem<Double> height_mm =
                getInfo().getMutableInput("height_mm", Double.class);
        height_mm.setValue(this, roi[3]);
    }

    boolean inProcess = false;

    public void defineClicked() {

        if (inProcess) {
            mp.errorMessageForUser.accept("Please confirm the previous rectangle", "ROI already being selected");
        } else {
            Thread t = new Thread(() -> {
                try {
                    inProcess = true;
                    CommandModule cm = cs.run(UserRectangleGetCommand.class, true,
                                    "bdvh", view.getBdvh(),
                                    "time_out_in_ms", -1,
                                    "message_for_user", "Select the rectangular region of interest.")
                            .get();
                    RealPoint p1 = (RealPoint) cm.getOutput("p1");
                    RealPoint p2 = (RealPoint) cm.getOutput("p2");

                    {
                        width_mm = Math.abs(p1.getDoublePosition(0) - p2.getDoublePosition(0));
                        height_mm = Math.abs(p1.getDoublePosition(1) - p2.getDoublePosition(1));

                        double minx = Math.min(p1.getDoublePosition(0), p2.getDoublePosition(0));
                        double miny = Math.min(p1.getDoublePosition(1), p2.getDoublePosition(1));

                        if (view.getDisplayMode() == BdvMultislicePositionerView.POSITIONING_MODE_INT) {
                            x_mm = Math.IEEEremainder(minx + mp.sX * 0.5, mp.sX);
                            y_mm = miny;
                        }

                        if (view.getDisplayMode() == BdvMultislicePositionerView.REVIEW_MODE_INT) {
                            x_mm = minx;
                            y_mm = miny;
                        }

                        logger.debug("x_mm = " + x_mm);
                        logger.debug("y_mm = " + y_mm);
                        logger.debug("width_mm = " + width_mm);
                        logger.debug("height_mm = " + height_mm);

                        run();
                        inProcess = false;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    fullSizeClicked();
                }
            });
            t.start();
        }

    }

    public void fullSizeClicked() {
        x_mm = -mp.sX / 2.0;
        y_mm = -mp.sY / 2.0;
        width_mm = mp.sX;
        height_mm = mp.sY;
        run();
    }
}
