package ch.epfl.biop.atlas.aligner.commands;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesChannelsSelect;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessor;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;

abstract public class RegistrationCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Atlas channels", min = "0")
    int atlasImageChannel;

    @Parameter(label = "Slices channels", min = "0")
    int sliceImageChannel;

    @Override
    final public void run() {
        if (atlasImageChannel>=mp.getNumberOfAtlasChannels()) {
            mp.log.accept("The atlas has only "+mp.getNumberOfAtlasChannels()+" channels!");
            return;
        }
        if (mp.getNumberOfSelectedSources()==0) {
            mp.log.accept("No slice selected");
            return;
        }
        if (sliceImageChannel>=mp.getChannelBoundForSelectedSlices()) {
            mp.log.accept("Missing channel in selected slice.");
            return;
        }
        runValidated();
    }

    abstract public void runValidated();

    public SourcesProcessor getFixedFilter() {
        return new SourcesChannelsSelect(atlasImageChannel);
    }

    public SourcesProcessor getMovingFilter() {
        return new SourcesChannelsSelect(sliceImageChannel);
    }

}
