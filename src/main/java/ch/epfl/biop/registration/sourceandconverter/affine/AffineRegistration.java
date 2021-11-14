package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.aligner.plugin.RegistrationTypeProperties;
import com.google.gson.Gson;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To make an affine transform programmatically conveniently
 *
 */
@Plugin(type = IABBARegistrationPlugin.class)
@RegistrationTypeProperties(
        isManual = false,
        isEditable = false,
        userInterface = {
        })

public class AffineRegistration extends AffineTransformSourceAndConverterRegistration{

    protected static Logger logger = LoggerFactory.getLogger(AffineRegistration.class);

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        super.setFixedImage(fimg);
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        super.setMovingImage(mimg);
    }

    public static String affineTransform3DToString(AffineTransform3D transform) {
        return new Gson().toJson(transform.getRowPackedCopy());
    }

    public static AffineTransform3D stringToAffineTransform3D(String string) {
        AffineTransform3D transform3D = new AffineTransform3D();
        double[] matrix = new Gson().fromJson(string, double[].class);
        transform3D.set(matrix);
        return transform3D;
    }

    @Override
    public boolean register() {
        at3d = stringToAffineTransform3D(parameters.get("transform"));
        isDone = true;
        return true;
    }

    @Override
    public void abort() {

    }

    public String toString() {
        return "Affine";
    }

}
