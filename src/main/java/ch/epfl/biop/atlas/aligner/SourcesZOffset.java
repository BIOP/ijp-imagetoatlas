package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.source.processor.SourcesIdentity;
import ch.epfl.biop.source.processor.SourcesProcessComposer;
import ch.epfl.biop.source.processor.SourcesProcessor;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;
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

    /**
     * @return the processor without its z offsets, followed by the z offset of the slice
     */
    public static SourcesProcessor replaceIn(SourcesProcessor processor, SliceSources slice) {
        return SourcesProcessorHelper.compose(removeFrom(processor), new SourcesZOffset(slice));
    }

    /**
     * @return the processor where z offsets are replaced by identities
     */
    public static SourcesProcessor removeFrom(SourcesProcessor processor) {
        if (processor instanceof SourcesZOffset) {
            return new SourcesIdentity();
        } else if (processor instanceof SourcesProcessComposer) {
            SourcesProcessComposer composer = (SourcesProcessComposer) processor;
            return new SourcesProcessComposer(removeFrom(composer.f2), removeFrom(composer.f1));
        } else {
            return processor;
        }
    }
}
