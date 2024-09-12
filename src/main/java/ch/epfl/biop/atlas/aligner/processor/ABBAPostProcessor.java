package ch.epfl.biop.atlas.aligner.processor;

import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.command.ABBAStateLoadCommand;
import ch.epfl.biop.atlas.aligner.command.ABBAStateSaveCommand;
import ch.epfl.biop.atlas.aligner.command.DisplaySettingsCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvShortCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvStartCommand;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.scijava.AtlasChooserCommand;
import ch.epfl.biop.bdv.img.omero.command.OmeroConnectCommand;
import ch.epfl.biop.scijava.command.bdv.userdefinedregion.GetUserRectangleCommand;
import org.scijava.Priority;
import org.scijava.command.CommandInfo;
import org.scijava.command.DynamicCommandInfo;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Resolves some outputs which are unnecessary to display when somebody uses ABBA
 * [WARNING] Ignoring unsupported output: ba [ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command]
 * [WARNING] Ignoring unsupported output: atlas [ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command]
 * [WARNING] Ignoring unsupported output: mp [ch.epfl.biop.atlas.aligner.MultiSlicePositioner]
 * [WARNING] Ignoring unsupported output: view [ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView]
 * [WARNING] Ignoring unsupported output: p1 [net.imglib2.RealPoint] in user roi draw
 * [WARNING] Ignoring unsupported output: p2 [net.imglib2.RealPoint]
 * [WARNING] Ignoring unsupported output: ds [spimdata.util.Displaysettings]
 * [WARNING] Ignoring unsupported output: ds [spimdata.util.Displaysettings] DisplaySettingsCommand
 */

@Plugin(type = PostprocessorPlugin.class, priority = Priority.LOW) // We want it to kick in before the swing input harvester
public class ABBAPostProcessor extends AbstractPostprocessorPlugin {

    final static Set<String> checkedCommands = new HashSet<>();

    static volatile boolean initialized = false;

    protected static final Logger logger = LoggerFactory.getLogger(ABBAPostProcessor.class);

    static void initializeCommands() {
        if (!initialized) {
            synchronized (checkedCommands) {
                if (initialized) return;
                checkedCommands.add(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class.getName());
                checkedCommands.add(ABBAStartCommand.class.getName());
                checkedCommands.add(ABBABdvStartCommand.class.getName());
                checkedCommands.add(ABBABdvShortCommand.class.getName());
                checkedCommands.add(AtlasChooserCommand.class.getName());
                checkedCommands.add(ABBAStateLoadCommand.class.getName());
                checkedCommands.add(ABBAStateSaveCommand.class.getName());
                checkedCommands.add(GetUserRectangleCommand.class.getName());
                checkedCommands.add(DisplaySettingsCommand.class.getName());
                checkedCommands.add(OmeroConnectCommand.class.getName());
                initialized = true;
            }
        }
    }

    @Override
    public void process(Module module) {
        if (module.getInfo() instanceof CommandInfo) {
            CommandInfo ci = (CommandInfo) module.getInfo();
            initializeCommands();
            if (checkedCommands.contains(ci.getClassName())) {
                module.getInfo().outputs().forEach(output ->  {
                    String name = output.getName();
                    if (!module.isOutputResolved(name))  {
                        module.resolveOutput(name); // big bertha, no question asked
                        logger.info("Resolving output "+name+" in command "+ci.getClassName());
                    }
                });
            }
        } else if (module.getInfo() instanceof DynamicCommandInfo) {
            DynamicCommandInfo ci = (DynamicCommandInfo) module.getInfo();
            initializeCommands();
            if (checkedCommands.contains(ci.getDelegateClassName())) {
                module.getInfo().outputs().forEach(output -> {
                    String name = output.getName();
                    if (!module.isOutputResolved(name))  {
                        module.resolveOutput(name); // big bertha, no question asked
                        logger.info("Resolving output "+name+" in dynamic command "+ci.getDelegateClassName());
                    }
                });
            }
        }
    }
}
