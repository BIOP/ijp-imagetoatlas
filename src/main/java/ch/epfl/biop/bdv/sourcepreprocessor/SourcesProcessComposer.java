package ch.epfl.biop.bdv.sourcepreprocessor;

import bdv.viewer.SourceAndConverter;

public class SourcesProcessComposer implements SourcesProcessor {

    final public SourcesProcessor f1;
    final public SourcesProcessor f2;

    public SourcesProcessComposer(SourcesProcessor f2, SourcesProcessor f1 ) {
        this.f1 = f1;
        this.f2 = f2;
    }

    @Override
    public SourceAndConverter[] apply(SourceAndConverter[] sourceAndConverters) {
        return f2.apply(f1.apply(sourceAndConverters));
    }

    public String toString() {
        return "["+f2.toString()+"].["+f1.toString()+"]";
    }
}
