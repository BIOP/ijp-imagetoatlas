package ch.epfl.biop.atlas.aligner.commands;

import bdv.viewer.SourceAndConverter;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import spimdata.util.Displaysettings;
import spimdata.util.DisplaysettingsHelper;

@Plugin(type = Command.class)
public class DisplaySettingsCommand implements Command {

    @Parameter
    double min;

    @Parameter
    double max;

    @Parameter
    ColorRGB color;

    @Parameter
    SourceAndConverter[] sacs;

    @Parameter(type = ItemIO.OUTPUT)
    Displaysettings ds;

    @Parameter
    Runnable postrun;

    @Override
    public void run() {
        ds = new Displaysettings(-1);
        ds.max = max;
        ds.min = min;
        ds.color = new int[]{color.getRed(), color.getGreen(), color.getBlue(), 255};

        DisplaysettingsHelper.applyDisplaysettings(sacs, ds);
        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .updateDisplays(sacs);

        if (postrun!=null) {
            postrun.run();
        }
    }
}
