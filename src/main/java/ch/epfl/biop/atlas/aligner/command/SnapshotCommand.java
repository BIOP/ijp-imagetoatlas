package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.inspect.SliceSnapshot;
import ij.ImagePlus;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Inspect>ABBA - Snapshot",
        description = "Renders slices with the atlas region borders at their current registration, or an earlier one, "
                + "independently of the window: a header describes each slice and rulers give coordinates in mm. Several "
                + "slices are tiled. Region labels are the atlas at the slice position, not an observation: after an import "
                + "that position is a placeholder (the header says 'reg=0'). Hide borders and labels to look at the tissue alone.")
public class SnapshotCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session to render.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slices", required = false, description = SliceIndices.DESCRIPTION)
    String slices_csv = "";

    @Parameter(label = "Pixel size (mm)", description = "Pixel size of the rendered image; smaller is sharper and slower.")
    double pixel_size_mm = 0.025;

    @Parameter(label = "Slice channels",
            description = "0-based indices of the slice channels to render, comma separated (e.g. '0' for the first channel alone), "
                    + "or '*' for all channels. Each channel is rendered with its current display settings.")
    String slice_channels_csv = "*";

    @Parameter(label = "Registration step back",
            description = "Renders the slices as they were this many registrations ago, 0 for their current registration "
                    + "(as 'View Previous' in the window). The header then says 'reg=shown/total', e.g. 'reg=2/4'; a slice "
                    + "with fewer registrations is rendered unregistered. Only the registrations are undone: the slice keeps "
                    + "its current position along the slicing axis.")
    int registration_step_back = 0;

    @Parameter(label = "Atlas channel", description = "Index of an atlas channel blended with the slice, -1 for none "
            + "('atlas_channels' in 'ABBA - Get State' lists them in index order).")
    int atlas_channel = -1;

    @Parameter(label = "Region (mm)", required = false,
            description = "Rendered region 'x,y,width,height' in mm, x and y being its top left corner as read on the rulers "
                    + "(e.g. '-1,0.5,2,2' for a 2 mm square). Empty for the whole region of interest. With a small region, "
                    + "lower the pixel size to see details.")
    String region_mm = "";

    @Parameter(label = "Show region borders")
    boolean show_region_borders = true;

    @Parameter(label = "Show region labels")
    boolean show_region_labels = true;

    @Parameter(label = "Columns", description = "Tiles per row when several slices are rendered, 0 for automatic.")
    int columns = 0;

    @Parameter(type = ItemIO.OUTPUT, label = "Snapshot")
    ImagePlus snapshot;

    @Parameter(type = ItemIO.OUTPUT, label = "Labelled regions",
            description = "With region labels: each labelled region as 'acronym: full name', one per line, from the atlas ontology.")
    String regions;

    @Override
    public void run() {
        mp.waitForTasks();
        List<SliceSources> slices = SliceIndices.parse(mp, slices_csv);
        if (slices.isEmpty()) throw new IllegalArgumentException("No slice to render: select slices, or give their indices ('*' for all)");
        SliceSnapshot.Options options = new SliceSnapshot.Options();
        options.pixelSizeMm = pixel_size_mm;
        if (!slice_channels_csv.trim().equals("*")) {
            options.sliceChannels = Arrays.stream(slice_channels_csv.trim().split("\\s*,\\s*")).mapToInt(Integer::parseInt).toArray();
        }
        if (registration_step_back < 0) throw new IllegalArgumentException("registration_step_back: 0 or more, not " + registration_step_back);
        options.registrationStepBack = registration_step_back;
        options.atlasChannel = atlas_channel;
        options.showRegionBorders = show_region_borders;
        options.showRegionLabels = show_region_labels;
        if (region_mm != null && !region_mm.trim().isEmpty()) {
            String[] parts = region_mm.trim().split("\\s*,\\s*");
            if (parts.length != 4) throw new IllegalArgumentException("region_mm: give 'x,y,width,height' in mm, not '" + region_mm + "'");
            options.regionMm = new double[4];
            for (int i = 0; i < 4; i++) options.regionMm[i] = Double.parseDouble(parts[i]);
            if (options.regionMm[2] <= 0 || options.regionMm[3] <= 0) throw new IllegalArgumentException("region_mm: width and height must be positive");
        }
        snapshot = new ImagePlus("ABBA snapshot", slices.size() == 1
                ? SliceSnapshot.render(mp, slices.get(0), options)
                : SliceSnapshot.overview(mp, slices, options, columns));
        StringBuilder names = new StringBuilder();
        options.labelledRegions.forEach((acronym, name) -> names.append(acronym).append(": ").append(name).append('\n'));
        regions = names.toString().trim();
    }
}
