package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.BdvHandle;
import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;


@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - Align Big Brains and Atlases (BDV)",
        description = "Starts ABBA from an Atlas with a BDV View")
public class ABBABdvStartCommand implements Command, Initializable {

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

            BdvHandle bdvh = new DefaultBdvSupplier(new SerializableBdvOptions()).get();//. SourceAndConverterServices.getBdvDisplayService().getNewBdv();
            view = new BdvMultislicePositionerView(mp, bdvh);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Parameter
    ObjectService os;

    public void initialize() {
        ABBAHelper.displayABBALogo(1500);
    }

}
