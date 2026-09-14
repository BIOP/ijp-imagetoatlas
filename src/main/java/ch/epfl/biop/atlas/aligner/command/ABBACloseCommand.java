package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - Close session",
        description = "Closes an ABBA session and releases it. Unsaved work is lost.")
public class ABBACloseCommand implements Command{

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "The session cannot be used after this command. Save the state before closing to keep the work.";
    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter
    ObjectService os;

    public void run() {
        mp.close();
        os.removeObject(mp);
    }
}
