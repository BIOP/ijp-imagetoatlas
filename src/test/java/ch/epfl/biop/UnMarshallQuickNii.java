package ch.epfl.biop;

import ch.epfl.biop.quicknii.QuickNIISeries;

import javax.xml.bind.JAXBContext;
import java.io.FileReader;

public class UnMarshallQuickNii {

    public static void main(String... args) throws Exception {
        JAXBContext context = JAXBContext.newInstance(QuickNIISeries.class);
        QuickNIISeries series = (QuickNIISeries) context.createUnmarshaller()
                .unmarshal(new FileReader("src/test/resources/quickniiresults.xml"));

        System.out.println(series);
    }
}
