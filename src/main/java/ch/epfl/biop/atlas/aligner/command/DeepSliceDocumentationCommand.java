package ch.epfl.biop.atlas.aligner.command;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.net.URL;

// TODO: link this command to Allen Brain Atlas Coronal only

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>DeepSlice>DeepSlice Info",
        description = "Opens the DeepSlice guide website in a web browser.",
        iconPath = "/graphics/AboutDeepslice.png")
public class DeepSliceDocumentationCommand implements Command {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Opens https://www.deepslice.com.au/guide in a web browser.";

    @Parameter
    PlatformService ps;

    @Override
    public void run() {
        try {
            ps.open(new URL("https://www.deepslice.com.au/guide"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
