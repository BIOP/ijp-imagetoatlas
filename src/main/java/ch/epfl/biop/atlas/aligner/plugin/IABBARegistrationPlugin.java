package ch.epfl.biop.atlas.aligner.plugin;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.registration.Registration;
import org.scijava.plugin.SciJavaPlugin;

public interface IABBARegistrationPlugin extends SciJavaPlugin, Registration<SourceAndConverter<?>[]> {

    void setSliceInfo(MultiSlicePositioner.SliceInfo sliceInfo);

}
