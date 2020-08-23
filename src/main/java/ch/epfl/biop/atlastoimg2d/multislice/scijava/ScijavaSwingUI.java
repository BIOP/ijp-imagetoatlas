package ch.epfl.biop.atlastoimg2d.multislice.scijava;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleRunner;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.PluginService;
import org.scijava.ui.swing.widget.SwingInputHarvester;
import org.scijava.ui.swing.widget.SwingInputPanel;
import org.scijava.widget.InputHarvester;
import org.scijava.widget.InputPanel;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * Thanks to @frauzufall, helper class which build Swing UI of Scijava Commands
 */

public class ScijavaSwingUI {

    static public<C extends Command> JPanel getPanel(Context context, Class<C> scijavaCommand, Object... args) {
        Module module = null;
        JPanel panel = null;
        try {
            module = createModule(context, scijavaCommand, args);
            panel = createModulePanel(context, module);

        } catch (ModuleException e) {
            e.printStackTrace();
        }
        return panel;
    }

    static private Module createModule(Context context, Class commandClass, Object... args) throws ModuleException {
        Module module = context.getService(CommandService.class).getCommand(commandClass).createModule();
        context.inject(module);
        preprocessWithoutHarvesting(context,module);
        setModuleInputs(module, args);
        return module;
    }

    static private <M extends Module> void preprocessWithoutHarvesting(Context context, M module) {
        ModuleRunner moduleRunner = new ModuleRunner(context, module, preprocessorsWithoutHarvesting(context), Collections.emptyList());
        moduleRunner.preProcess();
    }

    static private List<? extends PreprocessorPlugin> preprocessorsWithoutHarvesting(Context context) {
        //remove input harvesters from preprocessing
        List<PreprocessorPlugin> preprocessors = context.getService(PluginService.class).createInstancesOfType(PreprocessorPlugin.class);
        preprocessors.removeIf(preprocessor -> preprocessor instanceof InputHarvester);
        return preprocessors;
    }

    static private void setModuleInputs(Module module, Object[] args) {
        assert(args.length % 2 == 0);
        for (int i = 0; i < args.length-1; i+=2) {
            String input = (String) args[i];
            module.setInput(input, args[i+1]);
            module.resolveInput(input);
        }
    }

    static private JPanel createModulePanel(Context context,Module module) throws ModuleException {
        SwingInputHarvester swingInputHarvester = new SwingInputHarvester();
        context.inject(swingInputHarvester);
        InputPanel<JPanel, JPanel> inputPanel = new SwingInputPanel();
        swingInputHarvester.buildPanel(inputPanel, module);
        return inputPanel.getComponent();
    }

}
