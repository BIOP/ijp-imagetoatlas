package ch.epfl.biop.quicknii;

import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import net.imglib2.realtransform.AffineTransform3D;

import java.text.DecimalFormat;
import java.util.List;

public class QuickNIISeries {
    public String name;
    public String target;
    public String aligner;
    public List<SliceInfo> slices;
    public static class SliceInfo {
        public String filename;
        public double[] anchoring;
        public double height;
        public double width;
        public int nr;
        // "markers": [] <- ignored for now
    }


    /**
     * Deprecated, use {@link QuickNIISeries#getTransform(String, SliceInfo, double, double)} instead
     *
     * @param slice the registered slice in question
     * @param imgWidth image width in pixel (given because DeepSlice returns a wrong size in the dataset)
     * @param imgHeight image height in pixel
     * @return the transform to apply on the slice to match DeepSlice results
     */
    @Deprecated
    public static AffineTransform3D getTransformInCCFv3(SliceInfo slice, double imgWidth, double imgHeight) {
        return getTransform("Adult Mouse Brain - Allen Brain Atlas V3p1", slice, imgWidth, imgHeight);
    }

    /**
     * @param atlasName, brainglobe atlas name or Java ABBA atlas name
     * @param slice the registered slice in question
     * @param imgWidth image width in pixel (given because DeepSlice returns a wrong size in the dataset)
     * @param imgHeight image height in pixel
     * @return the transform to apply on the slice to match DeepSlice results
     */
    public static AffineTransform3D getTransform(String atlasName, SliceInfo slice, double imgWidth, double imgHeight) {

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

        AffineTransform3D toCCF = getToCCF(atlasName);

        return transform.preConcatenate(toCCF);
    }

    public static AffineTransform3D getToCCF(String atlasName) {
        AffineTransform3D toCCF = new AffineTransform3D();

        if (DeepSliceHelper.isDeepSliceMouseCompatible(atlasName)) {
            toCCF.set(0.0, -0.025, 0.0, 13.2,
                    0.0, 0.0, -0.025, 8.0,
                    0.025, 0.0, 0.0, 0.0);
            if (!atlasName.startsWith("Adult Mouse Brain - Allen Brain Atlas V3")) {
                AffineTransform3D toBrainGlobe = new AffineTransform3D();
                toBrainGlobe.set(0.0, 0, 1.0, 0,
                        0.0, 1.0, 0.0, 0,
                        -1.0, 0.0, 0.0, 11.4);
                toCCF.preConcatenate(toBrainGlobe.inverse());
            }

        } else if (DeepSliceHelper.isDeepSliceRatCompatible(atlasName)) {
            toCCF.set(0.0390625, 0.0, 0.0, -9.53125,
                    0.0, 0.0390625, 0.0, -24.3359375,
                    0.0, 0.0, 0.0390625, -9.6875);
            if (!atlasName.startsWith("Rat - Waxholm Sprague Dawley V4")) {
                AffineTransform3D toBrainGlobe = new AffineTransform3D();
                toBrainGlobe.set(-1.0,  0.0,  0.0, 10.45,
                        0.0,  0.0, -1.0, 10.26,
                        0.0, -1.0,  0.0, 15.565);
                toCCF.preConcatenate(toBrainGlobe);
            }

        } else {
            System.err.println("Unknown or unsupported atlas named "+atlasName);
        }
        return toCCF;
    }

    public static class Anchor {
        double ox, oy, oz;
        double ux, uy, uz;
        double vx, vy, vz;

        static DecimalFormat df = new DecimalFormat("###.##");

        public Anchor(double[] values) {

            ox = values[0];
            oy = values[1];
            oz = values[2];

            ux = values[3];
            uy = values[4];
            uz = values[5];

            vx = values[6];
            vy = values[7];
            vz = values[8];

        }

        public String toString() {
            return "o["+df.format(ox)+","+df.format(oy)+","+df.format(oz)+"]"+
                    " u["+df.format(ux)+","+df.format(uy)+","+df.format(uz)+"]"+
                    " v["+df.format(vx)+","+df.format(vy)+","+df.format(vz)+"]";
        }

    }

}
