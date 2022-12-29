package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p1.command.WaxholmSpragueDawleyRatV4p1Command;
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

    @Parameter(choices = {"Adult Mouse Brain - Allen Brain Atlas V3.1", "Rat - Waxholm Sprague Dawley V4.1"})
    String atlasType;

    @Parameter(type = ItemIO.OUTPUT)
    Atlas atlas;

    @Parameter
    CommandService cs;

    @Override
    public void run() {
        try {
            switch (atlasType) {
                case "Adult Mouse Brain - Allen Brain Atlas V3.1":
                    atlas = (Atlas) cs.run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");
                    break;
                case "Rat - Waxholm Sprague Dawley V4.1":
                    atlas = (Atlas) cs.run(WaxholmSpragueDawleyRatV4p1Command.class, true).get().getOutput("ba");
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
