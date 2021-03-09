package ch.epfl.biop.atlas.plugin;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.registration.Registration;
import org.scijava.plugin.SciJavaPlugin;

import java.util.function.Consumer;

public interface IABBARegistrationPlugin extends SciJavaPlugin, Registration<SourceAndConverter<?>[]> {

    void setLogger(Consumer<String> logger);
}
