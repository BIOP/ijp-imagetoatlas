package ch.epfl.biop.atlas.aligner.sourcepreprocessor;

import bdv.viewer.SourceAndConverter;

import java.util.function.Function;

/**
 * Interface useful fo serialization of source and converters processors
 */

public interface SourcesProcessor extends Function<SourceAndConverter[], SourceAndConverter[]> {
}
