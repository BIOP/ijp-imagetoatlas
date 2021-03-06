package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.net.URL;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>ABBA - Go to documentation")
public class ABBADocumentationCommand implements Command {

    @Parameter
    PlatformService ps;

    @Override
    public void run() {
        try {
            ps.open(new URL("https://c4science.ch/w/bioimaging_and_optics_platform_biop/image-processing/image-to-atlas-registration/"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
