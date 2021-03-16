package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Elastix Registration (Affine)")
public class RegistrationElastixAffineCommand extends RegistrationCommand {

    @Parameter(label = "Show registration results as ImagePlus")
    boolean showImagePlusRegistrationResult;

    public void runValidated() {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("showImagePlusRegistrationResult", showImagePlusRegistrationResult);

        mp.register(this,
                getFixedFilter(),
                getMovingFilter(),
                parameters);



        //mp.registerElastixAffine(getFixedFilter(), getMovingFilter(), showIJ1Result);
    }

}
