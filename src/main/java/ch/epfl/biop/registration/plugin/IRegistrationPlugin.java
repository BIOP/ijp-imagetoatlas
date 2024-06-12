package ch.epfl.biop.registration.plugin;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.registration.Registration;
import org.scijava.plugin.SciJavaPlugin;

public interface IRegistrationPlugin extends SciJavaPlugin, Registration<SourceAndConverter<?>[]> {

}
