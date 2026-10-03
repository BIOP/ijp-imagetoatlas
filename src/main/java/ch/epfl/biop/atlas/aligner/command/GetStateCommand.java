package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Inspect>ABBA - Get State",
        description = "Returns the state of the session as JSON, once the queued work is done. Compact by default: the atlas, "
                + "its channels in index order, the slicing angles, the region of interest, and per slice its index, name, position 'z_mm' (as "
                + "displayed), selection, key slice flag, channel names and registrations. With 'full': the content of a "
                + "state file instead, plus per slice its index, name, channels, selection and position in both conventions "
                + "('z_mm' as displayed, 'slicing_axis_position' as stored), long transforms elided.")
public class GetStateCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session to read.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slices", required = false, description = SliceIndices.DESCRIPTION)
    String slices_csv = "*";

    @Parameter(label = "Full state", description = "Returns the full state (every action, with its transforms) instead of the compact summary.")
    boolean full = false;

    @Parameter(label = "Max string length", description = "With 'full': longer strings, like spline transforms, are elided; 0 keeps them all.")
    int max_string_length = 200;

    @Parameter(type = ItemIO.OUTPUT, label = "State", description = "The state, as JSON.")
    String state;

    @Override
    public void run() {
        mp.waitForTasks();
        List<SliceSources> slices = SliceIndices.parse(mp, slices_csv);
        state = full ? mp.serializeSlices(slices, max_string_length) : summary(mp, slices);
    }

    static String summary(MultiSlicePositioner mp, List<SliceSources> slices) {
        JsonObject json = new JsonObject();
        json.addProperty("atlas", mp.getAtlas().getName());
        json.add("atlas_channels", atlasChannels(mp));
        json.addProperty("slicing_angle_x_deg", Math.toDegrees(mp.getReslicedAtlas().getRotateX()));
        json.addProperty("slicing_angle_y_deg", Math.toDegrees(mp.getReslicedAtlas().getRotateY()));
        json.addProperty("roi_mm", SetRegionOfInterestCommand.format(mp.getROI()));
        json.addProperty("number_of_slices", mp.getSlices().size());
        JsonArray list = new JsonArray();
        for (SliceSources slice : slices) {
            JsonObject s = new JsonObject();
            s.addProperty("index", mp.getSlices().indexOf(slice));
            s.addProperty("name", slice.getName());
            s.addProperty("z_mm", Math.round(mp.toAtlasZ(slice.getSlicingAxisPosition()) * 1e4) / 1e4);
            s.addProperty("selected", slice.isSelected());
            s.addProperty("key_slice", slice.isKeySlice());
            JsonArray channels = new JsonArray();
            Arrays.stream(slice.getOriginalSources()).forEach(sac -> channels.add(sac.getSpimSource().getName()));
            s.add("channels", channels);
            JsonArray registrations = new JsonArray();
            slice.getRegistrationNames().forEach(registrations::add);
            s.add("registrations", registrations);
            list.add(s);
        }
        json.add("slices", list);
        return new Gson().toJson(json);
    }

    /** The atlas channels in index order: the indices of 'atlas_channels_csv' and 'atlas_channel' in the commands */
    static JsonArray atlasChannels(MultiSlicePositioner mp) {
        JsonArray channels = new JsonArray();
        for (SourceAndConverter<?> sac : mp.getReslicedAtlas().nonExtendedSlicedSources) channels.add(sac.getSpimSource().getName());
        return channels;
    }
}
