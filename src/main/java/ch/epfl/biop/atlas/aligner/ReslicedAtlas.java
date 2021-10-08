package ch.epfl.biop.atlas.aligner;

import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.sourceandconverter.EmptyMultiResolutionSourceAndConverterCreator;
import ch.epfl.biop.registration.sourceandconverter.affine.AffineTransformedSourceWrapperRegistration;
import ch.epfl.biop.sourceandconverter.transform.SourceMosaicZSlicer;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.LinAlgHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.util.ArrayList;
import java.util.List;

public class ReslicedAtlas implements RealInterval {

    protected static Logger logger = LoggerFactory.getLogger(ReslicedAtlas.class);

    final public Atlas ba;

    AffineTransform3D slicingTransfom = new AffineTransform3D();

    double slicingResolution = -1;

    SourceAndConverter slicingModel;

    public SourceAndConverter[] extendedSlicedSources; // TODO : make accessors

    public SourceAndConverter[] nonExtendedSlicedSources; // TODO : make accessors

    AffineTransform3D centerTransform;

    AffineTransformedSourceWrapperRegistration nonExtendedAffineTransform = new AffineTransformedSourceWrapperRegistration();

    private volatile int zStep = 1;

    // rotation angle in radian
    private double rotx;

    // rotation angle in radian
    private double roty;

    private double cX, cY, cZ;

    List<Runnable> listeners = new ArrayList<>();

    public ReslicedAtlas(Atlas ba) {
        this.ba = ba;
    }

    public void addListener(Runnable runnable) {
        listeners.add(runnable);
    }

    public void setSlicingTransform(AffineTransform3D slicingTransfom) {
        this.slicingTransfom = slicingTransfom;
        computeReslicedSources();
    }

    public AffineTransform3D getSlicingTransform() {
        return slicingTransfom.copy();
    }

    public void setResolution(double slicingResolution) {
        this.slicingResolution = slicingResolution;
        computeReslicedSources();
    }

    double minZAxis = Double.MAX_VALUE;
    double maxZAxis = -Double.MAX_VALUE;

    double minXAxis = Double.MAX_VALUE;
    double maxXAxis = -Double.MAX_VALUE;

    double minYAxis = Double.MAX_VALUE;
    double maxYAxis = -Double.MAX_VALUE;

