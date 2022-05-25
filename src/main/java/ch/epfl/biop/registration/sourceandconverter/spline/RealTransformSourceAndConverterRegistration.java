package ch.epfl.biop.registration.sourceandconverter.spline;

import bdv.util.BoundedRealTransform;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.sourceandconverter.SourceAndConverterRegistration;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.RealTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;
import sc.fiji.persist.ScijavaGsonHelper;

import java.util.ArrayList;

abstract public class RealTransformSourceAndConverterRegistration extends SourceAndConverterRegistration {

    final static Logger logger = LoggerFactory.getLogger(RealTransformSourceAndConverterRegistration.class);

    protected RealTransform rt;

    @Override
    public SourceAndConverter[] getTransformedImageMovingToFixed(SourceAndConverter[] img) {
        SourceAndConverter[] out = new SourceAndConverter[img.length];
        SourceRealTransformer srt = new SourceRealTransformer(rt);
        for (int idx = 0;idx<img.length;idx++) {
            out[idx] = srt.apply(img[idx]);
        }
        return out;
    }

    @Override
    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts) {
        RealTransform innerRT = rt; // To unbox bounded realtransform

        // Unbox bounded transform
        if (rt instanceof BoundedRealTransform) {
            innerRT = ((BoundedRealTransform)rt).getTransform().copy();
        }

        ArrayList<RealPoint> cvtList = new ArrayList<>();
        for (RealPoint p : pts.ptList) {
            RealPoint pt3d = new RealPoint(3);
            pt3d.setPosition(new double[]{p.getDoublePosition(0), p.getDoublePosition(1),0});
            innerRT.apply(pt3d, pt3d);
            RealPoint cpt = new RealPoint(pt3d.getDoublePosition(0), pt3d.getDoublePosition(1));
            cvtList.add(cpt);
        }

        return new RealPointList(cvtList);
    }

    public RealTransform getRealTransform() {
        return rt;
    }

    public void setRealTransform(RealTransform transform) {
        this.rt = transform.copy();
    }

    @Override
    final public String getTransform() {
        //logger.debug("Serializing transform of class "+rt.getClass().getSimpleName());
        String transform = ScijavaGsonHelper.getGson(context).toJson(rt, RealTransform.class);
        //logger.debug("Serialization result = "+transform);
        return transform;
    }

    @Override
    final public void setTransform(String serialized_transform) {
        setRealTransform(ScijavaGsonHelper.getGson(context).fromJson(serialized_transform, RealTransform.class));
        isDone = true;
    }

    public RealTransform getTransformAsRealTransform() {
        return rt.copy();
    }

}
