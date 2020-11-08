package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>Export Regions To QuPath project")
public class ExportRegionsToQuPathCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Erase Previous ROIs")
    boolean erasePreviousFile;

    @Override
    public void run() {
        mp.exportSelectedSlicesRegionsToQuPathProject(erasePreviousFile);
    }

}
