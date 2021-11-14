package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.mouse.allen.ccfv3.command.AllenBrainAdultMouseAtlasCCF2017Command;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4.command.WaxholmSpragueDawleyRatV4Command;
import ch.epfl.biop.atlas.struct.Atlas;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Open Atlas",
        description = "Open an atlas")
public class ABBAOpenAtlasCommand implements Command {

    @Parameter(choices = {"Adult Mouse Brain - Allen Brain Atlas V3", "Rat - Waxholm Sprague Dawley V4"})
    String atlasType;

    @Parameter(type = ItemIO.OUTPUT)
    Atlas atlas;

    @Parameter
    CommandService cs;

    @Override
    public void run() {
        try {
            switch (atlasType) {
                case "Adult Mouse Brain - Allen Brain Atlas V3":
                    atlas = (Atlas) cs.run(AllenBrainAdultMouseAtlasCCF2017Command.class, true).get().getOutput("ba");
                    break;
                case "Rat - Waxholm Sprague Dawley V4":
                    atlas = (Atlas) cs.run(WaxholmSpragueDawleyRatV4Command.class, true).get().getOutput("ba");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
