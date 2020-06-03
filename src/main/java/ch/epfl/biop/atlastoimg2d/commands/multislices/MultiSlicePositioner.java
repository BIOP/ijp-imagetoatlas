package ch.epfl.biop.atlastoimg2d.commands.multislices;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.allen.adultmousebrain.AllenBrainAdultMouseAtlasCCF2017;
import ch.epfl.biop.atlastoimg2d.AtlasToSourceAndConverter2D;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import ch.epfl.biop.scijava.command.Elastix2DAffineRegisterCommand;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Tile;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.behaviour.ClickBehaviourInstaller;
import sc.fiji.bdvpg.behaviour.EditorBehaviourInstaller;
import sc.fiji.bdvpg.behaviour.EditorBehaviourUnInstaller;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceAffineTransformer;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * All specific functions and method dedicated to the multislice positioner
 *
 * Let's think a bit:
 * There will be a positioning mode and a registration mode
 * In the registration mode, all slices are displayed, and z step equals 1, and source are overlayed
 *
 */

public class MultiSlicePositioner extends BdvOverlay {

    /**
     * BdvHandle displaying everything
     */
    final BdvHandle bdvh;

    final SourceSelectorBehaviour ssb;

    /**
     * Controller of the number of steps displayed
     */
    final MultiSlicePositioner.ZStepSetter zStepSetter;

    /**
     * The slicing model
     */
    final SourceAndConverter slicingModel;

    /**
     * Slicing Model Properties
     */
    int nPixX, nPixY, nPixZ;
    double sX, sY, sZ;
    double sizePixX, sizePixY, sizePixZ;

    List<SliceSources> slices = new ArrayList<>();

    /**
     * Keep track of already contained sources to avoid duplicates
     */
    Set<SourceAndConverter> containedSources = new HashSet<>();

    int totalNumberOfActionsRecorded = 30; // TODO : Implement
    List<CancelableAction> userActions = new ArrayList<>();

   // boolean avoidOverlap = true;

    /**
     * Current coordinate where Sources are dragged
     */
    int iSliceNoStep;

    /**
     * Shift in Y : control overlay or not of sources
     * @param bdvh
     * @param slicingModel
     */

    SourceAndConverter[] slicedSources;

    public MultiSlicePositioner(BdvHandle bdvh, SourceAndConverter slicingModel, SourceAndConverter[] slicedSources) {
        this.slicedSources = slicedSources;
        this.bdvh = bdvh;
        this.slicingModel = slicingModel;
        zStepSetter = new ZStepSetter();
        this.bdvh.getViewerPanel().setTransferHandler(new MultiSlicePositioner.TransferHandler());

        nPixX = (int) slicingModel.getSpimSource().getSource(0,0).dimension(0);
        nPixY = (int) slicingModel.getSpimSource().getSource(0,0).dimension(1);
        nPixZ = (int) slicingModel.getSpimSource().getSource(0,0).dimension(2);

        AffineTransform3D at3D = new AffineTransform3D();
        slicingModel.getSpimSource().getSourceTransform(0,0,at3D);

        double[] m = at3D.getRowPackedCopy();

        sizePixX = Math.sqrt(m[0]*m[0]+m[4]*m[4]+m[8]*m[8]);
        sizePixY = Math.sqrt(m[1]*m[1]+m[5]*m[5]+m[9]*m[9]);
        sizePixZ = Math.sqrt(m[2]*m[2]+m[6]*m[6]+m[10]*m[10]);

        sX = nPixX*sizePixX;
        sY = nPixY*sizePixY;
        sZ = nPixZ*sizePixZ;

        BdvFunctions.showOverlay( this, "MultiSlice Overlay", BdvOptions.options().addTo( bdvh ) );


        Behaviours navigation = new Behaviours(new InputTriggerConfig(), "bdv");

        ssb = (SourceSelectorBehaviour) SourceAndConverterServices.getSourceAndConverterDisplayService().getDisplayMetadata(
                bdvh, SourceSelectorBehaviour.class.getSimpleName());
        new EditorBehaviourUnInstaller(bdvh).run();

        new ClickBehaviourInstaller(bdvh, (x,y) -> this.cancelLastAction()).install("cancel_last_action", "ctrl Z");
        new ClickBehaviourInstaller(bdvh, (x,y) -> this.toggleOverlap()).install("toggle_superimpose", "O");
        new ClickBehaviourInstaller(bdvh, (x,y) -> this.elastixRegister()).install("register_test", "R");
        new ClickBehaviourInstaller(bdvh, (x,y) -> this.navigateNextSlice()).install("navigate_next_slice", "N");
        new ClickBehaviourInstaller(bdvh, (x,y) -> this.navigatePreviousSlice()).install("navigate_previous_slice", "P");

    }

