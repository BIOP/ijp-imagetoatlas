package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.action.ExportSliceToImagePlusAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.java.utilities.roi.types.CompositeFloatPoly;
import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.plugin.Concatenator;
import ij.plugin.RoiScaler;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Registered Slices to ImageJ",
        description = "Export registered (deformed) slices in the atlas coordinates. "+
                      "A pixel size should be specified to resample the registered images.")
public class ExportSlicesToImageJCommand extends DynamicCommand implements
        Initializable {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label="Pixel Size in micron")
    double px_size_micron = 20;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String channels = "*";

    @Parameter(label="Atlas Roi Naming")
    String naming_choice; // Intellij claims it's not used. but it's wrong. It's use through scijava reflection

    @Parameter(label = "Exported image name")
    String image_name = "Untitled";

    @Parameter
    boolean interpolate;

    @Parameter(type = ItemIO.OUTPUT)
    ImagePlus image;



    @Override
    public void run() {
        // TODO : check if tasks are done
        List<SliceSources> slicesToExport = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slicesToExport.size()==0) {
            mp.log.accept("No slice selected");
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to export");
            return;
        }

        SourcesProcessor preprocess = SourcesProcessorHelper.Identity();

        if (!channels.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(channels.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();

            if (maxIndex>=mp.getChannelBoundForSelectedSlices()) {
                mp.log.accept("Missing channel in selected slice(s).");
                mp.errlog.accept("Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }

            preprocess = new SourcesChannelsSelect(indices);
        }

        double[] roi = mp.getROI();

        Map<SliceSources, ExportSliceToImagePlusAction> tasks = new HashMap<>();

        for (SliceSources slice : slicesToExport) {
            ExportSliceToImagePlusAction export = new ExportSliceToImagePlusAction(mp, slice,
                    preprocess,
                    roi[0], roi[1], roi[2], roi[3],
                    px_size_micron / 1000.0, 0,interpolate);

            tasks.put(slice, export);
            export.runRequest();
        }

        ImagePlus[] images = new ImagePlus[slicesToExport.size()];
        IntStream.range(0,slicesToExport.size()).parallel().forEach(i -> {
            SliceSources slice = slicesToExport.get(i);
            boolean success = slice.waitForEndOfAction(tasks.get(slice));
            if (success) {
                images[i] = tasks.get(slice).getImagePlus();
                tasks.get(slice).clean();
                mp.log.accept("Export to ImagePlus of slice "+slice+" done ("+(i+1)+"/"+images.length+")");
                images[i].setTitle("Slice_"+i+"_"+slice);
                images[i].show();
                addRegionsOverlay(i, images[i], slice);
            } else {
                mp.errorMessageForUser.accept("Export to ImageJ Stack error","Error in export of slice "+slice);
            }
        });

        if (images.length>1) {
            // Concatenate overlays and restore min max display of first slice
            int nChannels = images[0].getNChannels();
            Overlay concatOverlay = new Overlay();
            if (images[0].getOverlay()!=null) {
                for (int i = 0; i<images.length; i++) {
                    Roi[] rois = images[i].getOverlay().toArray();
                    for (Roi aRoi: rois) {
                      aRoi.setPosition(0,i+1,1);
                      concatOverlay.add(aRoi);
                    }
                }
            }

            double[] min = new double[nChannels];
            double[] max = new double[nChannels];
            for (int iCh = 0; iCh<nChannels;iCh++) {
                images[0].setC(iCh+1);
                min[iCh] = images[0].getProcessor().getMin();
                max[iCh] = images[0].getProcessor().getMax();
            }
            image = Concatenator.run(images);
            for (int iCh = 0; iCh<nChannels; iCh++) {
                image.setC(iCh+1);
                image.setDisplayRange( min[iCh],  max[iCh]);
            }
            image.setOverlay(concatOverlay);
            image.setDimensions(image.getNChannels(), image.getNFrames(), image.getNSlices());
            image.draw();
        } else {
            image = images[0];
            image.draw(); // Let's hope this refreshes the overlay
        }

        image.show();
        image.setTitle(image_name);
    }

    private void addRegionsOverlay(int i, ImagePlus image, SliceSources slice) {
        if (!naming_choice.equals("Do not add regions")) {
            try {
                mp.addTask();
                double atlas_px_in_microns = 1000.0 * mp.getAtlas().getMap().getAtlasPrecisionInMillimeter();
                double scale = atlas_px_in_microns / (this.px_size_micron);

                if (image.getOverlay() == null) {
                    image.setOverlay(new Overlay());
                }

                IJShapeRoiArray ijRois = slice.getOriginalAtlasRegions(naming_choice);
                for (CompositeFloatPoly aRoi : ijRois.rois) {
                    int atlasId = Integer.parseInt(aRoi.name);
                    AtlasNode node = mp.getAtlas().getOntology().getNodeFromId(atlasId);
                    Roi atlas_roi = aRoi.getRoi();
                    atlas_roi.setName(node.data().get(naming_choice));
                    int[] color = node.getColor();
                    atlas_roi.setStrokeColor(new Color(color[0], color[1], color[2], color[3]));
                    double dxInUm = -(-mp.sX / 2.0 - mp.getROI()[0]) * 1000.0;
                    double dyInUm = -(-mp.sY / 2.0 - mp.getROI()[1]) * 1000.0;
                    atlas_roi.setLocation(atlas_roi.getXBase() - dxInUm / atlas_px_in_microns, atlas_roi.getYBase() - dyInUm / atlas_px_in_microns);
                    atlas_roi = RoiScaler.scale(atlas_roi, scale, scale, false);
                    image.getOverlay().add(atlas_roi);
                }
            } finally {
                mp.removeTask();
            }
        }
    }

    @Override
    public void initialize() {
        final MutableModuleItem<String> naming_choice = //
                getInfo().getMutableInput("naming_choice", String.class);
        List<String> names = new ArrayList<>(mp.getAtlas().getOntology().getRoot().data().keySet());
        names.add(0, "Do not add regions");
        naming_choice.setChoices(names);
    }

}