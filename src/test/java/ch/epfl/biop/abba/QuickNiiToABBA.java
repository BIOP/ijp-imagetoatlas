package ch.epfl.biop.abba;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.command.AllenBrainAdultMouseAtlasCCF2017Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.bdv.bioformats.command.BasicOpenFilesWithBigdataviewerBioformatsBridgeCommand;
import ch.epfl.biop.quicknii.QuickNIISeries;
import ch.epfl.biop.quicknii.QuickNIISlice;
import loci.common.DebugTools;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import javax.xml.bind.JAXBContext;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

public class QuickNiiToABBA {

    public static void main(String... args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        DebugTools.setRootLevel("off");
        Atlas mouseAtlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017Command.class, true).get().getOutput("ba");

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command()
                .run(ABBAStartCommand.class, true,
                        "ba", mouseAtlas,
                        "slicing_mode", "coronal").get().getOutput("mp"));

        String path = "src/test/resources/quicknii/";

        JAXBContext context = JAXBContext.newInstance(QuickNIISeries.class);
        QuickNIISeries series = (QuickNIISeries) context.createUnmarshaller()
                .unmarshal(new FileReader(path+"results2022-03-01.xml"));

        File[] files = Arrays.stream(series.slices)
                .map(slice -> new File(path, slice.filename))
                .toArray(File[]::new);

        // Creates a spimdata object
        AbstractSpimData asd = (AbstractSpimData) ij.command().run(BasicOpenFilesWithBigdataviewerBioformatsBridgeCommand.class,true,
                "unit", "MILLIMETER",
                "splitrgbchannels", false,
                "files", files
        ).get().getOutput("spimdata");

        // Retrieve sources from the spimdata
        List<SourceAndConverter> sources = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(asd);

        mp.getReslicedAtlas().setRotateX(0.4);
        mp.getReslicedAtlas().setRotateY(0.4);


        for (int nAdjust = 0;nAdjust<10;nAdjust++) { // Iterative rotation adjustement, because that's convenient
            AffineTransform3D toABBA = mp.getReslicedAtlas().getSlicingTransformToAtlas().inverse();

            // Transform sources according to anchoring
            double[] rxs = new double[sources.size()];
            double[] rys = new double[sources.size()];

            for (int i = 0; i < sources.size(); i++) {
                QuickNIISlice slice = series.slices[i];
                SourceAndConverter source = sources.get(i);

                AffineTransform3D toCCFv3 = QuickNIISlice.getTransformInCCFv3(slice,
                        (double) source.getSpimSource().getSource(0, 0).dimension(0),
                        (double) source.getSpimSource().getSource(0, 0).dimension(1));

                AffineTransform3D nonFlat = toCCFv3.preConcatenate(toABBA);

                // Get the z vector to measure the angle of rotation compared to the actual one

                double zx = nonFlat.get(2, 0);
                double zy = nonFlat.get(2, 1);
                double zz = nonFlat.get(2, 2);

                double zNorm = Math.sqrt(zx * zx + zy * zy + zz * zz);

                zx /= zNorm;
                zy /= zNorm;
                zz /= zNorm;

                //System.out.println("[" + zx + ", " + zy + ",  " + zz + "]");

                double ry = Math.asin(zx);
                double rx = Math.asin(zy);

                rxs[i] = rx;
                rys[i] = ry;

                //System.out.println("rx = " + (int) (rx * 180.0 / Math.PI) + " ry = " + (int) (ry * 180.0 / Math.PI));

            }

            System.out.println("Round "+nAdjust);
            System.out.println("rotation x =" + getMedian(rxs));
            System.out.println("rotation y =" + getMedian(rys));
            mp.getReslicedAtlas().setRotateY(mp.getReslicedAtlas().getRotateY() - getMedian(rys) / 2.0);
            mp.getReslicedAtlas().setRotateX(mp.getReslicedAtlas().getRotateX() + getMedian(rxs) / 2.0);
        }

        // Now put slices at their correct location in z.
        // It's probably ok to use nonFlat z shift
        // probably it can be improved if the user refuses the angle adjustement

        AffineTransform3D toABBA = mp.getReslicedAtlas().getSlicingTransformToAtlas().inverse();

        for (int i = 0; i < sources.size(); i++) {
            QuickNIISlice slice = series.slices[i];
            SourceAndConverter source = sources.get(i);

            AffineTransform3D toCCFv3 = QuickNIISlice.getTransformInCCFv3(slice,
                    (double) source.getSpimSource().getSource(0, 0).dimension(0),
                    (double) source.getSpimSource().getSource(0, 0).dimension(1));

            SourceTransformHelper.append(toCCFv3,
                                new SourceAndConverterAndTimeRange(source, 0));

            SourceTransformHelper.append(toABBA,
                    new SourceAndConverterAndTimeRange(source, 0));

            AffineTransform3D nonFlat = toCCFv3.preConcatenate(toABBA);

            AffineTransform3D flat = new AffineTransform3D();
            flat.set(nonFlat);

            double zLocation = nonFlat.get(2,3);

            /*System.out.println("slice ["+i+"], zpos = "+nonFlat.get(2,3));

            flat.set(0,2,0);
            flat.set(0,2,1);
            flat.set(1,2,2);
            flat.set(0,2,3);

            //AffineTransform3D toMm = new AffineTransform3D();
            //toMm.scale(0.04); // 40 microns per pixel exported*/

            //SourceTransformHelper.append(nonFlat,
            //            new SourceAndConverterAndTimeRange(source, 0));

            mp.createSlice(new SourceAndConverter[]{source}, zLocation ); // This doesn't work because the slice is recentered on creation

        }

        BdvHandle bdvh = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        BdvMultislicePositionerView view = new BdvMultislicePositionerView(mp, bdvh);

        /*nonFlat.set(0,2,0);
        nonFlat.set(0,2,1);
        nonFlat.set(1,2,2);*/

        /*SourceTransformHelper.append(nonFlat,
                new SourceAndConverterAndTimeRange(source, 0));**/

    }

    public static double getMedian(double[] array) {
        Arrays.sort(array);
        double median;
        if (array.length % 2 == 0)
            median = ((double)array[array.length/2] + (double)array[array.length/2 - 1])/2;
        else
            median = (double) array[array.length/2];
        return median;
    }

}