    int iCurrentSlice = 0;

    public void navigateNextSlice() {
        iCurrentSlice++;
        List<SliceSources> sortedSlices = new ArrayList<>(slices);
        Collections.sort(sortedSlices, Comparator.comparingDouble(s -> s.slicingAxisPosition));

        if (iCurrentSlice>= sortedSlices.size()) {
            iCurrentSlice = 0;
        }

        if (sortedSlices.size()>0) {
            centerBdvViewOn(sortedSlices.get(iCurrentSlice));
        }

    }

    public void navigatePreviousSlice() {
        iCurrentSlice--;
        List<SliceSources> sortedSlices = new ArrayList<>(slices);
        Collections.sort(sortedSlices, Comparator.comparingDouble(s -> s.slicingAxisPosition));

        if (iCurrentSlice< 0) {
            iCurrentSlice = sortedSlices.size()-1;
        }

        if (sortedSlices.size()>0) {
            centerBdvViewOn(sortedSlices.get(iCurrentSlice));
        }
    }

    public void centerBdvViewOn(SliceSources slice) {

        RealPoint centerSlice = getSourceAndConverterCenterPoint(slice.relocated_sacs[0]);

        AffineTransform3D nextAffineTransform = new AffineTransform3D();

        // It should have the same scaling and rotation than the current view
        AffineTransform3D at3D = new AffineTransform3D();
        bdvh.getViewerPanel().state().getViewerTransform(at3D);
        nextAffineTransform.set(at3D);

        // No Shift
        nextAffineTransform.set(0, 0, 3);
        nextAffineTransform.set(0, 1, 3);
        nextAffineTransform.set(0, 2, 3);

        double next_wcx = bdvh.getViewerPanel().getWidth() / 2.0; // Next Window Center X
        double next_wcy = bdvh.getViewerPanel().getHeight() / 2.0; // Next Window Center Y

        RealPoint centerScreenNextBdv = new RealPoint(new double[]{next_wcx, next_wcy, 0});
        RealPoint shiftNextBdv = new RealPoint(3);

        nextAffineTransform.inverse().apply(centerScreenNextBdv, shiftNextBdv);

        double sx = -centerSlice.getDoublePosition(0) + shiftNextBdv.getDoublePosition(0);
        double sy = -centerSlice.getDoublePosition(1) + shiftNextBdv.getDoublePosition(1);
        double sz = -centerSlice.getDoublePosition(2) + shiftNextBdv.getDoublePosition(2);

        RealPoint shiftWindow = new RealPoint(new double[]{sx, sy, sz});
        RealPoint shiftMatrix = new RealPoint(3);
        nextAffineTransform.apply(shiftWindow, shiftMatrix);

        nextAffineTransform.set(shiftMatrix.getDoublePosition(0), 0, 3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(1), 1, 3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(2), 2, 3);

        bdvh.getViewerPanel().setCurrentViewerTransform(nextAffineTransform);
        bdvh.getViewerPanel().requestRepaint();

    }

    public RealPoint getSourceAndConverterCenterPoint(SourceAndConverter source) {
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.identity();
        //double[] m = at3D.getRowPackedCopy();
        source.getSpimSource().getSourceTransform(0,0,at3D);
        long[] dims = new long[3];
        source.getSpimSource().getSource(0,0).dimensions(dims);

        RealPoint ptCenterGlobal = new RealPoint(3);
        RealPoint ptCenterPixel = new RealPoint((dims[0]-1.0)/2.0,(dims[1]-1.0)/2.0, (dims[2]-1.0)/2.0);

        at3D.apply(ptCenterPixel, ptCenterGlobal);

        return ptCenterGlobal;
    }

    public RealPoint getSourceAndConverterTopLeftPoint(SourceAndConverter source) {
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.identity();
        //double[] m = at3D.getRowPackedCopy();
        source.getSpimSource().getSourceTransform(0,0,at3D);
        long[] dims = new long[3];
        source.getSpimSource().getSource(0,0).dimensions(dims);

        RealPoint ptCenterGlobal = new RealPoint(3);
        RealPoint ptCenterPixel = new RealPoint((dims[0]-1.0),(dims[1]-1.0), 0);

        at3D.apply(ptCenterPixel, ptCenterGlobal);

        return ptCenterGlobal;
    }

    Context scijavaCtx;
    public void setScijavaContext(Context ctx) {
        scijavaCtx = ctx;
    }

