package ch.epfl.biop.atlas.aligner.commands;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
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
public class AtlasDisplayDynamicCommand extends InteractiveCommand implements
        Initializable, MultiSlicePositioner.ModeListener {

    public AtlasDisplayDynamicCommand(){
        excludedKeys.add("X");
        excludedKeys.add("Y");
        excludedKeys.add("Z");
        excludedKeys.add("Left Right");
    }

    @Parameter
    MultiSlicePositioner mp;

    public static List<String> excludedKeys = new ArrayList<>();

    // -- Initializable methods --

    @Override
    public void initialize() {
        int idx_Channel = 0;
        for (String key: mp.getAtlas().getMap().getImagesKeys()) {
            if (includedKey(key)) {
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
                modalitySlider.setWidgetStyle("slider");

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

    }

    // -- Runnable methods --

    @Parameter
    SourceAndConverterBdvDisplayService bdvDisplayService;

    boolean listenerRegistered=false;
    @Override
    public void run() {
        if ((mp!=null)&&(!listenerRegistered)) {
            mp.addModeListener(this);
            listenerRegistered=true;
        }

        assert mp != null;
        mp.getReslicedAtlas().setStep(((Integer)(getInput("slicingSteps"))));

        List<String> keys = mp.getAtlas().getMap().getImagesKeys();
        for (int iChannel = 0; iChannel<keys.size(); iChannel++) {
            String key = keys.get(iChannel);
            if (includedKey(key)) {
                mp.getBdvh().getViewerPanel().state().setSourceActive(getSources()[iChannel], (Boolean) getInput(key + "_enable"));
                ConverterSetup converterSetup = bdvDisplayService.getConverterSetup(getSources()[iChannel]);
                double sliderValue = (Double) getInput(key + "_slider");
                converterSetup.setDisplayRange(0,mp.getAtlas().getMap().getImageMax(key)*(1.0-sliderValue));
            }
        }
    }

    public SourceAndConverter<?>[] getSources() {
        switch (mp.getDisplayMode()) {
            case MultiSlicePositioner.POSITIONING_MODE_INT:
                return mp.getReslicedAtlas().extendedSlicedSources;
            case MultiSlicePositioner.REVIEW_MODE_INT:
                return mp.getReslicedAtlas().nonExtendedSlicedSources;
            default:
                return null;
        }
    }

    private boolean includedKey(String key) {
        return !(excludedKeys.contains(key));
    }

    @Override
    public void modeChanged(MultiSlicePositioner mp, int oldmode, int newmode) {

        if (newmode == MultiSlicePositioner.REVIEW_MODE_INT) {
            SourceAndConverterHelper.transferColorConverters(mp.getReslicedAtlas().extendedSlicedSources, mp.getReslicedAtlas().nonExtendedSlicedSources);
        } else {
            SourceAndConverterHelper.transferColorConverters(mp.getReslicedAtlas().nonExtendedSlicedSources, mp.getReslicedAtlas().extendedSlicedSources);
        }

        for (SourceAndConverter<?> sac : mp.getReslicedAtlas().extendedSlicedSources) {
            mp.getBdvh().getViewerPanel().state().setSourceActive(sac, false);
        }
        for (SourceAndConverter<?> sac : mp.getReslicedAtlas().nonExtendedSlicedSources) {
            mp.getBdvh().getViewerPanel().state().setSourceActive(sac, false);
        }

        List<String> keys = mp.getAtlas().getMap().getImagesKeys();
        for (int iChannel = 0; iChannel<keys.size(); iChannel++) {
            String key = keys.get(iChannel);
            if (includedKey(key)) {
                switch (newmode) {
                    case MultiSlicePositioner.POSITIONING_MODE_INT:
                        mp.getBdvh().getViewerPanel().state().setSourceActive(mp.getReslicedAtlas().extendedSlicedSources[iChannel], (Boolean) this.getInput(key + "_enable"));
                        break;
                    case MultiSlicePositioner.REVIEW_MODE_INT:
                        mp.getBdvh().getViewerPanel().state().setSourceActive(mp.getReslicedAtlas().nonExtendedSlicedSources[iChannel], (Boolean) this.getInput(key + "_enable"));
                        break;
                    default:
                }
            }
        }

    }

    @Override
    public void sliceDisplayModeChanged(MultiSlicePositioner mp, int oldmode, int newmode) {

    }
}
