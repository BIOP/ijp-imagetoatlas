package ch.epfl.biop.atlas.aligner;

import net.imglib2.realtransform.AffineTransform3D;

/**
 * In-plane affine transform of slices, in aligner coordinates: mm, x to the right, y down on screen.
 * Shared by the rotation buttons, {@link ch.epfl.biop.atlas.aligner.command.RotateSlicesCommand} and the
 * in-plane transform editor. Only the matrix returned by {@link #toAffine()} is stored in the registration,
 * so this parametrization can change without breaking states.
 */
public class InPlaneTransform {

    /** Rotation angle in radians, positive is clockwise on screen */
    public final double angle;
    public final double scaleX, scaleY;
    /** Shear factor: x += shear * y */
    public final double shear;
    /** Translation in mm */
    public final double translationX, translationY;
    /** Center of the rotation, scaling and shear, in mm */
    public final double pivotX, pivotY;

    public InPlaneTransform(double angle, double scaleX, double scaleY, double shear,
                            double translationX, double translationY, double pivotX, double pivotY) {
        this.angle = angle;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.shear = shear;
        this.translationX = translationX;
        this.translationY = translationY;
        this.pivotX = pivotX;
        this.pivotY = pivotY;
    }

    public static InPlaneTransform rotation(double angle, double pivotX, double pivotY) {
        return new InPlaneTransform(angle, 1, 1, 0, 0, 0, pivotX, pivotY);
    }

    /**
     * @return M = T(c + t) · R(angle) · Sh(shear) · S(scaleX, scaleY) · T(-c), with c the pivot and t the translation.
     * The z row and column are left to identity.
     */
    public AffineTransform3D toAffine() {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        // R · Sh · S
        double m00 = cos * scaleX, m01 = (cos * shear - sin) * scaleY;
        double m10 = sin * scaleX, m11 = (sin * shear + cos) * scaleY;
        AffineTransform3D m = new AffineTransform3D();
        m.set(m00, m01, 0, pivotX + translationX - (m00 * pivotX + m01 * pivotY),
              m10, m11, 0, pivotY + translationY - (m10 * pivotX + m11 * pivotY),
              0, 0, 1, 0);
        return m;
    }

}
