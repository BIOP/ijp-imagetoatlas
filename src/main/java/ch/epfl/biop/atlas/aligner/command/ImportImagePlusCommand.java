package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>ABBA - Import Current ImageJ Window",
        description = "Import the current ImageJ image as a slice into ABBA")
public class ImportImagePlusCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Initial axis position (0 = front, mm units)", style="format:0.000", stepSize = "0.1")
    double slice_axis;

    @Parameter
    ImagePlus image;

    @Parameter
    SourceAndConverterService sac_service;

    @Override
    public void run() {

        AbstractSpimData<?> asd = ImagePlusToSpimData.getSpimData(image);
        sac_service.register(asd);
        sac_service.setSpimDataName(asd, image.getTitle());

        SourceAndConverter[] sacs = sac_service.getSourceAndConverterFromSpimdata(asd).toArray(new SourceAndConverter[0]);

        mp.createSlice(sacs, slice_axis);
    }

}
