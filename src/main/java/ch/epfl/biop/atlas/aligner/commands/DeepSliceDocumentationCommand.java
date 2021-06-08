package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.net.URL;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>DeepSlice Info")
public class DeepSliceDocumentationCommand implements Command {

    @Parameter
    PlatformService ps;

    @Override
    public void run() {
        try {
            ps.open(new URL("https://researchers.mq.edu.au/en/publications/deepslice-a-deep-neural-network-for-fully-automatic-alignment-of-"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
