package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export Options")
public class ExportRegionsCommand extends InteractiveCommand {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    Boolean addDescendants=false;

    @Parameter
    Boolean addAncestors=false;

    @Parameter
    Boolean clearRoiManager=false;

    @Parameter(label="Roi Naming",choices={"name","acronym","id","Roi Manager Index (no suffix)"})
    String namingChoice;

    @Parameter
    String roiPrefix="";

    @Parameter
    boolean outputLabelImage;

    public void run() {

    }
}
