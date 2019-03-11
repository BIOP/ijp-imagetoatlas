package ch.epfl.biop.atlastoimg2d.commands;


import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import ch.epfl.biop.registration.BigWarp2DRegistration;
import ch.epfl.biop.registration.CropAndScaleRegistration;
import ch.epfl.biop.registration.Elastix2DRegistration;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.wrappers.elastix.RegisterHelper;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Image To Atlas>Add Registration")
public class ImageToAtlasRegister implements Command {

    @Parameter(type = ItemIO.BOTH)
    AtlasToImg2D aligner;

    @Parameter(choices={"Crop and Scale","Elastix","BigWarp"})
    String registrationType;

    @Parameter
    Context ctx;

    @Override
    public void run() {
        Registration newReg;
        switch (registrationType) {
            case "Elastix":
                newReg = new Elastix2DRegistration();
                ((Elastix2DRegistration) newReg).ctx=ctx; // Needed because it's using a CommandService launching a Command
                aligner.addRegistration( newReg );
                break;
            case "BigWarp":
                aligner.addRegistration( new BigWarp2DRegistration());
                break;
            case "Crop and Scale":
                aligner.addRegistration( new CropAndScaleRegistration());
                break;
        }
    }
}
