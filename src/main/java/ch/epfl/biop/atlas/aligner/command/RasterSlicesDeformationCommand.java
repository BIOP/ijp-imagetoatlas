package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RasterDeformationAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Raster and cache deformation field",
        description = "Speeds up the display of the selected slices by computing their deformation field on a grid "+
                      "and interpolating it. Only useful after spline registrations. Experimental.")
public class RasterSlicesDeformationCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label="Grid spacing (micrometers)",
            description = "Spacing of the grid on which the deformation is computed. Larger is faster but less accurate. "
                    + "Must be above the atlas resolution.")
    double grid_spacing_um = 150;

    @Override
    public void run() {

        if (grid_spacing_um<0) {
            mp.errorMessageForUser.accept("Raster deformation error","Please use a positive value for the grid size.");
            return;
        }

        if (grid_spacing_um<mp.getAtlas().getMap().getAtlasPrecisionInMillimeter()*1000.0) {
            mp.errorMessageForUser.accept("Raster deformation error","Raster size below the atlas resolution. Please increase your grid spacing.");
            return;
        }

        // TODO : check if tasks are done
        List<SliceSources> slicesToProcess = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slicesToProcess.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to operate on");
            return;
        }

        new MarkActionSequenceBatchAction(mp).runRequest();
        for (SliceSources slice : slicesToProcess) {
            RasterDeformationAction rasterDeformation = new RasterDeformationAction(mp, slice,grid_spacing_um);
            rasterDeformation.runRequest();
        }
        new MarkActionSequenceBatchAction(mp).runRequest();

    }

}