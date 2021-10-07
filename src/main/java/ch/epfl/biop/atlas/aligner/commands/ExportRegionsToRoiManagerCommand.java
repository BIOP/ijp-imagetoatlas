package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

// TODO : make this command atlas agnostic
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Regions To Roi Manager",
        description = "Export atlas regions to ROI Manager (for each selected slice).")
public class ExportRegionsToRoiManagerCommand extends DynamicCommand implements
        Initializable {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Roi Naming")
    String naming_choice; // Intellij claims it's not used. but it's wrong. It's use through scijava reflection

    @Override
    public void run() {
        mp.exportSelectedSlicesRegionsToRoiManager((String) getInput("naming_choice"));
    }


    @Override
    public void initialize() {
        final MutableModuleItem<String> naming_choice = //
                getInfo().getMutableInput("naming_choice", String.class);
        List<String> names = new ArrayList<>(mp.getAtlas().getOntology().getRoot().data().keySet());
        naming_choice.setChoices(names);
    }

}
