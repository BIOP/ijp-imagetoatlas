package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Registrations To QuPath Project",
        description = "Export atlas regions and transformations to QuPath project (for each selected slice)")
public class ExportToQuPathProjectCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Erase Previous ROIs")
    boolean erase_previous_file;

    @Override
    public void run() {
        mp.exportSelectedSlicesRegionsToQuPathProject(erase_previous_file);
    }

}
