package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.bdv.command.userdefinedregion.GetUserRectangleCommand;
import net.imglib2.RealPoint;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>ABBA - Define Rectangular ROI",
        description = "Defines a rectangular ROI that will be considered for registrations")
public class SliceDefineROICommand extends InteractiveCommand {

    protected static Logger logger = LoggerFactory.getLogger(SliceDefineROICommand.class);

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    BdvMultislicePositionerView view;

    @Parameter
    double px;

    @Parameter
    double py;

    @Parameter
    double sx = -1;

    @Parameter
    double sy = -1;

    @Parameter(label = "Define Interactively", callback = "defineClicked")
    Button defineRegionInteractively;

    @Parameter(label = "Full Size", callback = "fullSizeClicked")
    Button setRegionFullSize;

    @Override
    public void run() {
        mp.setROI(px,py,sx,sy);
    }

    @Parameter
    CommandService cs;

    public void defineClicked() {

        Thread t = new Thread(() -> {
        try {
            List<RealPoint> pts = (List<RealPoint>)
            cs.run(GetUserRectangleCommand.class, true,
                    "bdvh", view.getBdvh(),
                    "timeOutInMs", -1,
                    "messageForUser", "Select the rectangular region of interest.")
                    .get().getOutput("pts");

            if (pts==null) {
                fullSizeClicked();
            } else {
                assert pts.size() == 2;

                logger.debug("pts.get(0).getDoublePosition(0) = "+pts.get(0).getDoublePosition(0));

                logger.debug("pts.get(1).getDoublePosition(0) = "+pts.get(1).getDoublePosition(0));

                sx = Math.abs(pts.get(0).getDoublePosition(0)-pts.get(1).getDoublePosition(0));
                sy = Math.abs(pts.get(0).getDoublePosition(1)-pts.get(1).getDoublePosition(1));


                double minx = Math.min(pts.get(0).getDoublePosition(0),pts.get(1).getDoublePosition(0));
                double miny = Math.min(pts.get(0).getDoublePosition(1),pts.get(1).getDoublePosition(1));

                if (view.getDisplayMode() == BdvMultislicePositionerView.POSITIONING_MODE_INT) {
                    px = Math.IEEEremainder(minx+mp.sX*0.5, mp.sX);
                    py = miny;
                }

                if (view.getDisplayMode() == BdvMultislicePositionerView.REVIEW_MODE_INT) {
                    px = minx;
                    py = miny;
                }

                logger.debug("px = "+px);
                logger.debug("py = "+py);
                logger.debug("sx = "+sx);
                logger.debug("sy = "+sy);

                run();
            }

        } catch (Exception e) {
            e.printStackTrace();
            fullSizeClicked();
        }});
        t.start();

    }

    public void fullSizeClicked() {
        px = -mp.sX / 2.0;
        py = -mp.sY / 2.0;
        sx = mp.sX;
        sy = mp.sY;
        run();
    }
}
