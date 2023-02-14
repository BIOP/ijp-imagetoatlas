package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - Close session",
        description = "Close ABBA session")
public class ABBACloseCommand implements Command{
    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    ObjectService os;

    public void run() {
        mp.close();
        os.removeObject(mp);
    }
}