    void computeReslicedSources() {
        if ((slicingTransfom==null)||(slicingResolution<=0)) {
            logger.error("No slicing transform or slicing resolution specified");
            return;
        }

        // ----------------------------- COMPUTES BOUNDS

        // No let's check for bounds along the z axis
        // Pick the first SourceAndConverter
        SourceAndConverter sacForBoundsTesting = ba.getMap().getStructuralImages().get(ba.getMap().getImagesKeys().get(0));

        // Gets level 0 (and timepoint 0) and source transform
        AffineTransform3D sacTransform = new AffineTransform3D();
        sacForBoundsTesting.getSpimSource().getSourceTransform(0,0, sacTransform);

        RandomAccessibleInterval rai = sacForBoundsTesting.getSpimSource().getSource(0,0);

        minZAxis = Double.MAX_VALUE;
        maxZAxis = -Double.MAX_VALUE;

        minXAxis = Double.MAX_VALUE;
        maxXAxis = -Double.MAX_VALUE;

        minYAxis = Double.MAX_VALUE;
        maxYAxis = -Double.MAX_VALUE;

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
        cZ = (minZAxis+maxZAxis)/2.0;
        double dZ = (maxZAxis-minZAxis)/2.0;
        minZAxis = cZ - dZ*1.15;
        maxZAxis = cZ + dZ*1.15;

        // Adds margin XY 15 % also for correct registration
        cX = (maxXAxis+minXAxis)/2.0;
        cY = (maxYAxis+minYAxis)/2.0;

        double dX = (maxXAxis-minXAxis)/2.0;
        double dY = (maxYAxis-minYAxis)/2.0;

        maxXAxis = cX+dX*1.15;
        maxYAxis = cY+dY*1.15;

        minXAxis = cX-dX*1.15;
        minYAxis = cY-dY*1.15;

        RealPoint realCenter = new RealPoint(cX, cY, cZ);

        slicingTransfom.inverse().apply(realCenter, realCenter);

        cX = realCenter.getDoublePosition(0);
        cY = realCenter.getDoublePosition(1);
        cZ = realCenter.getDoublePosition(2);

        slicingTransfom.scale(slicingResolution);

        long nPixX = (long)((maxXAxis-minXAxis)/slicingResolution);
        long nPixY = (long)((maxYAxis-minYAxis)/slicingResolution);
        long nPixZ = (long)((maxZAxis-minZAxis)/slicingResolution);

        adjustShiftSlicingTransform(slicingTransfom, cX, cY, cZ, nPixX, nPixY, nPixZ);

        // ------------------- NOW COMPUTES SOURCEANDCONVERTERS
        // 0 - slicing model : empty source but properly defined in space and resolution
        AffineTransform3D m = new AffineTransform3D();

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

        AtlasMap map = ba.getMap();

        // 1 -
        extendedSlicedSources = new SourceAndConverter[map.getStructuralImages().size()+1];
        SourceAndConverter[] tempNonExtendedSlicedSources = new SourceAndConverter[map.getStructuralImages().size()+1];

        SourceMosaicZSlicer mosaic = new SourceMosaicZSlicer(null, slicingModel, true, false, false,
                this::getStep);

        SourceResampler resampler = new SourceResampler(null, slicingModel,slicingModel.getSpimSource().getName(), true, false, false, 0);

        centerTransform = null;

        List<String> keys = map.getImagesKeys();

        for (int index = 0; index<map.getStructuralImages().size()+1;index++) {
            SourceAndConverter sac;
            if (index<map.getStructuralImages().size()) {
                System.out.println("index = "+index+"| source key: "+keys.get(index));
                sac = map.getStructuralImages().get(keys.get(index));
                System.out.println(sac.getSpimSource().getName());
            } else {
                labelIndex = index;
                System.out.println("index = "+index+"| LABELS");
                sac = map.getLabelImage();
                System.out.println(sac.getSpimSource().getName());
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
            //SourceAndConverterServices.getSourceAndConverterService()
            //        .register(reslicedSac);
            extendedSlicedSources[index] = reslicedSac;
        }

        nonExtendedSlicedSources = nonExtendedAffineTransform.getTransformedImageMovingToFixed(tempNonExtendedSlicedSources);
        //slicingUpdate();
    }

     static void adjustShiftSlicingTransform(AffineTransform3D slicingTransfom, double cx, double cy, double cz, long nX, long nY, long nZ) {
        AffineTransform3D notShifted = new AffineTransform3D();
        notShifted.set(slicingTransfom);
        notShifted.set(0,0,3);
        notShifted.set(0,1,3);
        notShifted.set(0,2,3);

        // We want that [notShifted+TR].[nx/2, nY/2, nZ/2, 1] = [cx, cy, cz, 1]
        // [notShifted].[nx/2, nY/2, nZ/2, 1] + TR.[nx/2, nY/2, nZ/2, 1] = [cx, cy, cz]
        // [TRX, TRY, TRZ] = [cx, cy, cz] - [notShifted].[nx/2, nY/2, nZ/2, 1]

        RealPoint pt = new RealPoint(nX/2.0, nY/2.0, nZ/2.0);
        //pt.setPosition(new double[]{cx, cy, cz});

        RealPoint ptRealSpace = new RealPoint(3);

        notShifted.apply(pt, ptRealSpace);

        slicingTransfom.set(cx-ptRealSpace.getDoublePosition(0), 0,3);
        slicingTransfom.set(cy-ptRealSpace.getDoublePosition(1), 1,3);
        slicingTransfom.set(cz-ptRealSpace.getDoublePosition(2), 2,3);

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
            listeners.forEach(Runnable::run);
        }
    }

    /**
     * Set slicing angle adjustement (rotation around x), in radian
     * Listeners are called to update potential viewers, etc.
     * @param rx angle in radian
     */
    public void setRotateX(double rx) {
        if (!lock)
        if (rx!=this.rotx) {
            this.rotx = rx;
            slicingUpdate();
            listeners.forEach(Runnable::run);
        }
    }

    public double getRotateX() {
        return rotx;
    }

    public double getRotateY() {
        return roty;
    }

    /**
     * Set slicing angle adjustement (rotation around x), in radian
     * Listeners are called to update potential viewers, etc.
     * @param ry angle in radian
     */
    public void setRotateY(double ry) {
        if (!lock)
        if (ry!=this.roty) {
            this.roty = ry;
            slicingUpdate();
            listeners.forEach(Runnable::run);
        }
    }

    public long getStep() {
        return zStep;
    }

    AffineTransform3D atlasToSlicingTransform = new AffineTransform3D();

