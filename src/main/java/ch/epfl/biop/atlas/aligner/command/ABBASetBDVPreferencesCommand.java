package ch.epfl.biop.atlas.aligner.command;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.config.BdvSettingsGUISetter;
import sc.fiji.bdvpg.scijava.command.BdvPlaygroundActionCommand;

import java.io.File;

@Plugin(type = BdvPlaygroundActionCommand.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>ABBA - Preferences",
        description = "Sets actions linked to key / mouse event in ABBA (not functional)")
public class ABBASetBDVPreferencesCommand implements Command {

    @Parameter
    Context context;

    @Override
    public void run() {
        new BdvSettingsGUISetter("plugins"+ File.separator + "bdvpgsettings"+File.separator+"abba", context).run();
    }

}
