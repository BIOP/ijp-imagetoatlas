package ch.epfl.biop.quicknii;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.text.DecimalFormat;

@XmlAccessorType(XmlAccessType.FIELD) // Not sure what this does
@XmlType(propOrder = { "anchoring", "filename", "height", "nr", "width" })
public class QuickNIISlice {

    @XmlAttribute
    String anchoring;
    // What is this format ?
    // "ox=582.9249635827216&amp;oy=127.81581407135504&amp;oz=373.3840284077289&amp;ux=-689.4945488868467&amp;uy=-4.688853378107666&amp;uz=43.05584187815771&amp;vx=-42.09934420738614&amp;vy=-1.2052866286174435&amp;vz=-475.54962975655036"

    @XmlAttribute
    String filename;

    @XmlAttribute
    int height;

    @XmlAttribute
    int nr; // What is this ?

    @XmlAttribute
    int width;


    public String toString() {
        return "anchor:"+new Anchor(anchoring)+" fName="+filename+"[w:"+width+" h:"+height+"]("+nr+")";
    }

    public static class Anchor {
        double ox, oy, oz;
        double ux, uy, uz;
        double vx, vy, vz;

        static DecimalFormat df = new DecimalFormat("###.##");

        public Anchor(String str) {
            // Parse this weird stuff (dirty)
            str = str.replace("&","=");

            String[] parts = str.split("=");
            ox = Double.parseDouble(parts[1]);
            oy = Double.parseDouble(parts[3]);
            oz = Double.parseDouble(parts[5]);

            ux = Double.parseDouble(parts[7]);
            uy = Double.parseDouble(parts[9]);
            uz = Double.parseDouble(parts[11]);

            vx = Double.parseDouble(parts[13]);
            vy = Double.parseDouble(parts[15]);
            vz = Double.parseDouble(parts[17]);

        }

        public String toString() {
            return "o["+df.format(ox)+","+df.format(oy)+","+df.format(oz)+"]"+
                   " u["+df.format(ux)+","+df.format(uy)+","+df.format(uz)+"]"+
                   " v["+df.format(vx)+","+df.format(vy)+","+df.format(vz)+"]";
        }

    }

}
