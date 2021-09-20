package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Regions To Roi Manager",
        description = "Export atlas regions to ROI Manager (for each selected slice).")
public class ExportRegionsToRoiManagerCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Roi Naming",choices={"name","acronym","id","Roi Manager Index (no suffix)"})
    String naming_choice;

    @Override
    public void run() {
        mp.exportSelectedSlicesRegionsToRoiManager(naming_choice);
    }

}
