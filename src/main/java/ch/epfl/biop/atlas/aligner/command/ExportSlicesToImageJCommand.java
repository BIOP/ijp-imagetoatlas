package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.action.ExportSliceToImagePlusAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.java.utilities.roi.types.CompositeFloatPoly;
import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import ch.epfl.biop.source.processor.SourcesChannelsSelect;
import ch.epfl.biop.source.processor.SourcesProcessor;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;
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
        description = "Exports the registered (deformed) selected slices as an ImageJ image, one plane per slice, "+
                      "resampled at the given pixel size within the current region of interest, "+
                      "optionally with the atlas regions as overlay. Same geometry as 'Export Atlas to ImageJ'.")
public class ExportSlicesToImageJCommand extends DynamicCommand implements
        Initializable {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label="Pixel size (micrometers)",
            description = "Pixel size of the exported image.")
    double pixel_size_um = 20;

    @Parameter(label = "Slice channels",
            description = "0-based indices of the slice channels to export, comma separated (e.g. '0,2'), or '*' for all channels.")
    String slice_channels_csv = "*";

    @Parameter(label="Add atlas regions overlay",
            description = "If checked, the atlas regions of each slice are added as an overlay of ROIs.")
    boolean add_regions_overlay = true;

    @Parameter(label="ROI naming",
            description = "Atlas ontology property used to name each region ROI of the overlay, for instance its acronym, name or id. "
                    + "The available choices depend on the atlas. Ignored without overlay.")
    String naming_choice; // Intellij claims it's not used. but it's wrong. It's use through scijava reflection

    @Parameter(label = "Image name",
            description = "Title of the exported ImageJ image.")
    String image_name = "Untitled";

    @Parameter(label = "Interpolate",
            description = "If checked, pixels are linearly interpolated when resampled; otherwise the nearest pixel is used.")
    boolean interpolate;

    @Parameter(type = ItemIO.OUTPUT, label = "Image",
            description = "Exported image: one plane per selected slice.")
    ImagePlus image;


    @Override
    public void run() {
        // TODO : check if tasks are done
        List<SliceSources> slicesToExport = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        if (slicesToExport.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to export");
            return;
        }

        SourcesProcessor preprocess = SourcesProcessorHelper.Identity();

        if (!slice_channels_csv.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(slice_channels_csv.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();

            if (maxIndex>=mp.getChannelBoundForSelectedSlices()) {
                mp.errorMessageForUser.accept("Missing channel in selected slice(s).",
                        "Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
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
                    pixel_size_um / 1000.0, 0,interpolate);

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
                mp.infoMessageForUser.accept("ImagePlus export", "Slice "+slice+" done ("+(i+1)+"/"+images.length+")");
                images[i].setTitle(slice.getName());
                images[i].show();
                addRegionsOverlay(images[i], slice);
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

            String[] labels = image.getStack().getSliceLabels();
            for (int iZ = 0; iZ<image.getNSlices(); iZ++) {
                for (int iCh = 0; iCh<nChannels; iCh++) {
                    labels[iZ * image.getNChannels()+iCh] = images[iZ].getTitle();
                }
            }
            image.draw();
        } else {
            image = images[0];
            image.draw(); // Let's hope this refreshes the overlay
        }

        //image.show();
        image.setTitle(image_name);
    }

    private void addRegionsOverlay(ImagePlus image, SliceSources slice) {
        if (add_regions_overlay) {
            try {
                mp.addTask();
                double atlas_px_in_microns = 1000.0 * mp.getAtlas().getMap().getAtlasPrecisionInMillimeter();
                double scale = atlas_px_in_microns / (this.pixel_size_um);

                image.setOverlay(new Overlay());

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
                    if (atlas_roi!=null) {
                        image.getOverlay().add(atlas_roi);
                    } else {
                        mp.errorMessageForUser.accept("Error in ROI export", "Region "+aRoi.name+" is null!");
                    }
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
        naming_choice.setChoices(names);
    }

}