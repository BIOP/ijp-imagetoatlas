package ch.epfl.biop.atlas.aligner;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.sourceandconverter.EmptyMultiResolutionSourceAndConverterCreator;
import ch.epfl.biop.registration.sourceandconverter.affine.AffineTransformedSourceWrapperRegistration;
import ch.epfl.biop.sourceandconverter.transform.SourceMosaicZSlicer;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.util.ArrayList;
import java.util.List;

public class ReslicedAtlas {

    final public BiopAtlas ba;

    AffineTransform3D slicingTransfom = new AffineTransform3D();

    double slicingResolution = -1;

    SourceAndConverter slicingModel;

    SourceAndConverter[] extendedSlicedSources;

    SourceAndConverter[] nonExtendedSlicedSources;

    AffineTransform3D centerTransform;

    AffineTransformedSourceWrapperRegistration nonExtendedAffineTransform = new AffineTransformedSourceWrapperRegistration();

    private volatile int zStep = 1;
    private double rx;
    private double ry;

    List<Runnable> listeners = new ArrayList<>();

    public ReslicedAtlas(BiopAtlas ba) {
        this.ba = ba;
    }

    public void addListener(Runnable runnable) {
        listeners.add(runnable);
    }

    public void setSlicingTransform(AffineTransform3D slicingTransfom) {
        this.slicingTransfom = slicingTransfom;
        computeReslicedSources();
    }

    public void setResolution(double slicingResolution) {
        this.slicingResolution = slicingResolution;
        computeReslicedSources();
    }