    public void updateDisplay() {
        // Sort slices along slicing axis

        if (cycleToggle==0) {
            slices.forEach(slice -> slice.yShift = 1.5);
        } else if (cycleToggle==2) {

            List<SliceSources> sortedSlices = new ArrayList<>(slices);
            Collections.sort(sortedSlices, Comparator.comparingDouble(s -> s.slicingAxisPosition));

            double lastPositionAlongX = -Double.MAX_VALUE;

            int stairIndex = 0;

            int zStep = (int) zStepSetter.getStep();

            for (SliceSources slice : sortedSlices) {
                double posX = ((slice.slicingAxisPosition/sizePixX/zStep)+0.5) * sX;
                if (posX>=(lastPositionAlongX+sX)) {
                    stairIndex = 0;
                    lastPositionAlongX = posX;
                    slice.yShift = 1.5;
                } else {
                    stairIndex++;
                    slice.yShift = 1.5+stairIndex;
                }
            }
        } else if (cycleToggle==1) {
            slices.forEach(slice -> slice.yShift = 0.5);
        }

        for (SliceSources src : slices) {
            src.updatePosition();
        }

        bdvh.getViewerPanel().requestRepaint();
    }

    int cycleToggle = 0;
    public void toggleOverlap() {
        cycleToggle++;
        if (cycleToggle==3) cycleToggle = 0;
        updateDisplay();
    }

    public ZStepSetter getzStepSetter() {
        return zStepSetter;
    }

    public void elastixRegister() {
        System.out.println("Elastix Registration");
        if (slices.size()==0) return;
        if (iCurrentSlice<0) iCurrentSlice = 0;
        if (iCurrentSlice>=slices.size()) iCurrentSlice = 0;

        SliceSources slice = slices.get(this.iCurrentSlice);
        RealPoint rpt = new RealPoint(3);
        double posX = ((slice.slicingAxisPosition/sizePixX/zStepSetter.getStep())) * sX;
        double posY = 0;
        rpt.setPosition(posX,0);
        rpt.setPosition(posY,1);

        Future<CommandModule> task = scijavaCtx.getService(CommandService.class).run(Elastix2DAffineRegisterCommand.class, true,
                "sac_fixed", slicedSources[0].getSpimSource().getNumMipmapLevels()-1,
                    "tpFixed", 0,
                    "levelFixedSource", 2,
                    "sac_moving", slice.relocated_sacs[0],
                    "tpMoving", 0,
                    "levelMovingSource", slice.relocated_sacs[0].getSpimSource().getNumMipmapLevels()-1,
                    "pxSizeInCurrentUnit", 0.04, // in mm
                    "interpolate", false,
                    "showImagePlusRegistrationResult", true,
                    "px",rpt.getDoublePosition(0),
                    "py",rpt.getDoublePosition(1),
                    "pz",rpt.getDoublePosition(2),
                    "sx",sX,
                    "sy",sY
                );
    }

    @Override
    protected void draw(Graphics2D g) {
        int colorCode = this.info.getColor().get();
        g.setColor(new Color(ARGBType.red(colorCode) , ARGBType.green(colorCode), ARGBType.blue(colorCode), ARGBType.alpha(colorCode) ));

        RealPoint[][] ptRectWorld = new RealPoint[2][2];
        Point[][] ptRectScreen = new Point[2][2];

        AffineTransform3D bdvAt3D = new AffineTransform3D();

        bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);

        for (int xp = 0; xp < 2; xp++) {
            for (int yp = 0; yp < 2; yp++) {
                ptRectWorld[xp][yp] = new RealPoint(3);
                RealPoint pt = ptRectWorld[xp][yp];
                pt.setPosition(sX * (iSliceNoStep + xp), 0);
                pt.setPosition(sY * (1 + yp), 1);
                pt.setPosition(0, 2);
                bdvAt3D.apply(pt, pt);
                ptRectScreen[xp][yp] = new Point((int) pt.getDoublePosition(0), (int) pt.getDoublePosition(1));
            }
        }

