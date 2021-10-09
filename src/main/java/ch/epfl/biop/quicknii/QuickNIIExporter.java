package ch.epfl.biop.quicknii;

import ch.epfl.biop.atlas.aligner.SliceToImagePlus;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.sourcepreprocessor.SourcesProcessor;
import ch.epfl.biop.atlas.aligner.sourcepreprocessor.SourcesProcessorHelper;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class QuickNIIExporter {
    final List<SliceSources> slices;
    final File datasetFolder;
    final SourcesProcessor processor;
    final double pxSizeMicron;
    final boolean interpolate;
    final boolean convertTo8Bits;
    final boolean convertToJpeg;
    final double[] roi;
    final Consumer<String> logger;
    final String imageName;
    // Use the builder
    private QuickNIIExporter(
                            List<SliceSources> slices,
                            File datasetFolder,
                            SourcesProcessor processor,
                            double pxSizeMicron,
                            boolean interpolate,
                            boolean convertTo8Bits,
                            boolean convertToJpeg,
                            double[] roi,
                            Consumer<String> logger,
                            String imageName) {
        this.slices = slices;
        this.datasetFolder = datasetFolder;
        this.processor = processor;
        this.pxSizeMicron = pxSizeMicron;
        this.convertTo8Bits = convertTo8Bits;
        this.convertToJpeg = convertToJpeg;
        this.interpolate = interpolate;
        this.roi = roi;
        this.logger = logger;
        this.imageName = imageName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void export() throws Exception {

        // Creates
        if (!datasetFolder.exists()) {
            if (!datasetFolder.mkdir()) {
                throw new IOException("QuickNII dataset export failure, Cannot create folder "+datasetFolder.getAbsolutePath());
            }
        }

        if (slices.size()==0) {
            return;
        }


        DecimalFormat df = new DecimalFormat("000");
        IntStream.range(0,slices.size()).parallel().forEach(i -> {
            SliceSources slice = slices.get(i);
            ImagePlus imp = SliceToImagePlus.export(slice,processor,
                    roi[0], roi[1], roi[2], roi[3],
                    pxSizeMicron/1000.0, 0,interpolate );

            imp.setTitle(imageName+"_s"+df.format(i));

            if (convertTo8Bits) {
                new ImageConverter(imp).convertToGray8();
            }

            if (convertToJpeg) {

                IJ.saveAs(imp,"jpeg",
                        datasetFolder.getAbsolutePath() + File.separator + // Folder
                                imageName + "_s" + df.format(i) + ".jpg" // image name, three digits, and underscore s
                );

            } else {
                IJ.save(imp,
                        datasetFolder.getAbsolutePath() + File.separator + // Folder
                                imageName + "_s" + df.format(i) + ".tif" // image name, three digits, and underscore s
                );
            }

            logger.accept("Export of slice "+slice+" done ("+(i+1)+"/"+slices.size()+")");
        });

        logger.accept("Export as QuickNii Dataset done - Folder : "+datasetFolder.getAbsolutePath());

    }

    public static class Builder {
        File datasetFolder;

        SourcesProcessor processor = SourcesProcessorHelper.Identity();

        double pxSizeMicron = 30;

        boolean interpolate = true;

        boolean convertTo8Bits = true;

        boolean convertToJpeg = true;

        List<SliceSources> slices = new ArrayList<>();

        double[] roi;

        Consumer<String> logger = System.out::println;

        String imageName = "Section";

        public Builder slices(List<SliceSources> slices) {
            this.slices = slices;
            return this;
        }

        public Builder name(String name) {
            this.imageName = name;
            return this;
        }

        public Builder folder(File datasetFolder) {
            this.datasetFolder = datasetFolder;
            return this;
        }

        public Builder roi(double[] roi) {
            this.roi = roi;
            return this;
        }

        public Builder jpeg(boolean flag) {
            this.convertToJpeg = flag;
            return this;
        }

        public Builder cvt8bits(boolean flag) {
            this.convertTo8Bits = flag;
            return this;
        }

        public Builder pixelSizeMicron(double pxSize) {
            this.pxSizeMicron = pxSize;
            return this;
        }

        public Builder interpolate(boolean flag) {
            this.interpolate = flag;
            return this;
        }

        public Builder setProcessor(SourcesProcessor processor) {
            this.processor = processor;
            return this;
        }

        public QuickNIIExporter create() {
            return new QuickNIIExporter( slices,
                    datasetFolder,
                    processor,
                    pxSizeMicron,
                    interpolate,
                    convertTo8Bits,
                    convertToJpeg,
                    roi,
                    logger,
                    imageName);
        }

    }
}
