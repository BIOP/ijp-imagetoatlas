package ch.epfl.biop.atlas.aligner.command;

import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.ColorRGB;
import spimdata.util.Displaysettings;

import java.util.function.Consumer;

/**
 * Command triggered by the user when he is clicking on the table header of the slice
 * display card.
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

    @Parameter(type = ItemIO.OUTPUT)
    Displaysettings ds;

    @Parameter(required = false)
    Consumer<Displaysettings> postrun;

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

        if (postrun!=null) {
            postrun.accept(ds);
        }
    }

}
