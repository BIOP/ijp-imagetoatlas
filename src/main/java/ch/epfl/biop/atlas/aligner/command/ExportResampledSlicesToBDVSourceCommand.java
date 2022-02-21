package ch.epfl.biop.atlas.aligner.command;

import bdv.util.source.fused.AlphaFusedResampledSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.ExportSliceToImagePlusAction;
import ch.epfl.biop.sourceandconverter.EmptyMultiResolutionSourceAndConverterCreator;
import ch.epfl.biop.sourceandconverter.SourceFuserAndResampler;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Concatenator;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Resampled Slices as BDV Source ( experimental )",
        description = "Export registered (deformed) slices in the atlas coordinates. "+
                      "A pixel size should be specified to resample the registered images.")
public class ExportResampledSlicesToBDVSourceCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String slices_string_channels = "*";

    @Parameter(label = "Exported source name")
    String image_name = "Untitled";

    @Parameter
    boolean interpolate;

    @Parameter(label="Match atlas resolution (parameters below ignored)")
    boolean matchAtlasResolution;

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Matching the atlas axis can vastly affect performance! The 'Z' axis is in the atlas coordinates 'X' for the allen brain atlas";

    @Parameter(label="Pixel Size in micron (X)")
    double px_size_micron_x = 20;

    @Parameter(label="Pixel Size in micron (Y)")
    double px_size_micron_y = 20;

    @Parameter(label="Pixel Size in micron (Z)")
    double px_size_micron_z = 20;

    @Parameter(label="X downsampling")
    int downsample_x = 2;

    @Parameter(label="Y downsampling")
    int downsample_y = 2;

    @Parameter(label="Z downsampling")
    int downsample_z = 2;

    @Parameter(label="Block Size X")
    int block_size_x = 1;

    @Parameter(label="Block Size Y")
    int block_size_y = 64;

    @Parameter(label="Block Size Z")
    int block_size_z = 64;

    @Parameter(label="Number of threads")
    int n_threads = 6;

    @Parameter(label="Number of resolution levels (min 1)")
    int resolution_levels = 6;

    @Parameter(type = ItemIO.OUTPUT)
    SourceAndConverter[] fusedImages;

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

        if (!slices_string_channels.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(slices_string_channels.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();

            if (maxIndex>=mp.getChannelBoundForSelectedSlices()) {
                mp.log.accept("Missing channel in selected slice(s).");
                mp.errlog.accept("Missing channel in selected slice(s)\n One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }

            preprocess = new SourcesChannelsSelect(indices);
        }

        SliceSources first = slicesToExport.get(0);
        int nChannels = preprocess.apply(first.getRegisteredSources()).length;

        AffineTransform3D at3D = mp.getAffineTransformFormAlignerToAtlas();
        SourceAffineTransformer sat = new SourceAffineTransformer(null, at3D);

        fusedImages = new SourceAndConverter[nChannels];

        SourceAndConverter model;
        if (matchAtlasResolution) {
            model = mp.getAtlas().getMap().getLabelImage();
        } else {
            AffineTransform3D atlasLocation = new AffineTransform3D();
            Source atlasLabel = mp.getAtlas().getMap().getLabelImage().getSpimSource();
            atlasLabel.getSourceTransform(0, 0, atlasLocation);
            long[] dims = new long[3];
            atlasLabel.getSource(0, 0).dimensions(dims);

            double pixSizeAtlas = mp.getAtlas().getMap().getAtlasPrecisionInMillimeter() * 1000.0;

            long nx = (long) (((double) dims[0]) * pixSizeAtlas / px_size_micron_x);
            long ny = (long) (((double) dims[1]) * pixSizeAtlas / px_size_micron_y);
            long nz = (long) (((double) dims[2]) * pixSizeAtlas / px_size_micron_z);

            double sgnX = Math.signum(atlasLocation.get(0, 0));
            double sgnY = Math.signum(atlasLocation.get(1, 1));
            double sgnZ = Math.signum(atlasLocation.get(2, 2));
            atlasLocation.set(sgnX * px_size_micron_x / 1000.0, 0, 0);
            atlasLocation.set(sgnY * px_size_micron_y / 1000.0, 1, 1);
            atlasLocation.set(sgnZ * px_size_micron_z / 1000.0, 2, 2);

            if (resolution_levels<=0) resolution_levels = 1;

            model = new EmptyMultiResolutionSourceAndConverterCreator("Model",
                    atlasLocation, nx, ny, nz, 1, downsample_x, downsample_y, downsample_z, resolution_levels).get();
        }

        for (int iCh = 0; iCh<nChannels; iCh++) {
            final int iChannel = iCh;
            List<SourceAndConverter> sourcesToFuse = slicesToExport.stream()
                    .map(SliceSources::getRegisteredSources)
                    .map(preprocess)
                    .map(sources -> sources[iChannel])
                    .map(sat)
                    .collect(Collectors.toList());

            fusedImages[iCh]
                    = new SourceFuserAndResampler(sourcesToFuse,
                    AlphaFusedResampledSource.SUM,
                    model,
                    image_name+"_ch"+iChannel,
                    true,true,interpolate,0,block_size_x,block_size_y,block_size_z,n_threads).get();

        }

    }

}