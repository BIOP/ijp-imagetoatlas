package ch.epfl.biop.abba.pluginexample;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.plugin.ABBACommand;
import ij.IJ;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
@SuppressWarnings("unused")
@Plugin(type = ABBACommand.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>(Experimental)>[List slices - Demo ABBA plugin]",
        description = "Plugin example for ABBA.")
public class ABBADemoExtraCommand implements ABBACommand {

    @Parameter
    MultiSlicePositioner mp; //COMPULSORY : name = mp

    @Override
    public void run() {
        IJ.log("Current ABBA slices: "+mp.getSlices());
    }

}