    void slicingUpdate() {
        // Pfou I don't understand anything anymore ...
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

        // Store X Axis
        double m00 = slicingTransfom.get(0,0);
        double m01 = slicingTransfom.get(1,0);
        double m02 = slicingTransfom.get(2,0);

        // Store Y Axis
        double m10 = slicingTransfom.get(0,1);
        double m11 = slicingTransfom.get(1,1);
        double m12 = slicingTransfom.get(2,1);

        // Rotate first along Y then along X

        // A spatial rotation around a fixed point of θ {\displaystyle \theta } \theta radians about a unit axis ( X , Y , Z ) {\displaystyle (X,Y,Z)} (X,Y,Z) that denotes the Euler axis is given by the quaternion ( C , X S , Y S , Z S ) {\displaystyle (C,X\,S,Y\,S,Z\,S)} {\displaystyle (C,X\,S,Y\,S,Z\,S)}, where C = cos ⁡ ( θ / 2 ) {\displaystyle C=\cos(\theta /2)} {\displaystyle C=\cos(\theta /2)} and S = sin ⁡ ( θ / 2 ) {\displaystyle S=\sin(\theta /2)} {\displaystyle S=\sin(\theta /2)}.

        double[] qx = new double[4];
        double normRx = Math.sqrt(m00*m00+m01*m01+m02*m02);
        m00/=normRx;m01/=normRx;m02/=normRx;

        qx[0] = Math.cos(rotx / 2.0);
        qx[1] = Math.sin(rotx / 2.0)*m00;
        qx[2] = Math.sin(rotx / 2.0)*m01;
        qx[3] = Math.sin(rotx / 2.0)*m02;

        double[] qy = new double[4];
        double normRy = Math.sqrt(m10*m10+m11*m11+m12*m12);
        m10/=normRy;m11/=normRy;m12/=normRy;

        qy[0] = Math.cos(roty / 2.0);
        qy[1] = Math.sin(roty / 2.0)*m10;
        qy[2] = Math.sin(roty / 2.0)*m11;
        qy[3] = Math.sin(roty / 2.0)*m12;

        double[] qXY = new double[4];

        LinAlgHelpers.quaternionMultiply(qy,qx,qXY);

        AffineTransform3D rotMatrix = new AffineTransform3D();

        double [][] m = new double[3][3];

        if ((rotx !=0)||(roty !=0)) {
            LinAlgHelpers.quaternionToR(qXY, m);

            rotMatrix.set(m[0][0], m[0][1], m[0][2], 0,
                    m[1][0], m[1][1], m[1][2], 0,
                    m[2][0], m[2][1], m[2][2], 0);

            slicingTransfom.preConcatenate(rotMatrix);
        }

        // Restore Z axis
        slicingTransfom.set(m20, 0,2 );
        slicingTransfom.set(m21, 1,2 );
        slicingTransfom.set(m22, 2,2 );

        adjustShiftSlicingTransform(slicingTransfom, cX, cY, cZ, nPixX, nPixY, nPixZ);

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

        at3d.preConcatenate(centerTr);

        atlasToSlicingTransform = atToInvert.inverse().preConcatenate(at3d);
        nonExtendedAffineTransform.setAffineTransform(atlasToSlicingTransform);
    }

    /**
     * Returns the norm of an axis after an affinetransform is applied
     * @param axis 0 1 2 corresponds to x y z
     * @param t transform matrix
     * @return the norm of an axis after an affinetransform is applied
     */
    static public double getNormTransform(int axis, AffineTransform3D t) {
        double f0 = t.get(0,axis);
        double f1 = t.get(1,axis);
        double f2 = t.get(2,axis);
        return Math.sqrt(f0 * f0 + f1 * f1 + f2 * f2);
    }

    @Override
    public double realMin(int i) {
        switch (i) {
            case 0:return minXAxis-(maxXAxis+minXAxis)/2.0;
            case 1:return minYAxis-(maxYAxis+minYAxis)/2.0;
            case 2:return minZAxis-(maxZAxis+minZAxis)/2.0;
            default:return -Double.MAX_VALUE;
        }
    }

    @Override
    public double realMax(int i) {
        switch (i) {
            case 0:return maxXAxis-(maxXAxis+minXAxis)/2.0;
            case 1:return maxYAxis-(maxYAxis+minYAxis)/2.0;
            case 2:return maxZAxis-(maxZAxis+minZAxis)/2.0;
            default:return Double.MAX_VALUE;
        }
    }

    @Override
    public int numDimensions() {
        return 3;
    }

    public AffineTransform3D getSlicingTransformToAtlas() {
        return atlasToSlicingTransform.inverse().copy();
    }

    private int labelIndex = -1;

    public int getLabelSourceIndex() {
        return labelIndex;
    }

    public int getCoordinateSourceIndex(int coordinates) {
        return getLabelSourceIndex()-(2-coordinates)-2;
    }

    public int getLeftRightSourceIndex() {
        return getLabelSourceIndex()-1;
    }

}
