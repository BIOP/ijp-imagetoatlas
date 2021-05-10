package ch.epfl.biop.atlas.aligner.commands;

import bdv.tools.brightness.ConverterSetup;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

/** less general but more convenient way of controlling the display settings of the Allen Brain Atlas
 *
 */

@Plugin(type = Command.class)
public class AllenAtlasDisplayCommand extends InteractiveCommand implements MultiSlicePositioner.ModeListener {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(persist = false)
    boolean showNissl = true;

    @Parameter(style="slider", min="0", max="1", stepSize = "0.01", persist = false)
    double nisslSlider = 0.5;

    @Parameter(persist = false)
    boolean showAutoFluo = true;

    @Parameter(style="slider", min="0", max="1", stepSize = "0.01", persist = false)
    double autofluoSlider = 0.5;

    @Parameter(persist = false)
    boolean showLabelBorder = true;

    @Parameter(style="slider", min="0", max="1", stepSize = "0.01", persist = false)
    double labelBorderSlider = 0.5;

    @Parameter(min = "10", max = "500", stepSize = "10", style = "slider", label = "Displayed slicing [micron]", persist = false)
    int zSamplingSteps = 200;

    @Parameter
    SourceAndConverterBdvDisplayService bdvDisplayService;

    boolean listenerRegistered=false;
    public void run() {
        if ((mp!=null)&&(!listenerRegistered)) {
            mp.addModeListener(this);
            listenerRegistered=true;
        }

        mp.getReslicedAtlas().setStep(zSamplingSteps/10);

        mp.getBdvh().getViewerPanel().state().setSourceActive(getSources()[0], showNissl);
        mp.getBdvh().getViewerPanel().state().setSourceActive(getSources()[1], showAutoFluo);
        mp.getBdvh().getViewerPanel().state().setSourceActive(getSources()[2], showLabelBorder);

        ConverterSetup nisslBC = bdvDisplayService.getConverterSetup(getSources()[0]);
        ConverterSetup autoFluoBC = bdvDisplayService.getConverterSetup(getSources()[1]);
        ConverterSetup labelBorderBC = bdvDisplayService.getConverterSetup(getSources()[2]);

        nisslBC.setDisplayRange(0,2*28000*(1-nisslSlider));
        autoFluoBC.setDisplayRange(0,2*512*(1-autofluoSlider));
        labelBorderBC.setDisplayRange(0,1024*(1-labelBorderSlider));

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

    @Override
    public void modeChanged(MultiSlicePositioner mp, int oldmode, int newmode) {

        if (newmode == MultiSlicePositioner.REVIEW_MODE_INT) {
            SourceAndConverterHelper.transferColorConverters(mp.getReslicedAtlas().extendedSlicedSources, mp.getReslicedAtlas().nonExtendedSlicedSources);
        } else {
            SourceAndConverterHelper.transferColorConverters(mp.getReslicedAtlas().nonExtendedSlicedSources, mp.getReslicedAtlas().extendedSlicedSources);
        }

        for (SourceAndConverter sac : mp.getReslicedAtlas().extendedSlicedSources) {
            mp.getBdvh().getViewerPanel().state().setSourceActive(sac, false);
        }
        for (SourceAndConverter sac : mp.getReslicedAtlas().nonExtendedSlicedSources) {
            mp.getBdvh().getViewerPanel().state().setSourceActive(sac, false);
        }

        switch (newmode) {
            case MultiSlicePositioner.POSITIONING_MODE_INT:
                    mp.getBdvh().getViewerPanel().state().setSourceActive(mp.getReslicedAtlas().extendedSlicedSources[0], showNissl);
                    mp.getBdvh().getViewerPanel().state().setSourceActive(mp.getReslicedAtlas().extendedSlicedSources[1], showAutoFluo);
                    mp.getBdvh().getViewerPanel().state().setSourceActive(mp.getReslicedAtlas().extendedSlicedSources[2], showLabelBorder);
                break;
            case MultiSlicePositioner.REVIEW_MODE_INT:
                    mp.getBdvh().getViewerPanel().state().setSourceActive(mp.getReslicedAtlas().nonExtendedSlicedSources[0], showNissl);
                    mp.getBdvh().getViewerPanel().state().setSourceActive(mp.getReslicedAtlas().nonExtendedSlicedSources[1], showAutoFluo);
                    mp.getBdvh().getViewerPanel().state().setSourceActive(mp.getReslicedAtlas().nonExtendedSlicedSources[2], showLabelBorder);
                break;
            default:
        }
    }

    @Override
    public void sliceDisplayModeChanged(MultiSlicePositioner mp, int oldmode, int newmode) {

    }

}

