package ch.epfl.biop.abba;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.img.bioformats.command.CreateBdvDatasetBioFormatsCommand;
import ch.epfl.biop.quicknii.QuickNIISeries;
import com.google.gson.Gson;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import org.apache.commons.io.FileUtils;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.io.File;
import java.util.List;

/** DO NOT WORK, use QuickNiiToABBA instead **/
public class QuickNiiDemoJSON {

    public static void main(String... args) throws Exception {
        // Un marshall xml

        String path = "src/test/resources/quicknii/";
        String data = FileUtils.readFileToString(new File(path + "2023-09-18_results.json"), "UTF-8");


        QuickNIISeries series = (new Gson()).fromJson(data, QuickNIISeries.class);

        System.out.println(series);

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

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

        // Transform sources according to anchoring
        for (int i=0; i<sources.size(); i++) {
            QuickNIISeries.SliceInfo slice = series.slices.get(i);
            SourceAndConverter source = sources.get(i);
            SourceTransformHelper.append(QuickNIISeries.getTransformInCCFv3(slice,
                    (double) source.getSpimSource().getSource(0,0).dimension(0),
                    (double) source.getSpimSource().getSource(0,0).dimension(1)), new SourceAndConverterAndTimeRange(source, 0));
        }

    }
}
