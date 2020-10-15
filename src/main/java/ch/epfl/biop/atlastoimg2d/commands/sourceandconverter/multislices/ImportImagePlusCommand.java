package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import ch.epfl.biop.spimdata.SpimDataFromImagePlusGetter;
import ij.IJ;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import Current IJ1 Image")
public class ImportImagePlusCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    double sliceAxis;

    @Parameter(callback = "clicked")
    Button run;

    @Parameter
    SourceAndConverterService sac_service;

    @Override
    public void run() {
        // Cannot be accessed
        clicked();
    }

    public void clicked() {
        ImagePlus imagePlus = IJ.getImage();
        AbstractSpimData asd = (new SpimDataFromImagePlusGetter()).apply(imagePlus);
        sac_service.register(asd);
        sac_service.setSpimDataName(asd, imagePlus.getTitle());

        SourceAndConverter[] sacs = sac_service.getSourceAndConverterFromSpimdata(asd).toArray(new SourceAndConverter[0]);

        mp.createSlice(sacs, sliceAxis);
    }
}
