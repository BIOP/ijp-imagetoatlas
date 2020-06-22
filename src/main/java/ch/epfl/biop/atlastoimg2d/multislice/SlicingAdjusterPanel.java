package ch.epfl.biop.atlastoimg2d.multislice;

import ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices.SlicerAdjusterCommand;
import org.scijava.Context;
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

// With help from  @frauzufall : https://gitter.im/scijava/scijava-common?at=5eea4a0a90cd6426c8124d88

public class SlicingAdjusterPanel {

    final MultiSlicePositioner mp;
    final Context context;

    public SlicingAdjusterPanel(MultiSlicePositioner mp, Context ctx) {
        this.mp = mp;
        this.context = ctx;
    }

    public JPanel getPanel(Object... args) {
        Module module = null;
        JPanel panel = null;
        try {
            module = createModule(SlicerAdjusterCommand.class, args);
            panel = createModulePanel(module);

        } catch (ModuleException e) {
            e.printStackTrace();
        }
        return panel;
    }

    private Module createModule(Class commandClass, Object... args) throws ModuleException {
        Module module = context.getService(CommandService.class).getCommand(commandClass).createModule();
        context.inject(module);
        preprocessWithoutHarvesting(module);
        setModuleInputs(module, args);
        return module;
    }

    private <M extends Module> void preprocessWithoutHarvesting(M module) {
        ModuleRunner moduleRunner = new ModuleRunner(context, module, preprocessorsWithoutHarvesting(), Collections.emptyList());
        moduleRunner.preProcess();
    }

    private List<? extends PreprocessorPlugin> preprocessorsWithoutHarvesting() {
        //remove input harvesters from preprocessing
        List<PreprocessorPlugin> preprocessors = context.getService(PluginService.class).createInstancesOfType(PreprocessorPlugin.class);
        preprocessors.removeIf(preprocessor -> preprocessor instanceof InputHarvester);
        return preprocessors;
    }

    private void setModuleInputs(Module module, Object[] args) {
        assert(args.length % 2 == 0);
        for (int i = 0; i < args.length-1; i+=2) {
            String input = (String) args[i];
            module.setInput(input, args[i+1]);
            module.resolveInput(input);
        }
    }

    private JPanel createModulePanel(Module module) throws ModuleException {
        SwingInputHarvester swingInputHarvester = new SwingInputHarvester();
        context.inject(swingInputHarvester);
        InputPanel<JPanel, JPanel> inputPanel = new SwingInputPanel();
        swingInputHarvester.buildPanel(inputPanel, module);
        return inputPanel.getComponent();
    }
}
