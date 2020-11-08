package ch.epfl.biop.atlas.aligner.commands;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.spimdata.imageplus.SpimDataFromImagePlusGetter;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>Import Current IJ1 Image")
public class ImportImagePlusCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    double sliceAxis;

    @Parameter
    ImagePlus imagePlus;

    @Parameter
    SourceAndConverterService sac_service;

    @Override
    public void run() {

        AbstractSpimData asd = (new SpimDataFromImagePlusGetter()).apply(imagePlus);
        sac_service.register(asd);
        sac_service.setSpimDataName(asd, imagePlus.getTitle());

        SourceAndConverter[] sacs = sac_service.getSourceAndConverterFromSpimdata(asd).toArray(new SourceAndConverter[0]);

        mp.createSlice(sacs, sliceAxis);
    }

}
