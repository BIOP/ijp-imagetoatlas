package ch.epfl.biop.atlas.aligner.command;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Elastix Registration (Affine)",
        description = "Uses Elastix for affine in plane registration of selected slices")
public class RegisterSlicesElastixAffineCommand extends RegistrationMultiChannelCommand {

    @Parameter(label = "Registration re-sampling (micrometers)")
    double pixel_size_micrometer = 40;

    @Parameter(label = "Show registration results as ImagePlus")
    boolean show_imageplus_registration_result;

    @Parameter(label = "Background offset value")
    double background_offset_value_moving = 0;

    double background_offset_value_fixed = 0;

    public void runValidated() {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("showImagePlusRegistrationResult", show_imageplus_registration_result);
        parameters.put("background_offset_value_moving", background_offset_value_moving);
        parameters.put("background_offset_value_fixed", background_offset_value_fixed);
        parameters.put("pxSizeInCurrentUnit", pixel_size_micrometer/1000.0);

        mp.registerSelectedSlices(this,
                getFixedFilter(),
                getMovingFilter(),
                parameters);
    }

}
