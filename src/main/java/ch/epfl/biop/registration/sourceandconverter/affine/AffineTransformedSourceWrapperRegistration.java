package ch.epfl.biop.registration.sourceandconverter.affine;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Fake registration which simply wraps a transform which is used to modify either:
 * - the place of the image in the user GUI, or to
 * TODO : write a bit more clearly what this does
 */
public class AffineTransformedSourceWrapperRegistration extends AffineTransformSourceAndConverterRegistration {

    Map<SourceAndConverter, SourceAndConverter> alreadyTransformedSources = new HashMap<>();

    @Override
    public boolean register() {
        isDone = true;
        return true;
    }

    /**
     * These function are kept in order to avoid serializing many times
     * unnecessary affinetransform
     * @param at3d_in
     */
    public void setAffineTransform(AffineTransform3D at3d_in) {
        this.at3d = at3d_in;
        alreadyTransformedSources.keySet().forEach(sac -> {
                SourceTransformHelper.set(at3d_in, new SourceAndConverterAndTimeRange(alreadyTransformedSources.get(sac), timePoint));
        });
    }

    public AffineTransform3D getAffineTransform() {
        return at3d.copy();
    }

    /**
     * Overriding to actually mutate SourceAndConverter,
     * it's the only registration which does that, because
     * it's actually not really a registration
     * @param img
     * @return
     */
    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed(SourceAndConverter[] img) {

        SourceAndConverter[] out = new SourceAndConverter[img.length];

        for (int idx = 0;idx<img.length;idx++) {
            if (alreadyTransformedSources.keySet().contains(img[idx])) {
                out[idx] = alreadyTransformedSources.get(img[idx]);
                SourceTransformHelper.set(at3d, new SourceAndConverterAndTimeRange(out[idx], timePoint));
            } else {
                out[idx] = SourceTransformHelper.createNewTransformedSourceAndConverter(at3d, new SourceAndConverterAndTimeRange(img[idx], timePoint));
                alreadyTransformedSources.put(img[idx], out[idx]);
            }
        }

        return out;
    }

    @Override
    public void abort() {

    }

    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {

    }
}
