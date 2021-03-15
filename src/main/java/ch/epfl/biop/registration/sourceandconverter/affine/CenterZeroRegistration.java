package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixAffineCommand;
import ch.epfl.biop.atlas.aligner.commands.RegistrationElastixAffineRemoteCommand;
import ch.epfl.biop.atlas.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.plugin.RegistrationTypeProperties;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

/**
 * Fake registration : simply centers the image in order to put its center
 * at the origin of the global coordinate system.
 */

public class CenterZeroRegistration extends AffineTransformSourceAndConverterRegistration {

    @Override
    public boolean register() {

        SourceAndConverter sac = mimg[0];

        RealPoint center = SourceAndConverterHelper.getSourceAndConverterCenterPoint(sac);

        at3d = new AffineTransform3D();

        at3d.translate(
                -center.getDoublePosition(0),
                -center.getDoublePosition(1),
                -center.getDoublePosition(2));

        isDone = true;

        return true;
    }

    /**
     * TODO : find a way to remove this function and use the one of the super class
     * @param img
     * @return
     */
    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed(SourceAndConverter[] img) {
        SourceAndConverter[] out = new SourceAndConverter[img.length];
        for (int idx = 0;idx<img.length;idx++) {
            out[idx] = SourceTransformHelper.createNewTransformedSourceAndConverter(at3d, new SourceAndConverterAndTimeRange(img[idx], timePoint));
        }
        return out;
    }

    @Override
    public void abort() {
        // Cannot be aborted - is very fast anyway
    }

}
