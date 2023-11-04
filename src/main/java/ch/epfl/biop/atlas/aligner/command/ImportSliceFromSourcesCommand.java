package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>ABBA - Import Sources",
        description = "Import a list of sources as a slice into ABBA")
public class ImportSliceFromSourcesCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Initial axis position (0 = front, mm units)", style="format:0.000", stepSize = "0.1")
    double slice_axis_mm;

    @Parameter(style="sorted")
    SourceAndConverter<?>[] sources;

    @Override
    public void run() {
        mp.createSlice(sources, slice_axis_mm);
    }

}
