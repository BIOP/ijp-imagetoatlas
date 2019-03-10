package ch.epfl.biop.atlastoimg2d.commands;


import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import ch.epfl.biop.wrappers.elastix.RegisterHelper;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Image To Atlas>Register Image To Atlas")
public class ImageToAtlasRegister implements Command {

    @Parameter(type = ItemIO.BOTH)
    AtlasToImg2D aligner;

    //@Parameter(type = ItemIO.OUTPUT)
    //RegisterHelper rh;

    @Override
    public void run() {
        System.out.println("Let's go");
        aligner.register();
        //rh = aligner.getRegistration();
    }
}
