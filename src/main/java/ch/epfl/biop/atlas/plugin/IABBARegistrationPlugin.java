package ch.epfl.biop.atlas.plugin;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.registration.Registration;
import org.scijava.plugin.SciJavaPlugin;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IABBARegistrationPlugin extends SciJavaPlugin, Registration<SourceAndConverter<?>[]> {

    void setLogger(Consumer<String> logger);

    void setSliceInfo(MultiSlicePositioner.SliceInfo sliceInfo);

    //void setZPositioner(Supplier<Double> zPosition);
}
