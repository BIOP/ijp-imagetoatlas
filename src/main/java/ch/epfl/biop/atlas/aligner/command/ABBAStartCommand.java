package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - Align Big Brains and Atlases",
        description = "Starts ABBA from an Atlas")
public class ABBAStartCommand implements Command {

    @Parameter(choices = {"coronal", "sagittal", "horizontal"})
    String slicing_mode;

    @Parameter
    public Atlas ba;

    @Parameter
    Context context;

    @Parameter
    ObjectService os;

    @Parameter(type = ItemIO.OUTPUT)
    MultiSlicePositioner mp;

    @Override
    public void run() {

        AffineTransform3D slicingTransfom = ba.getMap().getPreSlicingTransform();

        switch(slicing_mode) {
            case "coronal" :
                slicingTransfom.rotate(1,Math.PI/2);
                break;
            case "sagittal" :
                // No Change
                break;
            case "horizontal" :
                slicingTransfom.rotate(0,-Math.PI/2);
                break;
        }

        ReslicedAtlas ra = new ReslicedAtlas(ba);
        ra.setResolution(ba.getMap().getAtlasPrecisionInMillimeter());
        ra.setSlicingTransform(slicingTransfom);

        mp = new MultiSlicePositioner(ba, ra, context);
        os.addObject(mp);

    }

}
