package ch.epfl.biop.atlas.plugin;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.registration.Registration;
import org.scijava.plugin.SciJavaPlugin;

import java.util.function.Consumer;

public interface IABBARegistrationPlugin extends SciJavaPlugin, Registration<SourceAndConverter<?>[]> {

    //void setMultiSlicePositioner(MultiSlicePositioner msp); // TODO

    void setLogger(Consumer<String> logger);
}
