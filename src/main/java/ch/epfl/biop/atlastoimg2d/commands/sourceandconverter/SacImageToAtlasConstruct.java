package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.commands.BrowseAtlasCommand;
import ch.epfl.biop.atlastoimg2d.AtlasToSourceAndConverter2D;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.util.function.Consumer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Image To Atlas (BDV)>Start")
public class SacImageToAtlasConstruct implements Command {

    @Parameter(type = ItemIO.OUTPUT)
    public AtlasToSourceAndConverter2D aligner;

    @Parameter(required = false)
    public BiopAtlas ba;

    @Parameter
    public SourceAndConverter[] sacs;

    @Parameter
    public CommandService cs;

    @Parameter
    Context ctx;

    public static Consumer<String> errlog = (str) -> System.err.println(SacImageToAtlasConstruct.class.getSimpleName()+" error :"+str);

    @Override
    public void run() {
        // WEIRD BUG !! Launching a command inside a command destroys the input!
        // Need to capture sacs input before they are lost...
        aligner = new AtlasToSourceAndConverter2D();
        aligner.setImage(sacs);
        aligner.setScijavaContext(ctx);

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
            errlog.accept("Atlas not initialized, abort command "+this.getClass().getSimpleName());
            return;
        }

        aligner.setAtlas(ba);
    }
}