    void computeReslicedSources() {
        if ((slicingTransfom==null)||(slicingResolution<=0)) return;

        // ----------------------------- COMPUTES BOUNDS

        // No let's check for bounds along the z axis
        // Pick the first SourceAndConverter
        SourceAndConverter sacForBoundsTesting = ba.map.getStructuralImages()[0];

        // Gets level 0 (and timepoint 0) and source transform
        AffineTransform3D sacTransform = new AffineTransform3D();
        sacForBoundsTesting.getSpimSource().getSourceTransform(0,0, sacTransform);

        RandomAccessibleInterval rai = sacForBoundsTesting.getSpimSource().getSource(0,0);

        double minZAxis = Double.MAX_VALUE;
        double maxZAxis = -Double.MAX_VALUE;

        double minXAxis = Double.MAX_VALUE;
        double maxXAxis = -Double.MAX_VALUE;

        double minYAxis = Double.MAX_VALUE;
        double maxYAxis = -Double.MAX_VALUE;

        // Project all corners on slicing coordinate system and find min / max
        for (int x=0;x<2;x++)
            for (int y=0;y<2;y++)
                for (int z=0;z<2;z++){
                    RealPoint pt = new RealPoint(3);
                    pt.setPosition(new long[] {
                            x*rai.dimension(0),
                            y*rai.dimension(1),
                            z*rai.dimension(2),
                    });
                    RealPoint ptRealSpace = new RealPoint(3);
                    sacTransform.apply(pt,ptRealSpace);

                    double projectedPointOnSlicingAxis =
                            ptRealSpace.getDoublePosition(0)*slicingTransfom.get(2,0)+
                                    ptRealSpace.getDoublePosition(1)*slicingTransfom.get(2,1)+
                                    ptRealSpace.getDoublePosition(2)*slicingTransfom.get(2,2);
                    if (projectedPointOnSlicingAxis<minZAxis)
                        minZAxis = projectedPointOnSlicingAxis;
                    if (projectedPointOnSlicingAxis>maxZAxis)
                        maxZAxis = projectedPointOnSlicingAxis;

                    double projectedPointOnSlicingXAxis =
                            ptRealSpace.getDoublePosition(0)*slicingTransfom.get(0,0)+
                                    ptRealSpace.getDoublePosition(1)*slicingTransfom.get(0,1)+
                                    ptRealSpace.getDoublePosition(2)*slicingTransfom.get(0,2);
                    if (projectedPointOnSlicingXAxis<minXAxis)
                        minXAxis = projectedPointOnSlicingXAxis;
                    if (projectedPointOnSlicingXAxis>maxXAxis)
                        maxXAxis = projectedPointOnSlicingXAxis;

                    double projectedPointOnSlicingYAxis =
                            ptRealSpace.getDoublePosition(0)*slicingTransfom.get(1,0)+
                                    ptRealSpace.getDoublePosition(1)*slicingTransfom.get(1,1)+
                                    ptRealSpace.getDoublePosition(2)*slicingTransfom.get(1,2);
                    if (projectedPointOnSlicingYAxis<minYAxis)
                        minYAxis = projectedPointOnSlicingYAxis;
                    if (projectedPointOnSlicingYAxis>maxYAxis)
                        maxYAxis = projectedPointOnSlicingYAxis;

                }

        // Adds a margin of 15 % for tilt correction
        double cZ = (minZAxis+maxZAxis)/2.0;
        double dZ = (maxZAxis-minZAxis)/2.0;
        minZAxis = cZ - dZ*1.15;
        maxZAxis = cZ + dZ*1.15;

        // Adds margin XY 15 % also for correct registration
        double cX = (maxXAxis+minXAxis)/2.0;
        double cY = (maxYAxis+minYAxis)/2.0;

        double dX = (maxXAxis-minXAxis)/2.0;
        double dY = (maxYAxis-minYAxis)/2.0;

        maxXAxis = cX+dX*1.15;
        maxYAxis = cY+dY*1.15;

        minXAxis = cX-dX*1.15;
        minYAxis = cY-dY*1.15;

        // Gets slicing resolution
        // TODO : check null pointer exception if getvoxel not present
        //double slicingResolution = 0.01;

        slicingTransfom.scale(slicingResolution);

        long nPixX = (long)((maxXAxis-minXAxis)/slicingResolution);
        long nPixY = (long)((maxYAxis-minYAxis)/slicingResolution);
        long nPixZ = (long)((maxZAxis-minZAxis)/slicingResolution);

        adjustShiftSlicingTransform(slicingTransfom, nPixX, nPixY, nPixZ);

        // ------------------- NOW COMPUTES SOURCEANDCONVERTERS
        // 0 - slicing model : empty source but properly defined in space and resolution
        AffineTransform3D m = new AffineTransform3D();
        /*SourceAndConverter nonWrappedSlicingModel = new EmptySourceAndConverterCreator("SlicingModel", m,
                nPixX,
                nPixY,
                nPixZ
        ).get();*/
        SourceAndConverter nonWrappedSlicingModel = new EmptyMultiResolutionSourceAndConverterCreator("SlicingModel", m,
                nPixX,
                nPixY,
                nPixZ,
                2,2,1,5
        ).get();

        if (slicingModel!=null) {
            SourceAndConverterServices.getSourceAndConverterService().remove(slicingModel); // Hmm maybe I should document what I do...
        }
        // Wrapped as TransformedSource to adjust slicing
        slicingModel = new SourceAffineTransformer(nonWrappedSlicingModel, slicingTransfom).getSourceOut();

        SourceAndConverterServices.getSourceAndConverterService().register(slicingModel);

        // 1 -
        extendedSlicedSources = new SourceAndConverter[ba.map.getStructuralImages().length+1];
        SourceAndConverter[] tempNonExtendedSlicedSources = new SourceAndConverter[ba.map.getStructuralImages().length+1];

        SourceMosaicZSlicer mosaic = new SourceMosaicZSlicer(null, slicingModel, true, false, false,
                () -> getStep());

        SourceResampler resampler = new SourceResampler(null, slicingModel, true, false, false);

        centerTransform = null;
        for (int index = 0; index<ba.map.getStructuralImages().length+1;index++) {
            SourceAndConverter sac;
            if (index<ba.map.getStructuralImages().length) {
                sac = ba.map.getStructuralImages()[index];
            } else {
                sac = ba.map.getLabelImage();
            }

            SourceAndConverter reslicedSac = mosaic.apply(sac);
            tempNonExtendedSlicedSources[index] = resampler.apply(sac);

            if (centerTransform == null) {
                centerTransform = new AffineTransform3D();
                reslicedSac.getSpimSource().getSourceTransform(0,0,centerTransform);
                RealPoint ptCenterGlobal = new RealPoint(3);

                long[] dims = new long[3];
                reslicedSac.getSpimSource().getSource(0,0).dimensions(dims);
                dims[0] = dims[0]/dims[2];
                RealPoint ptCenterPixel = new RealPoint((dims[0]-1.0),(dims[1]-1.0)/2.0, 0);//(dims[2]-1.0)/2.0);

                centerTransform.apply(ptCenterPixel, ptCenterGlobal);
                centerTransform.identity();
                centerTransform.translate(-ptCenterGlobal.getDoublePosition(0),
                        -ptCenterGlobal.getDoublePosition(1),
                        0
                );

            }

            reslicedSac = new SourceAffineTransformer(null, centerTransform).apply(reslicedSac);
            SourceAndConverterServices.getSourceAndConverterService()
                    .register(reslicedSac);
            extendedSlicedSources[index] = reslicedSac;
        }

        nonExtendedSlicedSources = nonExtendedAffineTransform.getTransformedImageMovingToFixed(tempNonExtendedSlicedSources);

    }

