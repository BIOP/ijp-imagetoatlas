package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import ch.epfl.biop.scijava.command.bdv.userdefinedregion.GetUserRectangleCommand;
import net.imglib2.RealPoint;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import java.util.List;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Define Rectangle")
public class RectangleROIDefineCommand extends InteractiveCommand {

    @Parameter
    MultiSlicePositioner mp;

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

        mp.getBdvh().getViewerPanel().requestRepaint();
    }

    @Parameter
    CommandService cs;

    public void defineClicked() {

        Thread t = new Thread(() -> {
        try {
            List<RealPoint> pts = (List<RealPoint>)
            cs.run(GetUserRectangleCommand.class, true, "bdvh", mp.getBdvh(), "timeOut", -1)
                    .get().getOutput("pts");

            if (pts==null) {
                fullSizeClicked();
            } else {
                assert pts.size() == 2;

                System.out.println("pts.get(0).getDoublePosition(0) = "+pts.get(0).getDoublePosition(0));

                System.out.println("pts.get(1).getDoublePosition(0) = "+pts.get(1).getDoublePosition(0));

                sx = Math.abs(pts.get(0).getDoublePosition(0)-pts.get(1).getDoublePosition(0));
                sy = Math.abs(pts.get(0).getDoublePosition(1)-pts.get(1).getDoublePosition(1));


                double minx = Math.min(pts.get(0).getDoublePosition(0),pts.get(1).getDoublePosition(0));
                double miny = Math.min(pts.get(0).getDoublePosition(1),pts.get(1).getDoublePosition(1));

                if (mp.currentMode == MultiSlicePositioner.POSITIONING_MODE_INT) {
                    px = Math.IEEEremainder(minx+mp.sX*0.5, mp.sX);
                    py = miny;
                }

                if (mp.currentMode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
                    px = minx;
                    py = miny;
                }

                System.out.println("px = "+px);
                System.out.println("py = "+py);
                System.out.println("sx = "+sx);
                System.out.println("sy = "+sy);

                run();
            }

        } catch (Exception e) {
            e.printStackTrace();
            fullSizeClicked();
        }});
        t.start();

        // TODO : uncomment and avoid deadlock below ...

        /*try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

    }

    public void fullSizeClicked() {
        px = -mp.sX / 2.0;
        py = -mp.sY / 2.0;
        sx = mp.sX;
        sy = mp.sY;
        run();
    }
}
