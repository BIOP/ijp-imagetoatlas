package ch.epfl.biop.abba;

import ch.epfl.biop.quicknii.QuickNIISeries;

import javax.xml.bind.JAXBContext;
import java.io.FileReader;

public class DeepSliceOutputTest {
    public static void main(String... args) throws Exception {
        // Un marshall xml

        String path = "src/test/resources/quicknii/";

        JAXBContext context = JAXBContext.newInstance(QuickNIISeries.class);
        QuickNIISeries series = (QuickNIISeries) context.createUnmarshaller()
                .unmarshal(new FileReader(path+"results2022-02-02.xml"));
        System.out.println(series);
    }
}
