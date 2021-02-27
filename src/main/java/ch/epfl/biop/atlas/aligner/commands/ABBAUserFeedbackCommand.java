package ch.epfl.biop.atlas.aligner.commands;

import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.net.URL;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>ABBA - Give your feedback")
public class ABBAUserFeedbackCommand implements Command {
    @Parameter
    PlatformService ps;

    @Override
    public void run() {
        try {
            ps.open(new URL("https://docs.google.com/forms/d/e/1FAIpQLSfpDNp7nW6SlAhQMhVCRbAQ03lxx7pdIFErx0-INx2e68TaiQ/viewform"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
