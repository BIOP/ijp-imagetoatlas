package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.BdvHandle;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.concurrent.ExecutionException;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - Align Big Brains and Atlases (BDV)",
        description = "Starts ABBA from an Atlas with a BDV View")
public class ABBABdvStartCommand implements Command {

    @Parameter(choices = {"coronal", "sagittal", "horizontal"})
    String slicing_mode;

    @Parameter
    public Atlas ba;

    @Parameter
    CommandService cs;

    @Parameter(type = ItemIO.OUTPUT)
    BdvMultislicePositionerView view;

    @Override
    public void run() {
        try {
            MultiSlicePositioner mp = (MultiSlicePositioner) cs
                .run(ABBAStartCommand.class, true,
                    "slicing_mode", slicing_mode,
                    "ba", ba)
                    .get()
                    .getOutput("mp");

            BdvHandle bdvh = SourceAndConverterServices.getBdvDisplayService().getNewBdv();
            view = new BdvMultislicePositionerView(mp, bdvh);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
