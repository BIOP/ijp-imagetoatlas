package ch.epfl.biop.atlastoimg2d.commands;


import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import ch.epfl.biop.registration.BigWarp2DRegistration;
import ch.epfl.biop.registration.Elastix2DRegistration;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.wrappers.elastix.RegisterHelper;
import org.scijava.Context;
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

    @Parameter(choices={"Elastix","BigWarp"})
    String registrationType;

    @Parameter
    Context ctx;

    @Override
    public void run() {
        Registration newReg;
        switch (registrationType) {
            case "Elastix": //aligner = new AtlasToImagePlusElastixRegister();
                newReg = new Elastix2DRegistration();
                ((Elastix2DRegistration) newReg).ctx=ctx;
                aligner.addRegistration( newReg );
                break;
            case "BigWarp": //aligner = new AtlasToImagePlusBigWarpRegister();
                aligner.addRegistration( new BigWarp2DRegistration());
                break;
        }
        //aligner.register();
        //rh = aligner.getRegistration();
    }
}
