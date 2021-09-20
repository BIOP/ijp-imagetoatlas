package ch.epfl.biop.atlas.plugin;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;

import java.util.Map;

public class SimpleRotationDemo implements SimplePyABBARegistrationPlugin {

    @Override
    public double getVoxelSizeInMicron() {
        return 10.0;
    }

    @Override
    public void setRegistrationParameters(Map<String, String> parameters) {
        parameters.forEach((k,v) -> {
            System.out.println(k+":\t"+v);
        });
    }

    @Override
    public InvertibleRealTransform register(ImagePlus fixed, ImagePlus moving, ImagePlus fixedMask, ImagePlus movingMask) {
        fixed.show();
        moving.show();
        AffineTransform3D at3d = new AffineTransform3D();
        at3d.translate(50,50,0);
        at3d.rotate(2,1);
        return at3d;
    }
}
