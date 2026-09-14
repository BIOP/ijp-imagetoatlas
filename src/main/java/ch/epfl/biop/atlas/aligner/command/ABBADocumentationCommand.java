package ch.epfl.biop.atlas.aligner.command;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.net.URL;
@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>ABBA - Go to documentation",
        description = "Opens the ABBA documentation website in a web browser.")
public class ABBADocumentationCommand implements Command {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Opens https://abba-documentation.readthedocs.io in a web browser.";

    @Parameter
    PlatformService ps;

    @Override
    public void run() {
        try {
            ps.open(new URL("https://abba-documentation.readthedocs.io/"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
