package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.registration.sourceandconverter.affine.Elastix2DAffineRegistration;
import ch.epfl.biop.scijava.command.source.register.ElastixHelper;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

import static ch.epfl.biop.atlas.aligner.ABBAHelper.getResource;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Elastix Registration (Affine)",
        description = "Uses Elastix for affine in plane registration of selected slices",
        iconPath = "/graphics/ABBAAffine.png")
public class RegisterSlicesElastixAffineCommand extends RegistrationMultiChannelCommand {

    @Parameter(label = "Registration re-sampling (micrometers)")
    double pixel_size_micrometer = 40;

    @Parameter(label = "Show registration results as ImagePlus")
    boolean show_imageplus_registration_result;

    @Parameter(label = "Background offset value")
    double background_offset_value_moving = 0;

    double background_offset_value_fixed = 0;

    public void runValidated() {

        if (atlas_channels.size()!=slice_channels.size()) {
            mp.errorMessageForUser.accept("Number of channels issue", "The number of slice channel(s) should be equal to the number of atlas channel(s).");
            return;
        }

        ElastixHelper.checkOrSetLocal(this.mp.getContext());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("show_image_registration", show_imageplus_registration_result);
        parameters.put("background_offset_value_moving", background_offset_value_moving);
        parameters.put("background_offset_value_fixed", background_offset_value_fixed);
        parameters.put("px_size_in_current_unit", pixel_size_micrometer/1000.0);

        mp.registerSelectedSlices(Elastix2DAffineRegistration.class,
                getFixedFilter(),
                getMovingFilter(),
                parameters);
    }


    @Override
    protected String getMessage() {
        return "<html>" +
                "    <p><img src='"+getResource("graphics/ABBAAffine.png")+"' width='80' height='80'></img></p>" +
                "</html>\n";
    }

}
