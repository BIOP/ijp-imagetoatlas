package ch.epfl.biop.atlastoimg2d.multislice;


import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlastoimg2d.AtlasToSourceAndConverter2D;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SliceSources {
    // What are they ?
    SourceAndConverter[] original_sacs;

    // Visible to the user in slicing mode
    SourceAndConverter[] relocated_sacs_slicing_mode;

    // Visible to the user in 3D mode
    SourceAndConverter[] relocated_sacs_3D_mode;

    // Used for registration : like 3D, but tilt and roll ignored because it is handled on the fixed source side
    SourceAndConverter[] relocated_sacs_registration_mode;

    // Where are they ?
    double slicingAxisPosition;

    AffineTransform3D at3D;

    boolean isSelected = false;

    boolean isLocked = false;

    double yShift_slicing_mode = 0;

    AtlasToSourceAndConverter2D aligner;

    final MultiSlicePositioner mp;

    // For fast display : Icon TODO : see https://github.com/bigdataviewer/bigdataviewer-core/blob/17d2f55d46213d1e2369ad7ef4464e3efecbd70a/src/main/java/bdv/tools/RecordMovieDialog.java#L256-L318
    protected SliceSources(SourceAndConverter[] sacs, double slicingAxisPosition, MultiSlicePositioner mp) {
        this.mp = mp;
        this.original_sacs = sacs;
        this.slicingAxisPosition = slicingAxisPosition;

        aligner = new AtlasToSourceAndConverter2D();
        aligner.setScijavaContext(mp.scijavaCtx);

        List<SourceAndConverter<?>> sacsTransformed = new ArrayList<>();
        at3D = new AffineTransform3D();
        for (SourceAndConverter sac : sacs) {
            RealPoint center = new RealPoint(3);
            center.setPosition(new double[] {0,0,0}); // Center
            SourceAndConverter zeroCenteredSource = recenterSourcesAppend(sac, center);
            sacsTransformed.add(new SourceAffineTransformer(zeroCenteredSource, at3D).getSourceOut());
        }

        this.relocated_sacs_slicing_mode = sacsTransformed.toArray(new SourceAndConverter[sacsTransformed.size()]);

        sacsTransformed.clear();
        for (SourceAndConverter sac : sacs) {
            RealPoint center = new RealPoint(3);
            center.setPosition(new double[] {0,0,0}); // Center
            SourceAndConverter zeroCenteredSource = recenterSourcesAppend(sac, center);
            sacsTransformed.add(new SourceAffineTransformer(zeroCenteredSource, at3D).getSourceOut());
        }

        this.relocated_sacs_registration_mode = sacsTransformed.toArray(new SourceAndConverter[sacsTransformed.size()]);

        sacsTransformed.clear();
        for (SourceAndConverter sac : sacs) {
            RealPoint center = new RealPoint(3);
            center.setPosition(new double[] {0,0,0}); // Center
            SourceAndConverter zeroCenteredSource = recenterSourcesAppend(sac, center);
            sacsTransformed.add(new SourceAffineTransformer(zeroCenteredSource, at3D).getSourceOut());
        }
        this.relocated_sacs_3D_mode = sacsTransformed.toArray(new SourceAndConverter[sacsTransformed.size()]);


        aligner.setImage(relocated_sacs_registration_mode);
    }

    protected boolean exactMatch(List<SourceAndConverter<?>> testSacs) {
        Set originalSacsSet = new HashSet();
        for (SourceAndConverter sac : original_sacs) {
            originalSacsSet.add(sac);
        }
        if (originalSacsSet.containsAll(testSacs) && testSacs.containsAll(originalSacsSet)) {
            return true;
        }
        Set transformedSacsSet = new HashSet();
        for (SourceAndConverter sac : relocated_sacs_slicing_mode) {
            transformedSacsSet.add(sac);
        }
        if (transformedSacsSet.containsAll(testSacs) && testSacs.containsAll(transformedSacsSet)) {
            return true;
        }

        return false;
    }

    protected void updatePosition() {
        double posX = ((slicingAxisPosition/mp.sizePixX/mp.zStepSetter.getStep())) * mp.sX;
        double posY = mp.sY * yShift_slicing_mode;

        AffineTransform3D new_at3D = new AffineTransform3D();
        new_at3D.set(posX,0,3);
        new_at3D.set(posY,1,3);

        for (SourceAndConverter sac : relocated_sacs_slicing_mode) {
            assert sac.getSpimSource() instanceof TransformedSource;
            ((TransformedSource)sac.getSpimSource()).setFixedTransform(new_at3D);
        }

        for (SourceAndConverter sac : relocated_sacs_registration_mode) {
            assert sac.getSpimSource() instanceof TransformedSource;
            new_at3D = new AffineTransform3D();
            ((TransformedSource)sac.getSpimSource()).getFixedTransform(new_at3D);
            new_at3D.set(slicingAxisPosition,2,3);
            ((TransformedSource)sac.getSpimSource()).setFixedTransform(new_at3D);
        }

        for (SourceAndConverter sac : relocated_sacs_3D_mode) {
            assert sac.getSpimSource() instanceof TransformedSource;
            new_at3D = new AffineTransform3D();
            ((TransformedSource)sac.getSpimSource()).getFixedTransform(new_at3D);
            new_at3D.set(slicingAxisPosition,2,3);
            ((TransformedSource)sac.getSpimSource()).setFixedTransform(new_at3D);
        }

    }

    /**
     * The source should be affined transformed only
     * @return
     */
    public static SourceAndConverter recenterSourcesAppend(SourceAndConverter source, RealPoint center) {
        // Affine Transform source such as the center is located at the center of where
        // we want it to be
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.identity();
        //double[] m = at3D.getRowPackedCopy();
        source.getSpimSource().getSourceTransform(0,0,at3D);
        long[] dims = new long[3];
        source.getSpimSource().getSource(0,0).dimensions(dims);

        RealPoint ptCenterGlobal = new RealPoint(3);
        RealPoint ptCenterPixel = new RealPoint((dims[0]-1.0)/2.0,(dims[1]-1.0)/2.0, (dims[2]-1.0)/2.0);

        at3D.apply(ptCenterPixel, ptCenterGlobal);

        // Just shifting
        double[] m = new AffineTransform3D().getRowPackedCopy();

        m[3] = center.getDoublePosition(0)-ptCenterGlobal.getDoublePosition(0);

        m[7] = center.getDoublePosition(1)-ptCenterGlobal.getDoublePosition(1);

        m[11] = center.getDoublePosition(2)-ptCenterGlobal.getDoublePosition(2);

        at3D.set(m);

        return new SourceAffineTransformer(source, at3D).getSourceOut();
    }

    boolean processInProgress = false; // flag : true if a registration process is in progress

}