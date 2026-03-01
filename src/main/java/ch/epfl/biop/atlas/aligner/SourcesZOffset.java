package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.source.processor.SourcesProcessor;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.source.SourceAndTimeRange;
import sc.fiji.bdvpg.source.transform.SourceTransformHelper;

public class SourcesZOffset implements SourcesProcessor {
    final transient SliceSources slice;
    boolean iniWithTransform = false;
    public AffineTransform3D at3d = new AffineTransform3D();

    public SourcesZOffset(SliceSources slice) {
        this.slice = slice;
    }

    public SourcesZOffset(AffineTransform3D transform) {
        slice = null;
        iniWithTransform = true;
        at3d = transform.copy();
    }

    @Override
    public SourceAndConverter<?>[] apply(SourceAndConverter<?>[] sourceAndConverters) {
        SourceAndConverter<?>[] out = new SourceAndConverter<?>[sourceAndConverters.length];
        if (!iniWithTransform) {
            at3d.translate(0, 0, -slice.getSlicingAxisPosition());
            iniWithTransform = true; // or problem with multichannel!!
        }
        for (int i = 0; i < out.length; i++) {
            out[i] = SourceTransformHelper.createNewTransformedSourceAndConverter(at3d, new SourceAndTimeRange(sourceAndConverters[i], 0));
        }
        return out;
    }


    public String toString() {
        return "Z0";
    }
}
