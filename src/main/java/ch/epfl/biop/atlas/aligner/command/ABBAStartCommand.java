package ch.epfl.biop.atlas.aligner.command;

import bdv.util.BdvHandle;
import bdv.util.BdvHandleFrame;
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
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;

import javax.swing.JFrame;
import javax.swing.ImageIcon;

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

    @Parameter(type = ItemIO.OUTPUT)
    BdvHandle bdvMultiSlicer;

    @Parameter
    ObjectService os;

    @Parameter(type = ItemIO.OUTPUT)
    MultiSlicePositioner mp;

    @Parameter(label = "No Graphical User Interface")
    boolean nogui = false;

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

        try {


                /*bdvMultiSlicer = new DefaultBdvSupplier(new SerializableBdvOptions()).get(); // Get a default supplier

                // Set ABBA Icon in Window
                JFrame frame = ((BdvHandleFrame)bdvMultiSlicer).getBigDataViewer().getViewerFrame();
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setIconImage((new ImageIcon(MultiSlicePositioner.class.getResource("/graphics/ABBAFrame.jpg"))).getImage());

                if (bdvMultiSlicer==null) {
                    System.err.println("Error : bdv multislicer null");
                    return;
                }*/
                mp = new MultiSlicePositioner(ba, ra, context);


            os.addObject(mp);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
