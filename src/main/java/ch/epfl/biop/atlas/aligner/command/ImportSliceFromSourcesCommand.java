package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>ABBA - Import Sources",
        description = "Imports BigDataViewer-Playground sources as a single slice: each source becomes one channel of the slice.",
        iconPath = "/graphics/BDVToABBA.png")
public class ImportSliceFromSourcesCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slice position (mm)", style="format:0.000", stepSize = "0.1",
            description = "Initial position of the slice along the atlas slicing axis, in mm, as the Z shown in the slice information (0 = front of the atlas).")
    double slice_position_mm;

    @Parameter(style="sorted", label = "Sources (channels)",
            description = "Sources forming the slice, one per channel, in channel order. Their calibration should be in mm.")
    SourceAndConverter<?>[] sources;

    @Override
    public void run() {
        mp.createSlice(sources, mp.fromAtlasZ(slice_position_mm));
    }

}
