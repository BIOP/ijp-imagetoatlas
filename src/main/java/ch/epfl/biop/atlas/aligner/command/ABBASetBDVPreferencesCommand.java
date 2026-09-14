package ch.epfl.biop.atlas.aligner.command;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.command.BdvPlaygroundActionCommand;
import sc.fiji.bdvpg.viewer.bdv.config.BdvSettingsGUISetter;

import java.io.File;
@SuppressWarnings("unused")
@Plugin(type = BdvPlaygroundActionCommand.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>ABBA - Preferences",
        description = "Not functional: meant to edit the keyboard and mouse bindings of the ABBA viewer.")
public class ABBASetBDVPreferencesCommand implements Command {

    @Parameter
    Context context;

    @Override
    public void run() {
        new BdvSettingsGUISetter("plugins"+ File.separator + "bdvpgsettings"+File.separator+"abba", context).run();
    }

}
