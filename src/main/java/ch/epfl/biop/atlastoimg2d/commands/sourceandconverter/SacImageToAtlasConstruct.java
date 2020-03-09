package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.commands.BrowseAtlasCommand;
import ch.epfl.biop.atlastoimg2d.AtlasToImagePlus2D;
import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import ch.epfl.biop.atlastoimg2d.AtlasToSourceAndConverter2D;
import ij.IJ;
import ij.ImagePlus;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Image To Atlas (BDV)>Start")
public class SacImageToAtlasConstruct implements Command {

    @Parameter(type = ItemIO.OUTPUT)
    public AtlasToSourceAndConverter2D aligner;

   // @Parameter(choices={"Elastix","BigWarp"})
   // String registrationType;

    @Parameter(required = false)
    public BiopAtlas ba;

    @Parameter(type = ItemIO.INPUT)
    public SourceAndConverter[] sacs;

    @Parameter
    public CommandService cs;

    @Parameter
    Context ctx;

    @Override
    public void run() {
        if (ba==null) {
            // Atlas not set -> need to set one
            Future<CommandModule> f = cs.run(BrowseAtlasCommand.class, true);
            try {
                ba = (BiopAtlas) f.get().getOutput("ba");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return;
            }
        }

        if (ba == null) {
            System.out.println("Atlas not initialized");
            return;
        }

        aligner = new AtlasToSourceAndConverter2D();


        aligner.setAtlas(ba);
        // Convert to RGB Stack in case the input image is a RGB image
        /*if (imp.getType()==ImagePlus.COLOR_RGB) {
           IJ.run(imp, "RGB Stack", "");
        }*/
        aligner.setImage(sacs);
        aligner.setScijavaContext(ctx);
    }
}
