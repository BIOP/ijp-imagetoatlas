package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** Slices named in a command input: 0-based indices along the slicing axis, comma separated, '*' for all, empty for the selected ones. */
class SliceIndices {

    static final String DESCRIPTION = "0-based indices of the slices along the slicing axis, comma separated (e.g. '0,3'), "
            + "'*' for all slices, or empty for the selected slices. Indices follow the slice order and change when a slice moves.";

    static List<SliceSources> parse(MultiSlicePositioner mp, String csv) {
        List<SliceSources> slices = mp.getSlices();
        String s = csv == null ? "" : csv.trim();
        if (s.isEmpty()) return slices.stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (s.equals("*")) return new ArrayList<>(slices);
        List<SliceSources> chosen = new ArrayList<>();
        for (String token : s.split(",")) {
            int i = Integer.parseInt(token.trim());
            if (i < 0 || i >= slices.size()) {
                throw new IllegalArgumentException("No slice " + i + ": the session has " + slices.size() + " slices");
            }
            chosen.add(slices.get(i));
        }
        return chosen;
    }
}
