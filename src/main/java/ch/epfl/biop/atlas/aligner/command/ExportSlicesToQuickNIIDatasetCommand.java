package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.quicknii.QuickNIIExporter;
import ch.epfl.biop.quicknii.QuickNIISeries;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import com.google.gson.GsonBuilder;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView.getSourceValueAt;


@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export registered slices as QuickNII dataset",
        iconPath = "/graphics/ABBAToQNII.png")
public class ExportSlicesToQuickNIIDatasetCommand implements Command {

    @Parameter(style = "message", visibility = ItemVisibility.MESSAGE)
    String message = "<html><b>WARNING:</b> The QuickNII export will apply all registrations and resample the images.<br>" +
            "Only JPG seems to be supported.";

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "QuickNII dataset export folder", style="directory")
    File dataset_folder;

    @Parameter(label="Pixel Size in micron", description = "The resolution at which the registered slices will be resampled")
    double px_size_micron = 40;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String channels = "*";

    @Parameter(label = "Section Name Prefix")
    String image_name = "Section";

    @Parameter(label = "Convert to 8 bit image")
    boolean convert_to_8_bits = true;

    @Parameter(label = "Convert to jpg (single channel recommended)")
    boolean convert_to_jpg = true;

    @Parameter
    boolean interpolate;

    @Override
    public void run() {

        List<SliceSources> slicesToExport = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

        SourcesProcessor preprocess = SourcesProcessorHelper.Identity();

        if (!channels.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(channels.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();

            if (maxIndex>=mp.getChannelBoundForSelectedSlices()) {
                mp.errorMessageForUser.accept("Missing channel in selected slice(s).",
                        "Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }

            preprocess = new SourcesChannelsSelect(indices);
        }

        try {
            mp.addTask();
            List<File> paths = QuickNIIExporter.builder()
                    .roi(mp.getROI())
                    .cvt8bits(convert_to_8_bits)
                    .jpeg(convert_to_jpg)
                    .setProcessor(preprocess)
                    .slices(slicesToExport)
                    .name(image_name)
                    .folder(dataset_folder)
                    .pixelSizeMicron(px_size_micron)
                    .interpolate(interpolate)
                    .create()
                    .export();

            double px_size_mm = px_size_micron/1000.0;

            QuickNIISeries series = new QuickNIISeries();
            series.slices = new ArrayList<>();

            SourceAndConverter<FloatType> xSource = mp.getReslicedAtlas().nonExtendedSlicedSources[mp.getReslicedAtlas().getCoordinateSourceIndex(0)]; // 0 = X
            SourceAndConverter<FloatType> ySource = mp.getReslicedAtlas().nonExtendedSlicedSources[mp.getReslicedAtlas().getCoordinateSourceIndex(1)]; // By convention the left right indicator image is the next to last one
            SourceAndConverter<FloatType> zSource = mp.getReslicedAtlas().nonExtendedSlicedSources[mp.getReslicedAtlas().getCoordinateSourceIndex(2)]; // By convention the left right indicator image is the next to last one

            AffineTransform3D toCCF = QuickNIISeries.getToCCF(mp.getReslicedAtlas().ba.getName());

            for (int idx = 0; idx<slicesToExport.size(); idx++) {

                QuickNIISeries.SliceInfo sliceInfo = new QuickNIISeries.SliceInfo();
                sliceInfo.filename = paths.get(idx).getName(); // Folder unsupported
                sliceInfo.nr = idx;
                sliceInfo.width = (int) (mp.getROI()[2]/px_size_mm-0.5);
                sliceInfo.height = (int) (mp.getROI()[3]/px_size_mm-0.5);

                RealPoint pt = new RealPoint(3);
                double margin = mp.getAtlas().getMap().getAtlasPrecisionInMillimeter()/10000;
                pt.setPosition(mp.getROI()[0]+margin,0);
                pt.setPosition(mp.getROI()[1]+margin,1);
                pt.setPosition(slicesToExport.get(idx).getSlicingAxisPosition(), 2);

                double oxi = getSourceValueAt(xSource, pt).get();
                double oyi = getSourceValueAt(ySource, pt).get();
                double ozi = getSourceValueAt(zSource, pt).get();

                pt.setPosition(mp.getROI()[0]+mp.getROI()[2]-margin,0);
                pt.setPosition(mp.getROI()[1]+margin,1);

                double uxi = getSourceValueAt(xSource, pt).get();
                double uyi = getSourceValueAt(ySource, pt).get();
                double uzi = getSourceValueAt(zSource, pt).get();

                pt.setPosition(mp.getROI()[0]+margin,0);
                pt.setPosition(mp.getROI()[1]+mp.getROI()[3]-margin,1);
                double vxi = getSourceValueAt(xSource, pt).get();
                double vyi = getSourceValueAt(ySource, pt).get();
                double vzi = getSourceValueAt(zSource, pt).get();

                RealPoint oi = new RealPoint(oxi, oyi, ozi);
                RealPoint o = new RealPoint(0, 0, 0);
                toCCF.inverse().apply(oi,o);

                RealPoint ui = new RealPoint(uxi, uyi, uzi);
                RealPoint u = new RealPoint(0, 0, 0);
                toCCF.inverse().apply(ui,u);

                RealPoint vi = new RealPoint(vxi, vyi, vzi);
                RealPoint v = new RealPoint(0, 0, 0);
                toCCF.inverse().apply(vi,v);

                sliceInfo.anchoring = new double[]{
                        o.getDoublePosition(0), o.getDoublePosition(1), o.getDoublePosition(2),
                        u.getDoublePosition(0)-o.getDoublePosition(0), u.getDoublePosition(1) -o.getDoublePosition(1), u.getDoublePosition(2)-o.getDoublePosition(2),
                        v.getDoublePosition(0)-o.getDoublePosition(0), v.getDoublePosition(1) -o.getDoublePosition(1), v.getDoublePosition(2)-o.getDoublePosition(2)};
                series.slices.add(sliceInfo);
            }

            try (FileWriter writer = new FileWriter(dataset_folder.getAbsolutePath()+File.separator+image_name+".json")) {
                writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(series));
            }

            mp.infoMessageForUser.accept("Export registered slices as QuickNII dataset", "Export completed.");
        } catch (Exception e) {
            mp.errorMessageForUser.accept("Export to Quick NII dataset error. ", e.getMessage());
        } finally {
            mp.removeTask();
        }

    }

}
