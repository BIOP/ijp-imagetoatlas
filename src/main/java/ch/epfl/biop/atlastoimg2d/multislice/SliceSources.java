package ch.epfl.biop.atlastoimg2d.multislice;


import bdv.tools.transformation.TransformedSource;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.sourceandconverter.AffineTransformedSourceWrapperRegistration;
import ch.epfl.biop.registration.sourceandconverter.CenterZeroRegistration;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SliceSources {
    // What are they ?
    SourceAndConverter[] original_sacs;

    // Visible to the user in slicing mode
    SourceAndConverter[] relocated_sacs_positioning_mode;

    // Visible to the user in 3D mode
    // SourceAndConverter[] relocated_sacs_3D_mode;

    // Used for registration : like 3D, but tilt and roll ignored because it is handled on the fixed source side
    SourceAndConverter[] registered_sacs;

    // Where are they ?
    double slicingAxisPosition;

    AffineTransform3D at3D;

    boolean isSelected = false;

    boolean isLocked = false;

    double yShift_slicing_mode = 0;

    final MultiSlicePositioner mp;

    List<GraphicalHandle> ghs = new ArrayList<>();

    Behaviours behavioursHandleSlice;

    volatile AffineTransformedSourceWrapperRegistration zPositioner;

    volatile AffineTransformedSourceWrapperRegistration slicingModePositioner;

    CenterZeroRegistration centerPositioner;

    // For fast display : Icon TODO : see https://github.com/bigdataviewer/bigdataviewer-core/blob/17d2f55d46213d1e2369ad7ef4464e3efecbd70a/src/main/java/bdv/tools/RecordMovieDialog.java#L256-L318
    protected SliceSources(SourceAndConverter[] sacs, double slicingAxisPosition, MultiSlicePositioner mp) {
        this.mp = mp;
        this.original_sacs = sacs;
        this.slicingAxisPosition = slicingAxisPosition;

        this.registered_sacs = this.original_sacs;

        centerPositioner = new CenterZeroRegistration();
        centerPositioner.setMovingImage(registered_sacs);

        this.addRegistration(centerPositioner, Function.identity(), Function.identity());

        zPositioner = new AffineTransformedSourceWrapperRegistration();

        this.addRegistration(zPositioner, Function.identity(), Function.identity());
        this.waitForEndOfRegistrations();

        updatePosition();


        /*List<SourceAndConverter<?>> sacsTransformed = new ArrayList<>();
        at3D = new AffineTransform3D();
        for (SourceAndConverter sac : sacs) {
            RealPoint center = new RealPoint(3);
            center.setPosition(new double[] {0,0,0}); // Center
            SourceAndConverter zeroCenteredSource = recenterSourcesAppend(sac, center);
            sacsTransformed.add(new SourceAffineTransformer(zeroCenteredSource, at3D).getSourceOut());
        }

        this.relocated_sacs_positioning_mode = sacsTransformed.toArray(new SourceAndConverter[sacsTransformed.size()]);

        sacsTransformed.clear();
        for (SourceAndConverter sac : sacs) {
            RealPoint center = new RealPoint(3);
            center.setPosition(new double[] {0,0,0}); // Center
            SourceAndConverter zeroCenteredSource = recenterSourcesAppend(sac, center);
            sacsTransformed.add(new SourceAffineTransformer(zeroCenteredSource, at3D).getSourceOut());
        }

        this.registered_sacs = sacsTransformed.toArray(new SourceAndConverter[sacsTransformed.size()]);

        sacsTransformed.clear();
        for (SourceAndConverter sac : sacs) {
            RealPoint center = new RealPoint(3);
            center.setPosition(new double[] {0,0,0}); // Center
            SourceAndConverter zeroCenteredSource = recenterSourcesAppend(sac, center);
            sacsTransformed.add(new SourceAffineTransformer(zeroCenteredSource, at3D).getSourceOut());
        }

        this.relocated_sacs_3D_mode = sacsTransformed.toArray(new SourceAndConverter[sacsTransformed.size()]);*/

        behavioursHandleSlice = new Behaviours(new InputTriggerConfig());

        behavioursHandleSlice.behaviour(mp.getSelectedSourceDragBehaviour(this), "dragSelectedSources"+this.toString(), "button1");
        behavioursHandleSlice.behaviour((ClickBehaviour) (x,y) -> isSelected = false, "deselectedSources"+this.toString(), "button3", "ctrl button1");

        GraphicalHandle gh = new CircleGraphicalHandle(mp,
                    behavioursHandleSlice,
                    mp.bdvh.getTriggerbindings(),
                    this.toString(), // pray for unicity ? TODO : do better than thoughts and prayers
                    this::getBdvHandleCoords,
                    this::getBdvHandleRadius,
                    this::getBdvHandleColor
                );
        ghs.add(gh);

    }

    public Integer[] getBdvHandleCoords() {
        RealPoint sliceCenter = SourceAndConverterUtils.getSourceAndConverterCenterPoint(relocated_sacs_positioning_mode[0]);

        AffineTransform3D bdvAt3D = new AffineTransform3D();

        mp.bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);
        bdvAt3D.apply(sliceCenter, sliceCenter);

        return new Integer[]{(int)sliceCenter.getDoublePosition(0), (int)sliceCenter.getDoublePosition(1)};
    }

    public Integer[] getBdvHandleColor() {
        if (isSelected) {
            return new Integer[]{0,255,0,200};

        } else {
            return new Integer[]{255,255,0,64};
        }
    }

    public Integer getBdvHandleRadius() {
        return 25;
    }

    public void drawGraphicalHandles(Graphics2D g) {
        ghs.forEach(gh -> gh.draw(g));
    }

    public void disableGraphicalHandles() {
        ghs.forEach(gh -> gh.disable());
    }

    public void enableGraphicalHandles() {
        ghs.forEach(gh -> gh.enable());
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
        for (SourceAndConverter sac : relocated_sacs_positioning_mode) {
            transformedSacsSet.add(sac);
        }
        if (transformedSacsSet.containsAll(testSacs) && testSacs.containsAll(transformedSacsSet)) {
            return true;
        }

        return false;
    }

    protected boolean isContainingAny(Collection<SourceAndConverter<?>> sacs) {
        Set originalSacsSet = new HashSet();
        for (SourceAndConverter sac : original_sacs) {
            originalSacsSet.add(sac);
        }
        if (sacs.stream().distinct().anyMatch(originalSacsSet::contains)) {
            return true;
        }
        Set transformedSacsSet = new HashSet();
        for (SourceAndConverter sac : relocated_sacs_positioning_mode) {
            transformedSacsSet.add(sac);
        }
        if (sacs.stream().distinct().anyMatch(transformedSacsSet::contains)) {
            return true;
        }
        return false;
    }

    public void waitForEndOfRegistrations() {
        try {
            CompletableFuture.allOf(registrationTasks).get();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Some registration were cancelled");
        }
    }

    protected synchronized void updatePosition() {

        AffineTransform3D zShiftAffineTransform = new AffineTransform3D();

        zShiftAffineTransform.translate(0,0,slicingAxisPosition);

        zPositioner.setAffineTransform(zShiftAffineTransform); // Moves the registered slices to the correct position

        AffineTransform3D slicingModePositionAffineTransform = new AffineTransform3D();

        double posX = ((slicingAxisPosition/mp.sizePixX/mp.zStepSetter.getStep())) * mp.sX;
        double posY = mp.sY * yShift_slicing_mode;

        System.out.println("posX = "+posX);
        System.out.println("posY = "+posY);

        slicingModePositionAffineTransform.translate(posX, posY, -slicingAxisPosition );

        slicingModePositioner.setAffineTransform(slicingModePositionAffineTransform);

        /*for (SourceAndConverter sac : registered_sacs) {
            assert sac.getSpimSource() instanceof TransformedSource;
            new_at3D = new AffineTransform3D();
            ((TransformedSource)sac.getSpimSource()).getFixedTransform(new_at3D);
            new_at3D.set(slicingAxisPosition,2,3);
            ((TransformedSource)sac.getSpimSource()).setFixedTransform(new_at3D);
        }*/

        /*new_at3D = new AffineTransform3D();


        new_at3D.set(posX,0,3);
        new_at3D.set(posY,1,3);

        for (int i =
        )

        for (SourceAndConverter sac : relocated_sacs_positioning_mode) {
            assert sac.getSpimSource() instanceof TransformedSource;
            ((TransformedSource)sac.getSpimSource()).setFixedTransform(new_at3D);
        }*/

        /*for (SourceAndConverter sac : relocated_sacs_3D_mode) {
            assert sac.getSpimSource() instanceof TransformedSource;
            new_at3D = new AffineTransform3D();
            ((TransformedSource)sac.getSpimSource()).getFixedTransform(new_at3D);
            new_at3D.set(slicingAxisPosition,2,3);
            ((TransformedSource)sac.getSpimSource()).setFixedTransform(new_at3D);
        }*/

    }

    /**
     * The source should be affined transformed only
     * @return
     */
    /*public static SourceAndConverter recenterSourcesAppend(SourceAndConverter source, RealPoint center) {
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
    }*/

    boolean processInProgress = false; // flag : true if a registration process is in progress


    public CompletableFuture<Boolean> registrationTasks = CompletableFuture.completedFuture(true);

    private List<Registration> registrations = new ArrayList<>();

    /**
     * Asynchronous handling of registrations + combining with manual sequential registration if necessary
     * @param reg
     */

    protected void addRegistration(Registration<SourceAndConverter[]> reg,
                                   Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed,
                                   Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving) {

        if (registrationTasks == null || registrationTasks.isDone()) {
            registrationTasks = CompletableFuture.supplyAsync(() -> true); // Starts async computation, maybe there's a better way
        }

        registrationTasks = registrationTasks.thenApplyAsync((flag) -> {
            if (flag==false) {
                System.out.println("Downstream registration failed");
                return false;
            }
            boolean out;
            if (reg.isManual()) {
                //current.setText("Lock (Manual)");
                synchronized (MultiSlicePositioner.manualActionLock) {
                    //current.setText("Current");
                    reg.setFixedImage(preprocessFixed.apply(mp.slicedSources));
                    reg.setMovingImage(preprocessMoving.apply(registered_sacs));
                    out = reg.register();
                    if (!out) {
                        //components.remove(current);
                        //demoReportingPanel.remove(current);
                        //current.setText("Canceled");
                    } else {
                        SourceAndConverterServices.getSourceAndConverterDisplayService()
                                .remove(mp.bdvh, registered_sacs);

                        registered_sacs = reg.getTransformedImageMovingToFixed(registered_sacs);

                        SourceAndConverterServices.getSourceAndConverterDisplayService()
                                .show(mp.bdvh, registered_sacs);

                        slicingModePositioner = new AffineTransformedSourceWrapperRegistration();

                        slicingModePositioner.setMovingImage(registered_sacs);
                        SourceAndConverterServices.getSourceAndConverterService().remove(relocated_sacs_positioning_mode);

                        relocated_sacs_positioning_mode = slicingModePositioner.getTransformedImageMovingToFixed(registered_sacs);
                        updatePosition();
                    }
                }
            } else {
                //current.setText("Current");
                reg.setFixedImage(preprocessFixed.apply(mp.slicedSources));
                reg.setMovingImage(preprocessMoving.apply(registered_sacs));
                out = reg.register();
                if (!out) {
                    //components.remove(current);
                    //demoReportingPanel.remove(current);
                    //current.setText("Canceled");
                } else {
                    SourceAndConverterServices.getSourceAndConverterDisplayService()
                            .remove(mp.bdvh, registered_sacs);

                    registered_sacs = reg.getTransformedImageMovingToFixed(registered_sacs);

                    SourceAndConverterServices.getSourceAndConverterDisplayService()
                            .show(mp.bdvh, registered_sacs);

                    slicingModePositioner = new AffineTransformedSourceWrapperRegistration();

                    slicingModePositioner.setMovingImage(registered_sacs);
                    SourceAndConverterServices.getSourceAndConverterService().remove(relocated_sacs_positioning_mode);

                    relocated_sacs_positioning_mode = slicingModePositioner.getTransformedImageMovingToFixed(registered_sacs);
                    updatePosition();
                }
            }
            if (out) {
                //current.setText("Done");
                registrations.add(reg);
            } else {
                //components.remove(current);
                //demoReportingPanel.remove(current);
            }
            System.out.println("Registration done");
            //demoFrame.repaint();
            return out;
        });

        registrationTasks.handle((result, exception) -> {
            System.out.println(result);
            if (result == false) {
                System.out.println("Registration task failed");
            }
            System.out.println(exception);
            return exception;
        });
    }

    protected void cancelRegistration(Registration reg) {

    }

}