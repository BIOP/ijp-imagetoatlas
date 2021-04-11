package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesAffineTransformer;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>Elastix Registration (Affine)")
public class RegistrationElastixAffineCommand extends SingleChannelRegistrationCommand {

    @Parameter(label = "Show registration results as ImagePlus")
    boolean showImagePlusRegistrationResult;

    @Parameter(label = "Background offset value")
    double background_offset_value_moving = 0;

    double background_offset_value_fixed = 0;

    public void runValidated() {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("showImagePlusRegistrationResult", showImagePlusRegistrationResult);
        parameters.put("background_offset_value_moving", background_offset_value_moving);
        parameters.put("background_offset_value_fixed", background_offset_value_fixed);

        mp.register(this,
                getFixedFilter(),
                getMovingFilter(),
                parameters);



        //mp.registerElastixAffine(getFixedFilter(), getMovingFilter(), showIJ1Result);
    }

}
