package ch.epfl.biop.atlastoimg2d.commands;

import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Image To Atlas>Compute Transformed Atlas ROIs")
public class ImageToAtlasComputeROIS implements Command{

    @Parameter(type = ItemIO.BOTH)
    AtlasToImg2D aligner;

    @Parameter(label="Put in Roi Manager ?")
    boolean putRoiManager;

    @Parameter(type = ItemIO.OUTPUT)
    ConvertibleRois cr;

    @Parameter
    ObjectService os;

    @Override
    public void run() {
        if (aligner.getRegistrations().size()>0) {
            //aligner.putTransformedRoisToObjectService();
            cr = aligner.getTransformedRois();
            if (putRoiManager) aligner.putTransformedRoisToImageJROIManager();
            os.addObject(cr);
        } else {
            System.err.println("Error : no registration done.");
        }
    }
}
