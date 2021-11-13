package ch.epfl.biop.quicknii;

import net.imglib2.realtransform.AffineTransform3D;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import java.text.DecimalFormat;

@XmlAccessorType(XmlAccessType.FIELD) // Not sure what this does
@XmlType(propOrder = { "anchoring", "filename", "height", "nr", "width" })
public class QuickNIISlice {

    @XmlAttribute
    public String anchoring;
    // What is this format ?
    // "ox=582.9249635827216&amp;oy=127.81581407135504&amp;oz=373.3840284077289&amp;ux=-689.4945488868467&amp;uy=-4.688853378107666&amp;uz=43.05584187815771&amp;vx=-42.09934420738614&amp;vy=-1.2052866286174435&amp;vz=-475.54962975655036"

    @XmlAttribute
    public String filename;

    @XmlAttribute
    public double height;

    @XmlAttribute
    public int nr; // What is this ?

    @XmlAttribute
    public double width;

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


    /**
     *
     * @param slice
     * @param imgWidth given because deepslice returns a wrong size in the dataset
     * @param imgHeight
     * @return
     */
    public static AffineTransform3D getTransformInCCFv3(QuickNIISlice slice, double imgWidth, double imgHeight) {

        // https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0216796

        AffineTransform3D transform = new AffineTransform3D();
        Anchor anchor = new Anchor(slice.anchoring);
        // Divide by 100 -> allen 10 um per pixel to physical coordinates in mm

        double[] u = {anchor.ux/imgWidth, anchor.uy/imgWidth, anchor.uz/imgWidth};

        double[] v = {anchor.vx/imgHeight,anchor.vy/imgHeight,anchor.vz/imgHeight};

        double[] w = {u[1]*v[2]-u[2]*v[1], u[2]*v[0]-u[0]*v[2], u[0]*v[1]-u[1]*v[0]};

        double norm = Math.sqrt(w[0]*w[0]+w[1]*w[1]+w[2]*w[2]);

        w[0]*=1.0/norm;
        w[1]*=1.0/norm;
        w[2]*=1.0/norm;

        transform.set(
                    u[0],v[0], w[0],anchor.ox,
                    u[1],v[1], w[1],anchor.oy,
                    u[2],v[2], w[2],anchor.oz
                );

        AffineTransform3D toCCF = new AffineTransform3D();

        toCCF.set(0.0, -0.025, 0.0, 13.2,
                0.0, 0.0, -0.025, 8.0,
                0.025, 0.0, 0.0, 0.0);

        return transform.preConcatenate(toCCF);
    }

}
