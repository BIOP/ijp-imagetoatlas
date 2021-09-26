package ch.epfl.biop.atlas.aligner.sourcepreprocessors;

import bdv.viewer.SourceAndConverter;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SourcesResampler implements SourcesProcessor {

    final public SourceAndConverter model;

    public SourcesResampler(SourceAndConverter model) {
        this.model = model;
    }

    @Override
    public SourceAndConverter[] apply(SourceAndConverter[] sourceAndConverters) {
        SourceResampler resampler = new SourceResampler(null,
                model,model.getSpimSource().getName(), false, false, false, 0
        );
        return Arrays.stream(sourceAndConverters)
                .map(resampler)
                .collect(Collectors.toList())
                .toArray(new SourceAndConverter[sourceAndConverters.length]);
    }

    public String toString() {
        return "Resample().";
    }
}
