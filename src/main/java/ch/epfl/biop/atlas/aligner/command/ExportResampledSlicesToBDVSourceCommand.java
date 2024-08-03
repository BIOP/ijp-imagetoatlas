package ch.epfl.biop.atlas.aligner.command;

import bdv.util.source.fused.AlphaFusedResampledSource;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.sourceandconverter.EmptyMultiResolutionSourceAndConverterCreator;
import ch.epfl.biop.sourceandconverter.SourceFuserAndResampler;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Resampled Slices as BDV Source ( experimental )",
        description = "Export registered (deformed) slices in the atlas coordinates. "+
                      "A pixel size should be specified to resample the registered images.")
public class ExportResampledSlicesToBDVSourceCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String channels = "*";

    @Parameter(label = "Exported source name")
    String image_name = "Untitled";

    @Parameter
    boolean interpolate;

    @Parameter(label="Pixel Size in micron (X)")
    double px_size_micron_x = 20;

    @Parameter(label="Pixel Size in micron (Y)")
    double px_size_micron_y = 20;

    @Parameter(label="Pixel Size in micron (Z)")
    double px_size_micron_z = 20;

    @Parameter(label="Margin in Z in micron")
    double margin_z = 0;

    @Parameter(label="X downsampling")
    int downsample_x = 2;

    @Parameter(label="Y downsampling")
    int downsample_y = 2;

    @Parameter(label="Z downsampling")
    int downsample_z = 1;

    @Parameter(label="Block Size X")
    int block_size_x = 64;

    @Parameter(label="Block Size Y")
    int block_size_y = 64;

    @Parameter(label="Block Size Z")
    int block_size_z = 4;

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

        if (slicesToExport.isEmpty()) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to export");
            return;
        }

        slicesToExport.forEach(SliceSources::setAlphaSources); // Should make things faster
        
        SourcesProcessor preprocess = SourcesProcessorHelper.Identity();

        if (!channels.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(channels.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

            int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();

            if (maxIndex>=mp.getChannelBoundForSelectedSlices()) {
                mp.errorMessageForUser.accept("Missing channel in selected slice(s)",
                        "One selected slice only has "+mp.getChannelBoundForSelectedSlices()+" channel(s).\n Maximum index : "+(mp.getChannelBoundForSelectedSlices()-1) );
                return;
            }

            preprocess = new SourcesChannelsSelect(indices);
        }

        SliceSources first = slicesToExport.get(0);
        int nChannels = preprocess.apply(first.getRegisteredSources()).length;

        AffineTransform3D at3D = mp.getAffineTransformFromAlignerToAtlas();
        SourceAffineTransformer sat = new SourceAffineTransformer(null, at3D);

        fusedImages = new SourceAndConverter[nChannels];

        SourceAndConverter model;

        double[] roi = mp.getROI();
        double sizeX = roi[2];
        double sizeY = roi[3];

        SliceSources frontSlice = slicesToExport.get(0);
        double minZ = frontSlice.getSlicingAxisPosition()-frontSlice.getThicknessInMm()/2.0-margin_z*0.001;
        SliceSources backSlice = slicesToExport.get(slicesToExport.size()-1);
        double maxZ = backSlice.getSlicingAxisPosition()+backSlice.getThicknessInMm()/2.0+margin_z*0.001;
        double sizeZ = maxZ-minZ;

        AffineTransform3D coord = new AffineTransform3D();
        coord.scale(px_size_micron_x/1000.0, px_size_micron_y/1000.0, px_size_micron_z/1000.0);
        coord.translate(roi[0], roi[1], minZ);

        coord.preConcatenate(mp.getAffineTransformFromAlignerToAtlas());

        // Now makes the matrix orthonormal
        double[] m = coord.getRowPackedCopy();
        double[] voxelSizes = new double[3];

        for(int d = 0; d < 3; ++d) {
            voxelSizes[d] = Math.sqrt(m[d] * m[d] + m[d + 4] * m[d + 4] + m[d + 8] * m[d + 8]);
        }

        for(int d = 0; d < 3; ++d) {
            double c0 = m[d];
            double c1 = m[d+4];
            double c2 = m[d+8];

            if (Math.abs(c0)>Math.abs(c1)) {
                // c0 > c1
                if (Math.abs(c0)>Math.abs(c2)) {
                    // c0 > c2
                    // c0 max
                    m[d] = voxelSizes[d]*Math.signum(m[d]);
                    m[d+4] = 0;
                    m[d+8] = 0;
                } else {
                    // c2 > c0 > c1
                    // c2 max
                    m[d] = 0;
                    m[d+4] = 0;
                    m[d+8] = voxelSizes[d]*Math.signum(m[d+8]);
                }
            } else {
                // c1 > c0
                if (Math.abs(c1)>Math.abs(c2)) {
                    // c1 > c2
                    // c1 max
                    m[d] = 0;
                    m[d+4] = voxelSizes[d]*Math.signum(m[d+4]);
                    m[d+8] = 0;
                } else {
                    // c2 > c1 > c0
                    // c2 max
                    m[d] = 0;
                    m[d+4] = 0;
                    m[d+8] = voxelSizes[d]*Math.signum(m[d+8]);
                }
            }
        }

        coord.set(m);

        model = new EmptyMultiResolutionSourceAndConverterCreator("Model",
                coord, (long)(sizeX/(px_size_micron_x/1000.0)),
                (long)(sizeY/(px_size_micron_y/1000.0)),
                (long)(sizeZ/(px_size_micron_z/1000.0)), 1, downsample_x, downsample_y, downsample_z, resolution_levels).get();

        //SourceAndConverterServices.getSourceAndConverterService().register(model);

        for (int iCh = 0; iCh<nChannels; iCh++) {
            final int iChannel = iCh;

            List<SourceAndConverter<?>> sourcesToFuse = slicesToExport.stream()
                    .map(SliceSources::getRegisteredSources)
                    .map(preprocess)
                    .map(sources -> sources[iChannel])
                    .map(source -> new SourceAffineTransformer(at3D).apply(source))//sat)
                    /*.map(src -> {
                        SourceAndConverterServices.getSourceAndConverterService()
                                .register(AlphaSourceHelper.getOrBuildAlphaSource(src));
                        return src;
                    })*/
                    .map(source -> (SourceAndConverter<?>) source)
                    .collect(Collectors.toList());

            fusedImages[iCh]
                    = new SourceFuserAndResampler(sourcesToFuse,
                    AlphaFusedResampledSource.SUM,
                    model,
                    image_name+"_ch"+iChannel,
                    true,true,interpolate,0,block_size_x,block_size_y,block_size_z,-1,n_threads).get();
        }

    }

}