        g.drawLine(ptRectScreen[0][0].x, ptRectScreen[0][0].y, ptRectScreen[1][0].x, ptRectScreen[1][0].y);
        g.drawLine(ptRectScreen[1][0].x, ptRectScreen[1][0].y, ptRectScreen[1][1].x, ptRectScreen[1][1].y);
        g.drawLine(ptRectScreen[1][1].x, ptRectScreen[1][1].y, ptRectScreen[0][1].x, ptRectScreen[0][1].y);
        g.drawLine(ptRectScreen[0][1].x, ptRectScreen[0][1].y, ptRectScreen[0][0].x, ptRectScreen[0][0].y);

    }

    public void cancelLastAction() {
        if (userActions.size()>0) {
            userActions.get(userActions.size()-1).cancel();
        }
    }

    public void createSlice(SourceAndConverter[] sacs, double slicingAxisPosition) {
        new CreateSlice(Arrays.asList(sacs), slicingAxisPosition).run();
    }

    /**
     * TransferHandler class :
     * Controls drag and drop actions in the multislice positioner
     */
    class TransferHandler extends BdvTransferHandler {

        @Override
        public void updateDropLocation(TransferSupport support, DropLocation dl) {

            // Gets the point in real coordinates
            RealPoint pt3d = new RealPoint(3);
            bdvh.getViewerPanel().displayToGlobalCoordinates(dl.getDropPoint().x, dl.getDropPoint().y, pt3d);

            // Computes which slice it corresponds to (useful for overlay redraw)
            iSliceNoStep = (int) (pt3d.getDoublePosition(0)/sX);

            //Repaint the overlay only
            bdvh.getViewerPanel().paint();
        }

        /**
         * When the user drops the data -> import the slices
         * @param support
         * @param sacs
         */
        @Override
        public void importSourcesAndConverters(TransferSupport support, List<SourceAndConverter<?>> sacs) {
            Optional<BdvHandle> bdvh = getBdvHandleFromViewerPanel(((bdv.viewer.ViewerPanel)support.getComponent()));
            if (bdvh.isPresent()) {
                double slicingAxisPosition = iSliceNoStep*sizePixX*(int) zStepSetter.getStep();
                //System.out.println("Slice dropped at "+slicingAxisPosition);
                if (sacs.size()>1) {
                    // Check whether the source can be splitted, maybe based
                    // Split based on Tile ?


                    Map<Tile, List<SourceAndConverter<?>>> sacsGroups =
                    sacs.stream().collect(Collectors.groupingBy(sac -> {
                        if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac, SPIM_DATA_INFO)!=null) {
                            SourceAndConverterService.SpimDataInfo sdi = (SourceAndConverterService.SpimDataInfo) SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac, SPIM_DATA_INFO);
                            AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>> asd = ( AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>>)sdi.asd;
                            BasicViewSetup bvs = asd.getSequenceDescription().getViewSetups().get(sdi.setupId);
                            return bvs.getAttribute(Tile.class);
                        } else {
                            return new Tile(-1);
                        }
                    }));

                    List<Tile> sortedTiles = new ArrayList<>();

                    sortedTiles.addAll(sacsGroups.keySet());

                    sortedTiles.sort(Tile::compareTo);

                    sortedTiles.forEach(tile -> {
                        new CreateSlice(sacsGroups.get(tile), slicingAxisPosition).run();
                    });

                } else {
                    new CreateSlice(sacs, slicingAxisPosition).run();
                }
            }
        }
    }

    /**
     * Simple to enable setting z step and allowing its communication
     * in multiple Commands
     */
    public class ZStepSetter {

        private int zStep = 1;

        public void setStep(int zStep) {
            if (zStep>0) {
                this.zStep = zStep;
            }
            updateDisplay();
        }

        public long getStep() {
            return (long) zStep;
        }
    }

    /**
     * Inner class which contains the information necessary for the viewing of the slices:
     *
     */
    private class SliceSources {
        // What are they ?
        SourceAndConverter[] original_sacs;

        SourceAndConverter[] relocated_sacs;

        // Where are they ?
        double slicingAxisPosition;

        AffineTransform3D at3D;

        boolean isSelected = false;

        double yShift = 0;

        AtlasToSourceAndConverter2D aligner;

        // For fast display : Icon TODO : see https://github.com/bigdataviewer/bigdataviewer-core/blob/17d2f55d46213d1e2369ad7ef4464e3efecbd70a/src/main/java/bdv/tools/RecordMovieDialog.java#L256-L318
        public SliceSources(SourceAndConverter[] sacs, double slicingAxisPosition) {

            this.original_sacs = sacs;
            List<SourceAndConverter<?>> sacsTransformed = new ArrayList<>();
            at3D = new AffineTransform3D();
            for (SourceAndConverter sac : sacs) {
                RealPoint center = new RealPoint(3);
                center.setPosition(new double[] {0,0,0}); // Center
                SourceAndConverter zeroCenteredSource = recenterSourcesAppend(sac, center);
                sacsTransformed.add(new SourceAffineTransformer(zeroCenteredSource, at3D).getSourceOut());
            }

            this.relocated_sacs = sacsTransformed.toArray(new SourceAndConverter[sacsTransformed.size()]);
            this.slicingAxisPosition = slicingAxisPosition;

            aligner = new AtlasToSourceAndConverter2D();
            aligner.setScijavaContext(scijavaCtx);
            aligner.setImage(sacs);

        }

        public boolean exactMatch(List<SourceAndConverter<?>> testSacs) {
            Set originalSacsSet = new HashSet();
            for (SourceAndConverter sac : original_sacs) {
                originalSacsSet.add(sac);
            }
            if (originalSacsSet.containsAll(testSacs) && testSacs.containsAll(originalSacsSet)) {
                return true;
            }
            Set transformedSacsSet = new HashSet();
            for (SourceAndConverter sac : relocated_sacs) {
                transformedSacsSet.add(sac);
            }
            if (transformedSacsSet.containsAll(testSacs) && testSacs.containsAll(transformedSacsSet)) {
                return true;
            }

            return false;
        }

        public void updatePosition() {
            double posX = ((slicingAxisPosition/sizePixX/zStepSetter.getStep())+0.5) * sX;
            double posY = sY * yShift;

            AffineTransform3D new_at3D = new AffineTransform3D();
            new_at3D.set(posX,0,3);
            new_at3D.set(posY,1,3);

            for (SourceAndConverter sac : relocated_sacs) {
                assert sac.getSpimSource() instanceof TransformedSource;
                ((TransformedSource)sac.getSpimSource()).setFixedTransform(new_at3D);
            }
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

    public abstract class CancelableAction {

        public void run() {
            userActions.add(this);
        }

        public void cancel() {
            if (userActions.get(userActions.size()-1).equals(this)) {
                userActions.remove(userActions.size()-1);
            } else {
                System.err.println("Error : cancel not called on the last action");
                return;
            }
        }
    }

    public class MoveSlice extends CancelableAction {

        private SliceSources sliceSource;
        private double oldSlicingAxisPosition;
        private double newSlicingAxisPosition;

        public MoveSlice(SliceSources sliceSource, double slicingAxisPosition ) {
            this.sliceSource = sliceSource;
            this.oldSlicingAxisPosition = sliceSource.slicingAxisPosition;
            this.newSlicingAxisPosition = slicingAxisPosition;
        }

        public void run() {
            sliceSource.slicingAxisPosition = newSlicingAxisPosition;
            sliceSource.updatePosition();
            updateDisplay();
            super.run();
        }

        public void cancel() {
            sliceSource.slicingAxisPosition = oldSlicingAxisPosition;
            sliceSource.updatePosition();
            updateDisplay();
            super.cancel();
        }
    }

    public class CreateSlice extends CancelableAction {

        private List<SourceAndConverter<?>> sacs;
        private SliceSources sliceSource;
        private double slicingAxisPosition;

        public CreateSlice(List<SourceAndConverter<?>> sacs, double slicingAxisPosition) {
            this.sacs = sacs;
            this.slicingAxisPosition = slicingAxisPosition;
        }

        @Override
        public void run() {
            System.out.println("Create Slice run");
            boolean sacAlreadyPresent = false;
            for (SourceAndConverter sac : sacs) {
                if (containedSources.contains(sac)) {
                    sacAlreadyPresent = true;
                }
            }

            if (sacAlreadyPresent) {
                SliceSources zeSlice = null;

                // A source is already included :
                // If all sources match exactly what's in a single SliceSources -> that's a move operation

                boolean exactMatch = false;
                for (SliceSources ss : slices) {
                    if (ss.exactMatch(sacs)) {
                        exactMatch = true;
                        zeSlice = ss;
                    }
                }

                if (!exactMatch) {
                    System.err.println("A source is already used in the positioner : action ignored");
                    return;
                } else {
                    System.out.println("Movind Source");
                    // Move action:
                    new MoveSlice(zeSlice, slicingAxisPosition).run();
                    return;
                }
            }

            sliceSource = new SliceSources(sacs.toArray(new SourceAndConverter[sacs.size()]), slicingAxisPosition);

            slices.add(sliceSource);

            containedSources.addAll(Arrays.asList(sliceSource.original_sacs));
            containedSources.addAll(Arrays.asList(sliceSource.relocated_sacs));

            updateDisplay();

            SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .show(bdvh, sliceSource.relocated_sacs);

            // The line below should be executed only if the action succeeded ... (if it's executed, calling cancel should have the same effect)
            super.run();
        }

        @Override
        public void cancel() {
            containedSources.removeAll(Arrays.asList(sliceSource.original_sacs));
            containedSources.removeAll(Arrays.asList(sliceSource.relocated_sacs));
            slices.remove(sliceSource);
            SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .remove(bdvh, sliceSource.relocated_sacs);
            super.cancel();
        }
    }


}