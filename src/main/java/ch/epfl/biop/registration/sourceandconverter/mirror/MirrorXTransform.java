package ch.epfl.biop.registration.sourceandconverter.mirror;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPositionable;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;

public class MirrorXTransform implements InvertibleRealTransform {

    final double xFactor;

    public MirrorXTransform(double xFactor) {
        this.xFactor = xFactor;
    }

    @Override
    public int numSourceDimensions() {
        return 3;
    }

    @Override
    public int numTargetDimensions() {
        return 3;
    }

    @Override
    public void apply(double[] source, double[] target) {
        target[0] = source[0]*xFactor>0 ? source[0]:-source[0];
        target[1] = source[1];
        target[2] = source[2];
    }

    @Override
    public void apply(RealLocalizable realLocalizable, RealPositionable realPositionable) {
        double xPos = realLocalizable.getDoublePosition(0);
        realPositionable.setPosition(xPos*xFactor>0 ? xPos:-xPos,0);
        realPositionable.setPosition(realLocalizable.getDoublePosition(1),1);
        realPositionable.setPosition(realLocalizable.getDoublePosition(2),2);
    }

    @Override
    public void applyInverse(double[] source, double[] target) {
        target[0] = source[0];
        target[1] = source[1];
        target[2] = source[2];
    }

    @Override
    public void applyInverse(RealPositionable realPositionable, RealLocalizable realLocalizable) {
        realPositionable.setPosition(realLocalizable.getDoublePosition(0),0);
        realPositionable.setPosition(realLocalizable.getDoublePosition(1),1);
        realPositionable.setPosition(realLocalizable.getDoublePosition(2),2);
    }

    @Override
    public InvertibleRealTransform inverse() {
        return new AffineTransform3D();
    }

    @Override
    public InvertibleRealTransform copy() {
        return new MirrorXTransform(xFactor);
    }
}
