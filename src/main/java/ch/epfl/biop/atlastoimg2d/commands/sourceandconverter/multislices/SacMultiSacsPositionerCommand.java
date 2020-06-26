package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import bdv.util.BdvHandle;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import ch.epfl.biop.atlastoimg2d.multislice.ReslicedAtlas;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.projector.Projection;
import sc.fiji.bdvpg.scijava.command.bdv.BdvWindowCreatorCommand;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Position Multiple Slices")
public class SacMultiSacsPositionerCommand implements Command {

    @Parameter(choices = {"coronal", "sagittal", "horizontal", "free"})
    String slicingMode;

    @Parameter
    public BiopAtlas ba;

    AffineTransform3D slicingTransfom;

    @Parameter
    CommandService cs;

    @Parameter
    Context context;

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvMultiSlicer;

    @Parameter
    ObjectService os;

    @Parameter(type = ItemIO.OUTPUT)
    MultiSlicePositioner mp;

    @Override
    public void run() {

        slicingTransfom = new AffineTransform3D();

        switch(slicingMode) {
            case "free" :
                throw new UnsupportedOperationException();
            case "coronal" :
                // No Change
                break;
            case "sagittal" :
                slicingTransfom.rotate(1,-Math.PI/2);
                break;
            case "horizontal" :
                slicingTransfom.rotate(0,Math.PI/2);
                break;
        }

        ReslicedAtlas ra = new ReslicedAtlas(ba);
        ra.setResolution(0.01);
        ra.setSlicingTransform(slicingTransfom);

        try {

            bdvMultiSlicer = (BdvHandle) cs.run(BdvWindowCreatorCommand.class, true,
                    "is2D", false, //true,
                    "windowTitle", "Multi Slice Positioner " + ba.toString(),
                    "interpolate", false,
                    "nTimepoints", 1,
                    "projector", Projection.SUM_PROJECTOR)
                    .get().getOutput("bdvh");

            mp = new MultiSlicePositioner(bdvMultiSlicer, ba, ra, context);

            os.addObject(mp);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
