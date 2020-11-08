package ch.epfl.biop.atlas.aligner.commands;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

import java.util.function.Function;

abstract public class RegistrationCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Atlas channels")
    int atlasImageChannel;

    @Parameter(label = "Slices channels")
    int sliceImageChannel;

    @Override
    abstract public void run();

    public Function<SourceAndConverter[], SourceAndConverter[]> getFixedFilter() {
        final int atlasChannel = atlasImageChannel;
        return (sacs) -> new SourceAndConverter[]{sacs[atlasChannel]};
    }

    public Function<SourceAndConverter[], SourceAndConverter[]> getMovingFilter() {
        final int sliceChannel = sliceImageChannel;
        return (sacs) -> new SourceAndConverter[]{sacs[sliceChannel]};
    }

}
