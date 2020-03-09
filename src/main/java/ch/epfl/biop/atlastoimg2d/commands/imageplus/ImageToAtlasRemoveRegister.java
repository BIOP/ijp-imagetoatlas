package ch.epfl.biop.atlastoimg2d.commands.imageplus;

import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Image To Atlas>Remove Last Registration")
public class ImageToAtlasRemoveRegister implements Command{

    @Parameter(type = ItemIO.BOTH)
    AtlasToImg2D aligner;

    @Override
    public void run() {
        if (aligner.getRegistrations().size()>0) {
            aligner.rmLastRegistration();
            aligner.showLastImage();
        } else {
            System.err.println("Error : no registration left.");
        }
    }
}
