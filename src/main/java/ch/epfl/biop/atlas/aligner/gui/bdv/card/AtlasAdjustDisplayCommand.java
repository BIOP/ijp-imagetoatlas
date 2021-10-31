package ch.epfl.biop.atlas.aligner.gui.bdv.card;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import org.scijava.Initializable;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class)
public class AtlasAdjustDisplayCommand extends InteractiveCommand implements
        Initializable, BdvMultislicePositionerView.ModeListener {

    @Parameter
    BdvMultislicePositionerView view;


    // -- Initializable methods --

    boolean listenerRegistered=false;

    @Override
    public void initialize() {
        int idx_Channel = 0;
        for (String key: view.msp.getAtlas().getMap().getImagesKeys()) {
            if (view.includedKey(key)) {
                addInput(key + "_enable", Boolean.class);
                final MutableModuleItem<Boolean> modalityEnable = //
                        getInfo().getMutableInput(key + "_enable", Boolean.class);
                modalityEnable.setLabel(key+" (Ch. "+idx_Channel+")");
                modalityEnable.setDefaultValue(true);

                addInput(key + "_slider", Double.class);
                final MutableModuleItem<Double> modalitySlider = //
                        getInfo().getMutableInput(key + "_slider", Double.class);
                modalitySlider.setLabel(" ");
                modalitySlider.setDefaultValue(0.5);
                modalitySlider.setMinimumValue(0.0);
                modalitySlider.setMaximumValue(1.0);
                modalitySlider.setStepSize(0.01);
                modalitySlider.setWidgetStyle("slider,format:#.00");

                idx_Channel++;
            }
        }

        addInput("slicingSteps", Integer.class);
        final MutableModuleItem<Integer> slicingSteps = //
                getInfo().getMutableInput("slicingSteps", Integer.class);
        slicingSteps.setWidgetStyle("slider");
        slicingSteps.setMinimumValue(1);
        slicingSteps.setMaximumValue(50);
        slicingSteps.setPersisted(false);
        slicingSteps.setLabel("Displayed slicing [atlas steps]");
        slicingSteps.setValue(this, (int) (view.msp.getReslicedAtlas().getStep())); // Initialisation to current state
        view.addToCleanUpHook(() -> view.removeModeListener(this));
    }

    // -- Runnable methods --

    @Parameter
    SourceAndConverterBdvDisplayService bdvDisplayService;

    @Override
    public void run() {
        if ((view!=null)&&(!listenerRegistered)) {
            view.addModeListener(this);
            listenerRegistered=true;
        }

        view.msp.getReslicedAtlas().setStep(((Integer)(getInput("slicingSteps"))));
        List<String> keys = view.msp.getAtlas().getMap().getImagesKeys();
        for (int iChannel = 0; iChannel<keys.size(); iChannel++) {
            String key = keys.get(iChannel);
            if (view.includedKey(key)) {
                view.getBdvh().getViewerPanel().state().setSourceActive(view.getDisplayedAtlasSources()[iChannel], (Boolean) getInput(key + "_enable"));
                ConverterSetup converterSetup = bdvDisplayService.getConverterSetup(view.getDisplayedAtlasSources()[iChannel]);
                double sliderValue = (Double) getInput(key + "_slider");
                converterSetup.setDisplayRange(0,view.msp.getAtlas().getMap().getImageMax(key)*(1.0-sliderValue));
            }
        }
    }

    @Override
    public void modeChanged(BdvMultislicePositionerView mp, int oldmode, int newmode) {

        if (newmode == BdvMultislicePositionerView.REVIEW_MODE_INT) {
            SourceAndConverterHelper.transferColorConverters(view.msp.getReslicedAtlas().extendedSlicedSources, view.msp.getReslicedAtlas().nonExtendedSlicedSources);
        } else {
            SourceAndConverterHelper.transferColorConverters(view.msp.getReslicedAtlas().nonExtendedSlicedSources, view.msp.getReslicedAtlas().extendedSlicedSources);
        }

        for (SourceAndConverter<?> sac : view.msp.getReslicedAtlas().extendedSlicedSources) {
            view.getBdvh().getViewerPanel().state().setSourceActive(sac, false);
        }
        for (SourceAndConverter<?> sac : view.msp.getReslicedAtlas().nonExtendedSlicedSources) {
            view.getBdvh().getViewerPanel().state().setSourceActive(sac, false);
        }

        List<String> keys = view.msp.getAtlas().getMap().getImagesKeys();
        for (int iChannel = 0; iChannel<keys.size(); iChannel++) {
            String key = keys.get(iChannel);
            if (view.includedKey(key)) {
                switch (newmode) {
                    case BdvMultislicePositionerView.POSITIONING_MODE_INT:
                        view.getBdvh().getViewerPanel()
                                .state()
                                .setSourceActive(view.msp.getReslicedAtlas().extendedSlicedSources[iChannel], (Boolean) this.getInput(key + "_enable"));
                        break;
                    case BdvMultislicePositionerView.REVIEW_MODE_INT:
                        view.getBdvh().getViewerPanel()
                                .state()
                                .setSourceActive(view.msp.getReslicedAtlas().nonExtendedSlicedSources[iChannel], (Boolean) this.getInput(key + "_enable"));
                        break;
                    default:
                }
            }
        }
    }

}
