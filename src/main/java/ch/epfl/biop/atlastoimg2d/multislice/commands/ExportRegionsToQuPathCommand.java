package ch.epfl.biop.atlastoimg2d.multislice.commands;

import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import java.io.File;

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
