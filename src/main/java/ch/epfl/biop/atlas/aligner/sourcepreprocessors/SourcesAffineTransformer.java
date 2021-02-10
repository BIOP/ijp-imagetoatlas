package ch.epfl.biop.atlas.aligner.sourcepreprocessors;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SourcesAffineTransformer implements SourcesProcessor {

    final public AffineTransform3D at3d;

    public SourcesAffineTransformer(AffineTransform3D at3d) {
        this.at3d = at3d.copy();
    }

    @Override
    public SourceAndConverter[] apply(SourceAndConverter[] sourceAndConverters) {
        return Arrays.stream(sourceAndConverters)
                .map(sac -> SourceTransformHelper.createNewTransformedSourceAndConverter(at3d, new SourceAndConverterAndTimeRange(sac, 0)))
                .collect(Collectors.toList())
                .toArray(new SourceAndConverter[sourceAndConverters.length]);
    }
}
