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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class Elastix2DAffineRegistration implements Registration<SourceAndConverter[]> {

    SourceAndConverter[] fimg, mimg;

    Context ctx;

    Map<String, Object> scijavaParameters = new HashMap<>();

    AffineTransform3D at3d;

    public void setScijavaContext(Context ctx) {
        this.ctx = ctx;
    }

    public void setScijavaParameters(Map<String, Object> scijavaParameters) {
        this.scijavaParameters.putAll(scijavaParameters);
    }

    @Override
    public void setFixedImage(SourceAndConverter[] fimg) {
        this.fimg = fimg;
        assert fimg.length==1;
        scijavaParameters.put("sac_fixed", fimg[0]);
    }

    @Override
    public void setMovingImage(SourceAndConverter[] mimg) {
        this.mimg = mimg;
        assert mimg.length==1;
        scijavaParameters.put("sac_moving", mimg[0]);
    }

    @Override
    public boolean register() {
        try {
             Future<CommandModule> task = ctx
                    .getService(CommandService.class)
                    .run(Elastix2DAffineRegisterCommand.class, true, scijavaParameters );

             at3d = (AffineTransform3D) task.get().getOutput("at3D");
             return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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

        ArrayList<RealPoint> cvtList = new ArrayList<>();
        for (RealPoint p : pts.ptList) {
            RealPoint pt3d = new RealPoint(3);
            pt3d.setPosition(new double[]{p.getDoublePosition(0), p.getDoublePosition(1),0});
            //float npx = p.getFloatPosition(0)/scale+roi.getBounds().x;
            //float npy = p.getFloatPosition(1)/scale+roi.getBounds().y;
            at3d.inverse().apply(pt3d, pt3d);
            RealPoint cpt = new RealPoint(pt3d.getDoublePosition(0), pt3d.getDoublePosition(1));
            cvtList.add(cpt);
        }
        return new RealPointList(cvtList);
    }

    @Override
    public boolean parallelSupported() {
        return true;
    }

    @Override
    public boolean isManual() {
        return false;
    }

    @Override
    public boolean edit() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEditable() {
        return false;
    }
}