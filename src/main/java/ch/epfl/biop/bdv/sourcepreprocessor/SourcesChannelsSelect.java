package ch.epfl.biop.bdv.sourcepreprocessor;

import bdv.viewer.SourceAndConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SourcesChannelsSelect implements SourcesProcessor {

    final public List<Integer> channels_indices;

    public SourcesChannelsSelect(List<Integer> channels_indices) {
        this.channels_indices = channels_indices;
    }

    public SourcesChannelsSelect(Integer... channels_indices) {
        this.channels_indices = Arrays.asList(channels_indices);
    }

    public SourcesChannelsSelect(int... channels_indices) {
        this.channels_indices = new ArrayList<>(channels_indices.length);
        for (int channels_index : channels_indices) {
            this.channels_indices.add(channels_index);
        }
    }

    public SourcesChannelsSelect(int channel) {
        this.channels_indices = new ArrayList<>(1);
        this.channels_indices.add(channel);
    }

    @Override
    public SourceAndConverter[] apply(SourceAndConverter[] sourceAndConverters) {
        SourceAndConverter[] sourcesSelected = new SourceAndConverter[channels_indices.size()];
        int idx = 0;
        for (Integer index : channels_indices) {
            sourcesSelected[idx] = sourceAndConverters[index];
            idx++;
        }
        return sourcesSelected;
    }

    public String toString() {
        return "Ch["+channels_indices.stream().map(Object::toString).collect(Collectors.joining(","))+"]";
    }
}
