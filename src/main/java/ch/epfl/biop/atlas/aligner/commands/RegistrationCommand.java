package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesChannelsSelect;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessor;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

abstract public class RegistrationCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Atlas channels")
    int atlasImageChannel;

    @Parameter(label = "Slices channels")
    int sliceImageChannel;

    @Override
    abstract public void run();

    public SourcesProcessor getFixedFilter() {
        return new SourcesChannelsSelect(atlasImageChannel);
    }

    public SourcesProcessor getMovingFilter() {
        return new SourcesChannelsSelect(sliceImageChannel);
    }

}
