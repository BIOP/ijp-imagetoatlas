package ch.epfl.biop.atlas.aligner.gui.bdv;

import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - ABBA Start",
        description = "Starts ABBA from an Atlas with a BDV View")
public class ABBABdvShortCommand implements Command {

    @Parameter
    CommandService cs;

    @Override
    public void run() {
        cs.run(ABBABdvStartCommand.class, true);
    }

}
