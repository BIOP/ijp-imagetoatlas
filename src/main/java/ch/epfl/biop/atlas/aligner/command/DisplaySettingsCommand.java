package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import spimdata.util.Displaysettings;

/**
 * Command triggered by the user when he is clicking on the the table header of the slice
 * display card. It allows to set common display settings to all sources {@link DisplaySettingsCommand#sacs}
 * given as input of this command
 */
@Plugin(type = Command.class)
public class DisplaySettingsCommand extends DynamicCommand implements
        Initializable {

    public static Displaysettings IniValue;

    @Parameter(persist = false)
    double min;

    @Parameter(persist = false)
    double max;

    @Parameter(persist = false)
    ColorRGB color;

    @Parameter
    public SourceAndConverter[] sacs;

    @Parameter(type = ItemIO.OUTPUT)
    Displaysettings ds;

    @Parameter(required = false)
    Runnable postrun;

    @Override
    public void initialize() {
        final MutableModuleItem<ColorRGB> colorItem = //
                getInfo().getMutableInput("color", ColorRGB.class);
        colorItem.setValue(this, new ColorRGB(IniValue.color[0],IniValue.color[1],IniValue.color[2]));

        final MutableModuleItem<Double> minItem = //
                getInfo().getMutableInput("min", Double.class);
        minItem.setValue(this, IniValue.min);

        final MutableModuleItem<Double> maxItem = //
                getInfo().getMutableInput("max", Double.class);
        maxItem.setValue(this, IniValue.max);
    }

    @Override
    public void run() {
        ds = new Displaysettings(-1);
        ds.max = max;
        ds.min = min;
        ds.color = new int[]{color.getRed(), color.getGreen(), color.getBlue(), 255};

        Displaysettings.applyDisplaysettings(sacs, ds);
        SourceAndConverterServices.getBdvDisplayService()
                .updateDisplays(sacs);

        if (postrun!=null) {
            postrun.run();
        }
    }

}
