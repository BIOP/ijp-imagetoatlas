package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import ij.ImagePlus;
import mpicbg.spim.data.generic.AbstractSpimData;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.service.SourceService;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>ABBA - Import Current ImageJ Window",
        description = "Imports an image opened in ImageJ as a single slice, all channels included. "
                + "The image should be calibrated (pixel size).",
        iconPath = "/graphics/ImportSliceFromImagePlus.png")
public class ImportSliceFromImagePlusCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slice position (mm)", style="format:0.000", stepSize = "0.1",
            description = "Initial position of the slice along the atlas slicing axis, in mm, as the Z shown in the slice information (0 = front of the atlas).")
    double slice_position_mm;

    @Parameter(label = "Image",
            description = "ImageJ image to import; by default the current image.")
    ImagePlus image;

    @Parameter
    SourceService sac_service;

    @Override
    public void run() {

        AbstractSpimData<?> asd = ImagePlusToSpimData.getSpimData(image);
        sac_service.register(asd);
        sac_service.setDatasetName(asd, image.getTitle());

        SourceAndConverter[] sacs = sac_service.getSourcesFromDataset(asd).toArray(new SourceAndConverter[0]);

        mp.createSlice(sacs, mp.fromAtlasZ(slice_position_mm));
    }

}
