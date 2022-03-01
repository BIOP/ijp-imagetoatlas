package ch.epfl.biop.quicknii;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Pfou....... XMl stuff, I need to learn xml, let's try JAXB (https://www.baeldung.com/jaxb)
 *
 *
 */
@XmlRootElement(name = "series")
@XmlType(propOrder = { "first", "last", "name", "slices"})
public class QuickNIISeries {

    @XmlAttribute
    public String first;

    @XmlAttribute
    public String last;

    @XmlAttribute
    public String name;

    @XmlElement(name="slice")
    public QuickNIISlice[] slices;

    public String toString() {
        String str = "First:"+first+" Last:"+last+" Name:"+name+"\n";
        for (QuickNIISlice slice : slices) {
            str+=slice.toString()+"\n";
        }
        return str;
    }

}
