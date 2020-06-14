package ch.epfl.biop.registration.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.scijava.command.Elastix2DAffineRegisterCommand;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class Elastix2DAffineRegistration implements Registration<SourceAndConverter[]> {

    SourceAndConverter[] fimg, mimg;

    Context ctx;

    Map<String, Object> scijavaParameters = new HashMap<>();

    public void setScijavaContext(Context ctx) {
        this.ctx = ctx;
    }

    public void setScijavaParameters(Map<String, Object> scijavaParameters) {
        this.scijavaParameters.putAll(scijavaParameters);
    }

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        this.fimg = fimg;
        scijavaParameters.put("sac_fixed", fimg);
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        this.mimg = mimg;
        scijavaParameters.put("sac_moving", mimg);
    }

    AffineTransform3D at3d;

    @Override
    public void register() {

        try {
             Future<CommandModule> task = ctx
                    .getService(CommandService.class)
                    .run(Elastix2DAffineRegisterCommand.class, true, scijavaParameters );

             at3d = (AffineTransform3D) task.get().getOutput("at3D");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed(SourceAndConverter[] img) {
        SourceAndConverter[] out = new SourceAndConverter[img.length];

        for (int idx = 0;idx<img.length;idx++) {
            out[idx] = SourceTransformHelper.append(at3d, new SourceAndConverterAndTimeRange(img[idx],(int) scijavaParameters.get("tpMoving")));
        }

        return out;
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {

        for (RealPoint pt : pts.ptList) {
            at3d.inverse().apply(pt,pt);
        }

        return pts;
    }

    @Override
    public boolean parallelSupported() {
        return true;
    }

    @Override
    public boolean isManual() {
        return false;
    }
}
