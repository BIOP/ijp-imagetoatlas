package ch.epfl.biop.atlas.aligner.command;

import bdv.util.source.fused.AlphaFusedResampledSource;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.source.EmptyMultiResolutionSourceCreator;
import ch.epfl.biop.source.SourceFuserAndResampler;
import ch.epfl.biop.source.processor.SourcesChannelsSelect;
import ch.epfl.biop.source.processor.SourcesProcessor;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.source.transform.SourceAffineTransformer;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Resampled Slices as BDV Source ( experimental )",
        description = "Experimental: fuses the registered selected slices into a 3D volume in atlas coordinates, "+
                      "resampled at the given voxel size and within the current region of interest, "+
                      "as multiresolution BigDataViewer sources (one per channel), computed lazily and cached.")
public class ExportResampledSlicesToBDVSourceCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slice channels",
            description = "0-based indices of the slice channels to export, comma separated (e.g. '0,2'), or '*' for all channels.")
    String slice_channels_csv = "*";

    @Parameter(label = "Source name",
            description = "Base name of the exported sources, '_ch<index>' is appended for each channel.")
    String image_name = "Untitled";

    @Parameter(label = "Interpolate",
            description = "If checked, pixels are linearly interpolated when resampled; otherwise the nearest pixel is used.")
    boolean interpolate;

    @Parameter(label="Voxel size X (micrometers)",
            description = "Size of the output voxels along the X axis of the sections.")
    double voxel_size_x_um = 20;

    @Parameter(label="Voxel size Y (micrometers)",
            description = "Size of the output voxels along the Y axis of the sections.")
    double voxel_size_y_um = 20;

    @Parameter(label="Voxel size Z (micrometers)",
            description = "Size of the output voxels along the slicing axis.")
    double voxel_size_z_um = 20;

    @Parameter(label="Z margin (micrometers)",
            description = "Extra space added before the first and after the last selected slice along the slicing axis.")
    double margin_z_um = 0;

    @Parameter(label="Downsampling between resolution levels X",
            description = "Factor along X between two consecutive resolution levels of the output.")
    int downsample_x = 2;

    @Parameter(label="Downsampling between resolution levels Y",
            description = "Factor along Y between two consecutive resolution levels of the output.")
    int downsample_y = 2;

    @Parameter(label="Downsampling between resolution levels Z",
            description = "Factor along Z between two consecutive resolution levels of the output.")
    int downsample_z = 1;

    @Parameter(label="Cache block size X (pixels)",
            description = "Size along X of the blocks computed and cached at once.")
    int block_size_x = 64;

    @Parameter(label="Cache block size Y (pixels)",
            description = "Size along Y of the blocks computed and cached at once.")
    int block_size_y = 64;

    @Parameter(label="Cache block size Z (pixels)",
            description = "Size along Z of the blocks computed and cached at once.")
    int block_size_z = 4;

    @Parameter(label="Number of threads",
            description = "Number of threads used to compute the blocks.")
    int n_threads = 6;

    @Parameter(label="Number of resolution levels",
            description = "Number of resolution levels of the output sources, at least 1.")
    int resolution_levels = 6;

    @Parameter(type = ItemIO.OUTPUT, label = "Fused sources",
            description = "One fused 3D source per exported channel.")
    SourceAndConverter<?>[] fusedImages;

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

        if (!slice_channels_csv.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(slice_channels_csv.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

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
        double minZ = frontSlice.getSlicingAxisPosition()-frontSlice.getThicknessInMm()/2.0-margin_z_um*0.001;
        SliceSources backSlice = slicesToExport.get(slicesToExport.size()-1);
        double maxZ = backSlice.getSlicingAxisPosition()+backSlice.getThicknessInMm()/2.0+margin_z_um*0.001;
        double sizeZ = maxZ-minZ;

        AffineTransform3D coord = new AffineTransform3D();
        coord.scale(voxel_size_x_um/1000.0, voxel_size_y_um/1000.0, voxel_size_z_um/1000.0);
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

        model = new EmptyMultiResolutionSourceCreator("Model",
                coord, (long)(sizeX/(voxel_size_x_um/1000.0)),
                (long)(sizeY/(voxel_size_y_um/1000.0)),
                (long)(sizeZ/(voxel_size_z_um/1000.0)), 1, downsample_x, downsample_y, downsample_z, resolution_levels).get();

        //SourceServices.getSourceService().register(model);

        for (int iCh = 0; iCh<nChannels; iCh++) {
            final int iChannel = iCh;

            List<SourceAndConverter<?>> sourcesToFuse = slicesToExport.stream()
                    .map(SliceSources::getRegisteredSources)
                    .map(preprocess)
                    .map(sources -> sources[iChannel])
                    .map(source -> new SourceAffineTransformer(at3D).apply(source))//sat)
                    /*.map(src -> {
                        SourceServices.getSourceService()
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