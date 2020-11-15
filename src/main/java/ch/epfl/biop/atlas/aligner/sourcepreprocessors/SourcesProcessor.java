package ch.epfl.biop.atlas.aligner.sourcepreprocessors;

import bdv.viewer.SourceAndConverter;

import java.util.function.Function;

/**
 * Interface useful fo serialization of source and converters processors
 */

public interface SourcesProcessor extends Function<SourceAndConverter[], SourceAndConverter[]> {
}
