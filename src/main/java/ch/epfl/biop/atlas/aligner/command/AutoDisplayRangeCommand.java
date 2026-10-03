package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.Source;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Auto Display Range",
        description = "Sets the display range of channels of the selected slices from percentiles of their pixel values, "
                + "measured on a downsampled resolution level. Besides the display, it changes the images sent to DeepSlice, "
                + "which works best without saturation: run it before DeepSlice.")
public class AutoDisplayRangeCommand implements Command {

    /** Fewest pixels of the resolution level measured, and most pixels sampled from it */
    static final long MIN_LEVEL_PIXELS = 1 << 18, MAX_SAMPLES = 1 << 21;

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Channels",
            description = "0-based indices of the channels to adjust, comma separated (e.g. '0,2'), or '*' for all channels. "
                    + "Channels missing in a slice, and RGB channels, are skipped.")
    String slice_channels_csv = "*";

    @Parameter(label = "Low percentile", min = "0", max = "100",
            description = "Percentile of the pixel values displayed as black.")
    double low_percentile = 0.1;

    @Parameter(label = "High percentile", min = "0", max = "100",
            description = "Percentile of the pixel values displayed at full brightness: 99.9 saturates 0.1% of the pixels.")
    double high_percentile = 99.9;

    @Parameter(label = "Same range for all slices",
            description = "One range per channel, measured on all the selected slices together, so that slices look alike; "
                    + "otherwise one range per slice and channel.")
    boolean same_range_for_all_slices = true;

    @Parameter(type = ItemIO.OUTPUT, label = "Display ranges",
            description = "The ranges set: per channel, or per slice and channel.")
    String display_ranges;

    @Override
    public void run() {
        if (!(0 <= low_percentile && low_percentile < high_percentile && high_percentile <= 100)) {
            throw new IllegalArgumentException("Percentiles: 0 <= low < high <= 100, not " + low_percentile + " and " + high_percentile);
        }
        List<SliceSources> slices = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (slices.isEmpty()) throw new IllegalArgumentException("No selected slice: select the slices to adjust");
        int maxChannels = slices.stream().mapToInt(slice -> slice.nChannels).max().getAsInt();
        List<Integer> channels = slice_channels_csv.trim().equals("*")
                ? IntStream.range(0, maxChannels).boxed().collect(Collectors.toList())
                : Arrays.stream(slice_channels_csv.trim().split("\\s*,\\s*")).map(Integer::parseInt).collect(Collectors.toList());

        Map<String, double[]> ranges = new LinkedHashMap<>();
        for (int c : channels) {
            List<float[]> pooled = new ArrayList<>();
            for (SliceSources slice : slices) {
                if (c >= slice.nChannels) continue;
                float[] values = sample(slice.getOriginalSources()[c].getSpimSource());
                if (values == null) continue;
                if (same_range_for_all_slices) {
                    pooled.add(values);
                } else {
                    double[] range = percentiles(values);
                    slice.setDisplayRange(c, range[0], range[1]);
                    ranges.put(mp.getSlices().indexOf(slice) + "/" + c, range);
                }
            }
            if (same_range_for_all_slices && !pooled.isEmpty()) {
                double[] range = percentiles(concat(pooled));
                for (SliceSources slice : slices) if (c < slice.nChannels) slice.setDisplayRange(c, range[0], range[1]);
                ranges.put(String.valueOf(c), range);
            }
        }
        display_ranges = ranges.entrySet().stream()
                .map(e -> String.format(Locale.ROOT, "%s %s: %.4g-%.4g", same_range_for_all_slices ? "channel" : "slice/channel",
                        e.getKey(), e.getValue()[0], e.getValue()[1]))
                .collect(Collectors.joining("; "));
        if (display_ranges.isEmpty()) display_ranges = "nothing adjusted: no grayscale channel among those given";
    }

    /** Pixel values of the coarsest resolution level with enough pixels, at most MAX_SAMPLES of them; null for non-numeric pixels */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static float[] sample(Source<?> source) {
        int level = source.getNumMipmapLevels() - 1;
        while (level > 0 && Intervals.numElements(source.getSource(0, level)) < MIN_LEVEL_PIXELS) level--;
        RandomAccessibleInterval rai = source.getSource(0, level);
        if (!(Util.getTypeFromInterval(rai) instanceof RealType)) return null;
        long step = Math.max(1, Intervals.numElements(rai) / MAX_SAMPLES);
        float[] values = new float[(int) Math.min(Intervals.numElements(rai), MAX_SAMPLES + 1)];
        Cursor<? extends RealType> cursor = Views.flatIterable((RandomAccessibleInterval<? extends RealType>) rai).cursor();
        int i = 0;
        for (long k = 0; cursor.hasNext() && i < values.length; k++) {
            RealType value = cursor.next();
            if (k % step == 0) values[i++] = value.getRealFloat();
        }
        return Arrays.copyOf(values, i);
    }

    double[] percentiles(float[] values) {
        float[] sorted = values.clone();
        Arrays.sort(sorted);
        return new double[] {at(sorted, low_percentile), at(sorted, high_percentile)};
    }

    static double at(float[] sorted, double percentile) {
        return sorted[(int) Math.round(percentile / 100.0 * (sorted.length - 1))];
    }

    static float[] concat(List<float[]> arrays) {
        float[] all = new float[arrays.stream().mapToInt(a -> a.length).sum()];
        int offset = 0;
        for (float[] a : arrays) { System.arraycopy(a, 0, all, offset, a.length); offset += a.length; }
        return all;
    }
}
