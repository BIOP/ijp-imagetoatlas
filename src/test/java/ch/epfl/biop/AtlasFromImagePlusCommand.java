package ch.epfl.biop;

import ch.epfl.biop.atlas.scijava.AtlasChooserCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.sourceandconverter.SourceVoxelProcessor;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


/*@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Import Current ImagePlus as Atlas",
        description = "Simple way to create a dummy atlas.")*/
public class AtlasFromImagePlusCommand {//} implements Command {

    /*@Parameter
    ImagePlus structural_images;

    @Parameter
    ImagePlus label_image;

    @Parameter
    Double atlas_precision_mm;

    @Parameter
    ObjectService os;

    @Override
    public void run() {
        Atlas atlas = AtlasFromSourcesHelper.fromImagePlus(structural_images, label_image, atlas_precision_mm);
        os.addObject(atlas, structural_images.getTitle());
        AtlasChooserCommand.registerAtlas(atlas.getName(), () -> atlas);

    }*/

}
