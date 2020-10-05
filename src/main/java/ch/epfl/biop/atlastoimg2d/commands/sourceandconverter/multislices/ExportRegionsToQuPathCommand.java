package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;

import java.io.File;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export ROIs To File")
public class ExportRegionsToQuPathCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Erase Previous ROIs")
    boolean erasePreviousFile;

    @Parameter(callback = "clicked")
    Button run;

    @Override
    public void run() {
        // Cannot be accessed
        clicked();
    }

    public void clicked() {
        mp.exportSelectedSlicesRegionsToQuPathProject(erasePreviousFile);
    }
}
