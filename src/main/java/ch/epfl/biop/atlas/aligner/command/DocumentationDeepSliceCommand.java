package ch.epfl.biop.atlas.aligner.command;

import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.net.URL;

// TODO: link this command to Allen Brain Atlas Coronal only

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>ABBA - DeepSlice Info",
        description = "Open deep slice reference webpage.")
public class DocumentationDeepSliceCommand implements Command {

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
