package ch.epfl.biop.abba;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.bdv.img.bioformats.command.CreateBdvDatasetBioFormatsCommand;
import ch.epfl.biop.quicknii.QuickNIISeries;
import com.google.gson.Gson;
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

// Pfou - do not work
public class QuickNiiToABBA {

    public static void main(String... args) throws Exception {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        DebugTools.setRootLevel("off");
        Atlas mouseAtlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");

        MultiSlicePositioner mp = (MultiSlicePositioner) (ij.command()
                .run(ABBAStartCommand.class, true,
                        "ba", mouseAtlas,
                        "slicing_mode", "coronal").get().getOutput("mp"));

        String path = "src/test/resources/quicknii/";

        /*
        String data = FileUtils.readFileToString(new File(path + "2023-09-18_results.json"), "UTF-8");
        QuickNIISeries series = (new Gson()).fromJson(data, QuickNIISeries.class);
         */

        QuickNIISeries series = new Gson().fromJson(new FileReader(path+"2023-09-18_results.json"), QuickNIISeries.class);

        File[] files = series.slices.stream()
                .map(slice -> new File(path, slice.filename))
                .toArray(File[]::new);

        // Creates a spimdata object
        AbstractSpimData asd = (AbstractSpimData) ij.command().run(CreateBdvDatasetBioFormatsCommand.class,true,
                "unit", "MILLIMETER",
                "split_rgb_channels", false,
                "files", files
        ).get().getOutput("spimdata");

        // Retrieve sources from the spimdata
        List<SourceAndConverter<?>> sources = SourceAndConverterServices
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
                QuickNIISeries.SliceInfo slice = series.slices.get(i);
                SourceAndConverter source = sources.get(i);

                AffineTransform3D toCCFv3 = QuickNIISeries.getTransformInCCFv3(slice,
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

                //("[" + zx + ", " + zy + ",  " + zz + "]");

                double ry = Math.asin(zx);
                double rx = Math.asin(zy);

                rxs[i] = rx;
                rys[i] = ry;

                //("rx = " + (int) (rx * 180.0 / Math.PI) + " ry = " + (int) (ry * 180.0 / Math.PI));

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
            QuickNIISeries.SliceInfo slice = series.slices.get(i);
            SourceAndConverter source = sources.get(i);

            AffineTransform3D toCCFv3 = QuickNIISeries.getTransformInCCFv3(slice,
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
