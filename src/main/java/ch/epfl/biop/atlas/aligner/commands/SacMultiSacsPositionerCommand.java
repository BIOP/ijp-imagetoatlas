package ch.epfl.biop.atlas.aligner.commands;

import bdv.util.BdvHandle;
import bdv.util.BdvHandleFrame;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.command.bdv.BdvWindowCreatorCommand;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

import javax.swing.*;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Position Multiple Slices")
public class SacMultiSacsPositionerCommand implements Command {

    @Parameter(choices = {"coronal", "sagittal", "horizontal"})//, "free"})
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

    @Parameter
    SourceAndConverterBdvDisplayService bdvDisplayService;

    @Override
    public void run() {

        slicingTransfom = new AffineTransform3D();

        switch(slicingMode) {
            case "coronal" :
                slicingTransfom.rotate(1,Math.PI/2);
                break;
            case "sagittal" :
                // No Change
                break;
            case "vertical" :
                slicingTransfom.rotate(0,-Math.PI/2);
                break;
        }

        ReslicedAtlas ra = new ReslicedAtlas(ba);
        ra.setResolution(0.01);
        ra.setSlicingTransform(slicingTransfom);

        try {

            bdvMultiSlicer = bdvDisplayService.getNewBdv();
                    /*(BdvHandle) cs.run(BdvWindowCreatorCommand.class, true,
                    "is2D", false, //true,
                    "windowTitle", "Allen Brain BIOP Aligner",//"Multi Slice Positioner " + ba.toString(),
                    "interpolate", false,
                    "nTimepoints", 1,
                    "projector", Projection.SUM_PROJECTOR)
                    .get().getOutput("bdvh");*/

            JFrame frame = ((BdvHandleFrame)bdvMultiSlicer).getBigDataViewer().getViewerFrame();
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setIconImage((new ImageIcon(MultiSlicePositioner.class.getResource("/graphics/ABBAFrame.jpg"))).getImage());

            if (bdvMultiSlicer==null) {
                System.err.println("Error : bdv multislicer null");
                return;
            }

            if (bdvMultiSlicer.getCardPanel()==null) {System.err.println("bdv multislicer card panel null");}

            mp = new MultiSlicePositioner(bdvMultiSlicer, ba, ra, context);

            os.addObject(mp);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
