package ch.epfl.biop.abba;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.img.legacy.bioformats.command.BasicOpenFilesWithBigdataviewerBioformatsBridgeCommand;
import ch.epfl.biop.quicknii.QuickNIISeries;
import ch.epfl.biop.quicknii.QuickNIISlice;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import javax.xml.bind.JAXBContext;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;

public class QuickNiiDemo {

    public static void main(String... args) throws Exception {
        // Un marshall xml

        String path = "src/test/resources/quicknii/";

        JAXBContext context = JAXBContext.newInstance(QuickNIISeries.class);
        QuickNIISeries series = (QuickNIISeries) context.createUnmarshaller()
                .unmarshal(new FileReader(path+"quickniiresults.xml"));

        System.out.println(series);

        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

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
        List<SourceAndConverter<?>> sources = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(asd);

        // Transform sources according to anchoring
        for (int i=0; i<sources.size(); i++) {
            QuickNIISlice slice = series.slices[i];
            SourceAndConverter source = sources.get(i);
            SourceTransformHelper.append(QuickNIISlice.getTransformInCCFv3(slice,
                    (double) source.getSpimSource().getSource(0,0).dimension(0),
                    (double) source.getSpimSource().getSource(0,0).dimension(1)), new SourceAndConverterAndTimeRange(source, 0));
        }

    }
}