    /**
     * ?? TODO : doc
     * @param slicingTransfom
     * @param nX
     * @param nY
     * @param nZ
     */
    public static void adjustShiftSlicingTransform(AffineTransform3D slicingTransfom, long nX, long nY, long nZ) {
        AffineTransform3D notShifted = new AffineTransform3D();
        notShifted.set(slicingTransfom);
        notShifted.set(0,0,3);
        notShifted.set(0,1,3);
        notShifted.set(0,2,3);

        RealPoint pt = new RealPoint(nX, nY, nZ);

        RealPoint ptRealSpace = new RealPoint(3);

        notShifted.apply(pt, ptRealSpace);

        slicingTransfom.set(-ptRealSpace.getDoublePosition(0)/2.0, 0,3);
        slicingTransfom.set(-ptRealSpace.getDoublePosition(1)/2.0, 1,3);
        slicingTransfom.set(-ptRealSpace.getDoublePosition(2)/2.0, 2,3);
    }

    boolean lock = false;

    public void lock() {
        lock = true;
    }

    public void unlock() {
        lock = false;
    }

    public void setStep(int zStep) {
        if (!lock)
        if ((zStep > 0)&&(zStep!=this.zStep)) {
            this.zStep = zStep;
            slicingUpdate();
            listeners.forEach(r -> r.run());
        }
    }

    public void setRotateX(double rx) {
        if (!lock)
        if (rx!=this.rx) {
            this.rx = rx;
            slicingUpdate();
            listeners.forEach(r -> r.run());
        }
    }

    public void setRotateY(double ry) {
        if (!lock)
        if (ry!=this.ry) {
            this.ry = ry;
            slicingUpdate();
            listeners.forEach(r -> r.run());
        }
    }

    public long getStep() {
        return (long) zStep;
    }

    void slicingUpdate() {

        long nPixX = slicingModel.getSpimSource().getSource(0,0).max(0);
        long nPixY = slicingModel.getSpimSource().getSource(0,0).max(1);
        long nPixZ = slicingModel.getSpimSource().getSource(0,0).max(2);

        AffineTransform3D slicingTransfom = this.slicingTransfom.copy();

        // Removes shift XYZ
        slicingTransfom.set(0,0,3);
        slicingTransfom.set(0,1,3);
        slicingTransfom.set(0,2,3);

        // Store Z Axis
        double m20 = slicingTransfom.get(0,2);
        double m21 = slicingTransfom.get(1,2);
        double m22 = slicingTransfom.get(2,2);

        slicingTransfom.rotate(0,rx);//180.0*Math.PI);
        slicingTransfom.rotate(1,ry);///180.0*Math.PI);

        // ReStore ZAxis
        slicingTransfom.set(m20, 0,2 );
        slicingTransfom.set(m21, 1,2 );
        slicingTransfom.set(m22, 2,2 );

        adjustShiftSlicingTransform(slicingTransfom, nPixX, nPixY, nPixZ);

        ((TransformedSource) (slicingModel.getSpimSource())).setFixedTransform(slicingTransfom);

        AffineTransform3D at3d = new AffineTransform3D();

        AffineTransform3D atToInvert = new AffineTransform3D();

        this.slicingModel.getSpimSource().getSourceTransform(0,0,atToInvert);

        this.slicingModel.getSpimSource().getSourceTransform(0,0,at3d);
        at3d.set(0,0,3);
        at3d.set(0,1,3);
        at3d.set(0,2,3);
        double xScale = getNormTransform(0, at3d);
        double yScale = getNormTransform(1, at3d);
        double zScale = getNormTransform(2, at3d);
        at3d.identity();
        at3d.scale(xScale, yScale, zScale);

        AffineTransform3D centerTr = centerTransform.copy();
        centerTr.translate(-centerTransform.get(0,3)/2,0,0);

        at3d = at3d.preConcatenate(centerTr);

        nonExtendedAffineTransform.setAffineTransform(atToInvert.inverse().preConcatenate(at3d));
    }

    /**
     * Returns the norm of an axis after an affinetransform is applied
     * @param axis
     * @param t
     * @return
     */
    static public double getNormTransform(int axis, AffineTransform3D t) {
        double f0 = t.get(0,axis);
        double f1 = t.get(1,axis);
        double f2 = t.get(2,axis);
        return Math.sqrt(f0 * f0 + f1 * f1 + f2 * f2);
    }

}
