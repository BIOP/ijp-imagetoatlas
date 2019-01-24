package ch.epfl.biop.atlastoimg2d.commands;


import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

// Is this really necessary to make a command for this ? ...
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Reset Image To Atlas Registration")
public class ResetImageToAtlasRegistration implements Command {

    @Parameter(type = ItemIO.BOTH)
    AtlasToImg2D atlasToImg;

    @Override
    public void run() {
        atlasToImg.resetRegistration();
    }
}
