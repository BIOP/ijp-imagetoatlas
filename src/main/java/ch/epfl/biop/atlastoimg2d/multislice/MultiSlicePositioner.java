package ch.epfl.biop.atlastoimg2d.multislice;

import bdv.tools.transformation.TransformedSource;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices.SacMultiSacsPositionerCommand;
import ch.epfl.biop.bdv.select.SelectedSourcesListener;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.sourceandconverter.Elastix2DAffineRegistration;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Tile;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import org.scijava.Context;
import org.scijava.ui.behaviour.*;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.behaviour.EditorBehaviourUnInstaller;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * All specific functions and method dedicated to the multislice positioner
 *
 * Let's think a bit:
 * There will be:
 * - a positioning mode
 * - a registration mode
 * - a 3d view mode
 * - an export mode
 * In the registration mode, all slices are displayed, and z step equals 1, and source are overlayed
 *
 */

public class MultiSlicePositioner extends BdvOverlay implements SelectedSourcesListener, GraphicalHandleListener, MouseMotionListener {

    //
    static public Object manualActionLock = new Object();


    /**
     * BdvHandle displaying everything
     */
    final BdvHandle bdvh;

    final SourceSelectorBehaviour ssb;

    /**
     * Controller of the number of steps displayed
     */
    //final SlicerSetter zStepSetter;

    /**
     * The slicing model
     */
    //final SourceAndConverter slicingModel;

    /**
     * Slicing Model Properties
     */
    int nPixX, nPixY, nPixZ;
    double sX, sY, sZ;
    double sizePixX, sizePixY, sizePixZ;

    List<SliceSources> slices = new ArrayList<>();

    int totalNumberOfActionsRecorded = 30; // TODO : Implement
    List<CancelableAction> userActions = new ArrayList<>();

    List<CancelableAction> redoableUserActions = new ArrayList<>();

    /**
     * Current coordinate where Sources are dragged
     */
    int iSliceNoStep;


    Context scijavaCtx;

    /**
     * Shift in Y : control overlay or not of sources
     * @param bdvh
     * @param slicingModel
     */

    SourceAndConverter[] extendedSlicedSources;

    public String currentMode = "";

    final static String POSITIONING_MODE = "positioning-mode";
    final static String POSITIONING_BEHAVIOURS_KEY = POSITIONING_MODE+"-behaviours";
    Behaviours positioning_behaviours = new Behaviours(new InputTriggerConfig(), POSITIONING_MODE);

    final static String REGISTRATION_MODE = "Registration";
    final static String REGISTRATION_BEHAVIOURS_KEY = REGISTRATION_MODE+"-behaviours";
    Behaviours registration_behaviours = new Behaviours(new InputTriggerConfig(), REGISTRATION_MODE);

    final static String COMMON_BEHAVIOURS_KEY = "multipositioner-behaviours";
    Behaviours common_behaviours = new Behaviours(new InputTriggerConfig(), "multipositioner" );

    private static final String BLOCKING_MAP = "multipositioner-blocking";

    int iCurrentSlice = 0;

    Integer[] rightPosition = new Integer[]{0,0,0};

    Integer[] leftPosition = new Integer[]{0,0,0};

    ReslicedAtlas reslicedAtlas;

    BiopAtlas biopAtlas;

    public MultiSlicePositioner(BdvHandle bdvh, BiopAtlas biopAtlas, ReslicedAtlas reslicedAtlas, Context ctx) {
        this.reslicedAtlas = reslicedAtlas;
        this.biopAtlas = biopAtlas;
        this.extendedSlicedSources = reslicedAtlas.extendedSlicedSources;//licedSources;

        this.bdvh = bdvh;
        this.scijavaCtx = ctx;

        iSliceNoStep = (int) (reslicedAtlas.getStep());

        this.bdvh.getViewerPanel().setTransferHandler(new MultiSlicePositioner.TransferHandler());

        nPixX = (int) reslicedAtlas.slicingModel.getSpimSource().getSource(0,0).dimension(0);
        nPixY = (int) reslicedAtlas.slicingModel.getSpimSource().getSource(0,0).dimension(1);
        nPixZ = (int) reslicedAtlas.slicingModel.getSpimSource().getSource(0,0).dimension(2);

        AffineTransform3D at3D = new AffineTransform3D();
        reslicedAtlas.slicingModel.getSpimSource().getSourceTransform(0,0,at3D);

        double[] m = at3D.getRowPackedCopy();

        sizePixX = Math.sqrt(m[0]*m[0]+m[4]*m[4]+m[8]*m[8]);
        sizePixY = Math.sqrt(m[1]*m[1]+m[5]*m[5]+m[9]*m[9]);
        sizePixZ = Math.sqrt(m[2]*m[2]+m[6]*m[6]+m[10]*m[10]);

        sX = nPixX*sizePixX;
        sY = nPixY*sizePixY;
        sZ = nPixZ*sizePixZ;

        BdvFunctions.showOverlay( this, "MultiSlice Overlay", BdvOptions.options().addTo( bdvh ) );

        ssb = (SourceSelectorBehaviour) SourceAndConverterServices.getSourceAndConverterDisplayService().getDisplayMetadata(
                bdvh, SourceSelectorBehaviour.class.getSimpleName());
        new EditorBehaviourUnInstaller(bdvh).run();

        // Disable edit mode by default
        bdvh.getTriggerbindings().removeInputTriggerMap(SourceSelectorBehaviour.SOURCES_SELECTOR_TOGGLE_MAP);

        setPositioningMode();

        common_behaviours.behaviour((ClickBehaviour)(x, y) -> this.cancelLastAction(), "cancel_last_action", "ctrl Z");
        common_behaviours.behaviour((ClickBehaviour)(x, y) -> this.redoAction(), "redo_last_action", "ctrl Y", "ctrl shift Z");
        common_behaviours.behaviour((ClickBehaviour)(x,y) -> this.navigateNextSlice(), "navigate_next_slice", "N");
        common_behaviours.behaviour((ClickBehaviour)(x,y) -> this.navigatePreviousSlice(), "navigate_previous_slice", "P"); // P taken for panel
        common_behaviours.behaviour((ClickBehaviour)(x,y) -> this.navigateCurrentSlice(), "navigate_current_slice", "C");
        common_behaviours.behaviour((ClickBehaviour)(x,y) -> this.nextMode(), "change_mode", "Q");
        common_behaviours.install(bdvh.getTriggerbindings(), COMMON_BEHAVIOURS_KEY);

        positioning_behaviours.behaviour((ClickBehaviour)(x,y) ->  this.toggleOverlap(), "toggle_superimpose", "O");
        positioning_behaviours.behaviour((ClickBehaviour)(x,y) ->  {if (ssb.isEnabled()) {
            ssb.disable();
            refreshBlockMap();
        } else {
            ssb.enable();
            refreshBlockMap();
        }}, "toggle_editormode", "E");
        positioning_behaviours.behaviour((ClickBehaviour)(x,y) ->  this.equalSpacingSelectedSlices(), "equalSpacingSelectedSlices", "A");
        positioning_behaviours.behaviour((ClickBehaviour)(x,y) ->  slices.forEach(slice -> slice.isSelected = true), "selectAllSlices", "ctrl A");

        ssb.addSelectedSourcesListener(this);

        registration_behaviours.behaviour((ClickBehaviour)(x,y) ->  this.elastixRegister(), "register_test", "R");

        List<SourceAndConverter<?>> sacsToAppend = new ArrayList<>();
        for (int i=0;i<biopAtlas.map.getStructuralImages().length;i++) {
            sacsToAppend.add(biopAtlas.map.getStructuralImages()[i]);
            sacsToAppend.add(extendedSlicedSources[i]);
        }

        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .show(bdvh, sacsToAppend.toArray(new SourceAndConverter[0]));

        setPositioningMode();
        bdvh.getViewerPanel().getDisplay().addHandler(this);

        GraphicalHandle ghRight = new CircleGraphicalHandle(this,  new DragRight(), "drag_right", "button1",bdvh.getTriggerbindings(),
                () -> rightPosition, () -> 50, () -> new Integer[]{255,0,255, 200});
        GraphicalHandle ghLeft = new CircleGraphicalHandle(this,  new DragLeft(), "drag_right", "button1",bdvh.getTriggerbindings(),
                () -> leftPosition, () -> 50, () -> new Integer[]{255,0,255,200});

        ghs.add(ghRight);
        ghs.add(ghLeft);

        if (this.bdvh.getCardPanel()!=null) {
            this.bdvh.getCardPanel().addCard( "Registration Options", new RegistrationPanel(this).getPanel(), true);
            this.bdvh.getCardPanel().setCardExpanded("Sources", false);
            this.bdvh.getCardPanel().setCardExpanded("Groups", false);
        } else {
            System.err.println("this.bdvh.getCardPanel() is null!");
        }

        reslicedAtlas.addListener(() -> {
            recenterBdvh();
            updateDisplay();
        });

        previouszStep = (int) reslicedAtlas.getStep();
    }

    int previouszStep;

    public void recenterBdvh() {
        double cur_wcx = bdvh.getViewerPanel().getWidth()/2.0; // Current Window Center X
        double cur_wcy = bdvh.getViewerPanel().getHeight()/2.0; // Current Window Center Y

        RealPoint centerScreenCurrentBdv = new RealPoint(new double[]{cur_wcx, cur_wcy, 0});
        RealPoint centerScreenGlobalCoord = new RealPoint(3);

        AffineTransform3D at3D = new AffineTransform3D();
        bdvh.getBdvHandle().getViewerPanel().state().getViewerTransform(at3D);

        at3D.inverse().apply(centerScreenCurrentBdv, centerScreenGlobalCoord);

        // New target
        centerScreenGlobalCoord.setPosition( centerScreenGlobalCoord.getDoublePosition(0)*(double)previouszStep/(double) reslicedAtlas.getStep(), 0);

        // How should we translate at3D, such as the screen center is the new one

        // Now compute what should be the matrix in the next bdv frame:
        AffineTransform3D nextAffineTransform = new AffineTransform3D();

        // It should have the same scaling and rotation than the current view
        nextAffineTransform.set(at3D);

        // No Shift
        nextAffineTransform.set(0,0,3);
        nextAffineTransform.set(0,1,3);
        nextAffineTransform.set(0,2,3);

        // But the center of the window should be centerScreenGlobalCoord
        // Let's compute the shift
        double next_wcx = bdvh.getViewerPanel().getWidth()/2.0; // Next Window Center X
        double next_wcy = bdvh.getViewerPanel().getHeight()/2.0; // Next Window Center Y

        RealPoint centerScreenNextBdv = new RealPoint(new double[]{next_wcx, next_wcy, 0});
        RealPoint shiftNextBdv = new RealPoint(3);

        nextAffineTransform.inverse().apply(centerScreenNextBdv, shiftNextBdv);

        double sx = -centerScreenGlobalCoord.getDoublePosition(0)+shiftNextBdv.getDoublePosition(0);
        double sy = -centerScreenGlobalCoord.getDoublePosition(1)+shiftNextBdv.getDoublePosition(1);
        double sz = -centerScreenGlobalCoord.getDoublePosition(2)+shiftNextBdv.getDoublePosition(2);

        RealPoint shiftWindow = new RealPoint(new double[]{sx, sy, sz});
        RealPoint shiftMatrix = new RealPoint(3);
        nextAffineTransform.apply(shiftWindow, shiftMatrix);

        nextAffineTransform.set(shiftMatrix.getDoublePosition(0),0,3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(1),1,3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(2),2,3);

        bdvh.getBdvHandle().getViewerPanel().setCurrentViewerTransform(nextAffineTransform);
        previouszStep = (int) reslicedAtlas.getStep();
    }

    /**
     * Gets all slices sorted along the slicing axis
     * @return
     */
    public List<SliceSources> getSortedSlices() {
        List<SliceSources> sortedSlices = new ArrayList<>(slices);
        Collections.sort(sortedSlices, Comparator.comparingDouble(s -> s.slicingAxisPosition));
        return sortedSlices;
    }

    // --------------------------------------------------------- SETTING MODES

    /**
     * Toggles between positioning and registration mode
     */
    public void nextMode() {
        switch (currentMode) {
            case POSITIONING_MODE:
                this.setRegistrationMode();
                break;
            case REGISTRATION_MODE:
                this.setPositioningMode();
                break;
        }
    }

    int zStepStored; // zStep is set to 1 when in registration mode

    /**
     * Set the positioning mode
     */
    public void setPositioningMode() {
        if (slices.stream().noneMatch(slice -> slice.processInProgress)) {
            if (!currentMode.equals(POSITIONING_MODE)) {
                synchronized (slices) {
                    reslicedAtlas.unlock();
                    currentMode = POSITIONING_MODE;
                    reslicedAtlas.setStep(this.zStepStored);
                    ghs.forEach(gh -> gh.enable());
                    slices.forEach(slice -> slice.enableGraphicalHandles());
                    List<SourceAndConverter> sacsToRemove = new ArrayList<>();
                    List<SourceAndConverter> sacsToAdd = new ArrayList<>();
                    getSortedSlices().forEach(ss -> {
                        // sacsToRemove.addAll(Arrays.asList(ss.relocated_sacs_3D_mode));
                        sacsToRemove.addAll(Arrays.asList(ss.registered_sacs));
                        sacsToAdd.addAll(Arrays.asList(ss.relocated_sacs_positioning_mode));
                    });
                    SourceAndConverterServices
                            .getSourceAndConverterDisplayService()
                            .remove(bdvh, sacsToRemove.toArray(new SourceAndConverter[0]));
                    SourceAndConverterServices
                            .getSourceAndConverterDisplayService()
                            .show(bdvh, sacsToAdd.toArray(new SourceAndConverter[0]));
                }

                List<SourceAndConverter<?>> sacToShow = new ArrayList<>();
                List<SourceAndConverter<?>> sacToHide = new ArrayList<>();
                for (int i=0;i<biopAtlas.map.getStructuralImages().length;i++) {
                    SourceAndConverter sac = biopAtlas.map.getStructuralImages()[i];
                    if (bdvh.getViewerPanel().state().isSourceVisible(sac)) {
                        sacToHide.add(sac);
                        sacToShow.add(extendedSlicedSources[i]);
                    }
                }

                bdvh.getViewerPanel().state().setSourcesActive(sacToHide, false);
                bdvh.getViewerPanel().state().setSourcesActive(sacToShow, true);

                /*bdvh.getViewerPanel().state()
                        .setSourcesActive(Arrays.asList(extendedSlicedSources));


                /*SourceAndConverterServices
                        .getSourceAndConverterDisplayService()
                        .remove(bdvh, nonextendedSlicedSources);*/

                /*SourceAndConverterServices
                        .getSourceAndConverterDisplayService()
                        .show(bdvh, extendedSlicedSources);*/

                bdvh.getTriggerbindings().removeInputTriggerMap(REGISTRATION_BEHAVIOURS_KEY);
                bdvh.getTriggerbindings().removeBehaviourMap(REGISTRATION_BEHAVIOURS_KEY);
                positioning_behaviours.install(bdvh.getTriggerbindings(), POSITIONING_BEHAVIOURS_KEY);
                navigateCurrentSlice();
                refreshBlockMap();
            }
        } else {
            bdvh.getViewerPanel().showMessage("Registration in progress : cannot switch to positioning mode.");
        }
    }

    /**
     * Set the registration mode
     */
    public void setRegistrationMode() {
        if (!currentMode.equals(REGISTRATION_MODE)) {
            reslicedAtlas.lock();
            currentMode = POSITIONING_MODE;
            zStepStored = (int) reslicedAtlas.getStep();
            reslicedAtlas.setStep(1);
            currentMode = REGISTRATION_MODE;

            ghs.forEach(gh -> gh.disable());
            slices.forEach(slice -> slice.disableGraphicalHandles());
            // Do stuff
            synchronized (slices) {
                if (slices.stream().anyMatch(slice -> slice.processInProgress)) {
                    System.err.println("Mode cannot be changed if a task is in process");
                } else {
                    List<SourceAndConverter> sacsToRemove = new ArrayList<>();
                    List<SourceAndConverter> sacsToAdd = new ArrayList<>();
                    getSortedSlices().forEach(ss -> {
                        //sacsToRemove.addAll(Arrays.asList(ss.relocated_sacs_3D_mode));
                        sacsToRemove.addAll(Arrays.asList(ss.relocated_sacs_positioning_mode));
                        sacsToAdd.addAll(Arrays.asList(ss.registered_sacs));
                    });
                    SourceAndConverterServices
                            .getSourceAndConverterDisplayService()
                            .remove(bdvh, sacsToRemove.toArray(new SourceAndConverter[0]));
                    SourceAndConverterServices
                            .getSourceAndConverterDisplayService()
                            .show(bdvh, sacsToAdd.toArray(new SourceAndConverter[0]));
                    ;
                }
            }

            /*SourceAndConverterServices
                    .getSourceAndConverterDisplayService()
                    .remove(bdvh, extendedSlicedSources);*/


            List<SourceAndConverter<?>> sacToShow = new ArrayList<>();
            List<SourceAndConverter<?>> sacToHide = new ArrayList<>();
            for (int i=0;i<biopAtlas.map.getStructuralImages().length;i++) {
                SourceAndConverter sac = extendedSlicedSources[i];
                if (bdvh.getViewerPanel().state().isSourceVisible(sac)) {
                    sacToHide.add(sac);
                    sacToShow.add(biopAtlas.map.getStructuralImages()[i]);
                }
            }

            bdvh.getViewerPanel().state().setSourcesActive(sacToHide, false);
            bdvh.getViewerPanel().state().setSourcesActive(sacToShow, true);

            bdvh.getTriggerbindings().removeInputTriggerMap(POSITIONING_BEHAVIOURS_KEY);
            bdvh.getTriggerbindings().removeBehaviourMap(POSITIONING_BEHAVIOURS_KEY);
            registration_behaviours.install(bdvh.getTriggerbindings(), REGISTRATION_BEHAVIOURS_KEY );
            navigateCurrentSlice();
        }
        refreshBlockMap();
    }

    // -------------------------------------------------------- NAVIGATION ( BOTH MODES )
    /**
     * Center bdv on next slice (iCurrentSlice + 1)
     */
    public void navigateNextSlice() {
        iCurrentSlice++;
        List<SliceSources> sortedSlices = getSortedSlices();
        if (iCurrentSlice>= sortedSlices.size()) {
            iCurrentSlice = 0;
        }
        if (sortedSlices.size()>0) {
            centerBdvViewOn(sortedSlices.get(iCurrentSlice));
        }
    }

    /**
     * Center bdv on current slice (iCurrentSlice)
     */
    public void navigateCurrentSlice() {
        List<SliceSources> sortedSlices = getSortedSlices();

        if (iCurrentSlice>= sortedSlices.size()) {
            iCurrentSlice = 0;
        }

        if (sortedSlices.size()>0) {
            centerBdvViewOn(sortedSlices.get(iCurrentSlice));
        }
    }

    /**
     * Center bdv on previous slice (iCurrentSlice - 1)
     */
    public void navigatePreviousSlice() {
        iCurrentSlice--;
        List<SliceSources> sortedSlices = getSortedSlices();

        if (iCurrentSlice< 0) {
            iCurrentSlice = sortedSlices.size()-1;
        }

        if (sortedSlices.size()>0) {
            centerBdvViewOn(sortedSlices.get(iCurrentSlice));
        }
    }

    /**
     * Center bdv on a slice
     * @param slice
     */
    public void centerBdvViewOn(SliceSources slice) {

        RealPoint centerSlice;
        switch (currentMode) {
            case POSITIONING_MODE:
                centerSlice = SourceAndConverterUtils.getSourceAndConverterCenterPoint(slice.relocated_sacs_positioning_mode[0]);
                break;
            case REGISTRATION_MODE:
                centerSlice = SourceAndConverterUtils.getSourceAndConverterCenterPoint(slice.registered_sacs[0]);
                break;
            default:
                System.err.println("Invalid multislicer mode");
                return;
        }

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

    private void updateDisplay() {
        // Sort slices along slicing axis

        if (cycleToggle==0) {
            slices.forEach(slice -> slice.yShift_slicing_mode = 1);
        } else if (cycleToggle==2) {

            double lastPositionAlongX = -Double.MAX_VALUE;

            int stairIndex = 0;

            int zStep = (int) reslicedAtlas.getStep();

            for (SliceSources slice : getSortedSlices()) {
                double posX = ((slice.slicingAxisPosition/sizePixX/zStep)+0) * sX;
                if (posX>=(lastPositionAlongX+sX)) {
                    stairIndex = 0;
                    lastPositionAlongX = posX;
                    slice.yShift_slicing_mode = 1;
                } else {
                    stairIndex++;
                    slice.yShift_slicing_mode = 1+stairIndex;
                }
            }
        } else if (cycleToggle==1) {
            slices.forEach(slice -> slice.yShift_slicing_mode = 0);
        }

        for (SliceSources slice : slices) {
            slice.updatePosition();
        }

        bdvh.getViewerPanel().requestRepaint();
    }

    int cycleToggle = 0;

    /**
     * Overlap or not of the positioned slices
     */
    public void toggleOverlap() {
        cycleToggle++;
        if (cycleToggle==3) cycleToggle = 0;
        navigateCurrentSlice();
        updateDisplay();
    }

    public void elastixRegister() {
        for (SliceSources slice : slices) {
            if (slice.isSelected) {
                Elastix2DAffineRegistration elastixAffineReg = new Elastix2DAffineRegistration();
                elastixAffineReg.setScijavaContext(scijavaCtx);
                RealPoint rpt = new RealPoint(3);
                double posX = -sX/2;
                double posY = -sY/2;
                rpt.setPosition(posX,0);
                rpt.setPosition(posY,1);
                rpt.setPosition(slice.slicingAxisPosition, 2);
                Map<String, Object> params = new HashMap<>();
                params.put("tpFixed", 0);
                params.put("levelFixedSource", 2);
                params.put("tpMoving", 0);
                params.put("levelMovingSource", slice.registered_sacs[0].getSpimSource().getNumMipmapLevels()-1);
                params.put("pxSizeInCurrentUnit", 0.04);
                params.put("interpolate", false);
                params.put("showImagePlusRegistrationResult", false);//true);
                params.put("px",rpt.getDoublePosition(0));
                params.put("py",rpt.getDoublePosition(1));
                params.put("pz",rpt.getDoublePosition(2));
                params.put("sx",sX);
                params.put("sy",sY);
                elastixAffineReg.setScijavaParameters(params);
                new RegisterSlice(slice, elastixAffineReg,(sacs) -> new SourceAndConverter[] {sacs[1]}, (sacs) -> new SourceAndConverter[] {sacs[0]}).run();
            }
        }
    }

    @Override
    protected synchronized void draw(Graphics2D g) {
        {
            int colorCode = this.info.getColor().get();
            Color color = new Color(ARGBType.red(colorCode) , ARGBType.green(colorCode), ARGBType.blue(colorCode), ARGBType.alpha(colorCode) );
            g.setColor(color);

            g.drawString(currentMode,10,10);

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

            for (SliceSources slice : slices) {
                slice.drawGraphicalHandles(g);
            }

            g.setColor(color);

            if (currentMode.equals(POSITIONING_MODE) && slices.stream().anyMatch(slice -> slice.isSelected)) {
                List<SliceSources> sortedSelected = getSortedSlices().stream().filter(slice -> slice.isSelected).collect(Collectors.toList());
                RealPoint precedentPoint = null;

                for (int i=0;i<sortedSelected.size();i++) {
                    SliceSources slice = sortedSelected.get(i);

                    Integer[] coords = slice.getBdvHandleCoords();
                    RealPoint sliceCenter = new RealPoint(coords[0], coords[1], 0);

                    if (precedentPoint!=null) {
                        g.drawLine((int)precedentPoint.getDoublePosition(0),(int)precedentPoint.getDoublePosition(1),
                                   (int)sliceCenter.getDoublePosition(0),(int)sliceCenter.getDoublePosition(1));
                    } else {
                        precedentPoint = new RealPoint(2);
                    }

                    precedentPoint.setPosition(sliceCenter.getDoublePosition(0),0);
                    precedentPoint.setPosition(sliceCenter.getDoublePosition(1),1);

                    bdvAt3D.apply(sliceCenter, sliceCenter);

                    if (i ==0) {
                        double slicingAxisSnapped = (((int)(slice.slicingAxisPosition/sizePixX))*sizePixX);

                        double posX = ((slicingAxisSnapped/sizePixX/reslicedAtlas.getStep())) * sX;
                        double posY = sY * -1;

                        RealPoint handleLeftPoint = new RealPoint(posX, posY, 0);

                        bdvAt3D.apply(handleLeftPoint, handleLeftPoint);


                        leftPosition[0] = (int) handleLeftPoint.getDoublePosition(0);
                        leftPosition[1] = (int) handleLeftPoint.getDoublePosition(1);
                    }

                    if (i == sortedSelected.size()-1) {

                        double slicingAxisSnapped = (((int)(slice.slicingAxisPosition/sizePixX))*sizePixX);

                        double posX = ((slicingAxisSnapped/sizePixX/reslicedAtlas.getStep())) * sX;
                        double posY = sY * -1;

                        RealPoint handleRightPoint = new RealPoint(posX, posY, 0);

                        bdvAt3D.apply(handleRightPoint, handleRightPoint);

                        rightPosition[0] = (int) handleRightPoint.getDoublePosition(0);
                        rightPosition[1] = (int) handleRightPoint.getDoublePosition(1);
                    }
                }

                if (sortedSelected.size()>2) {
                    ghs.forEach(gh -> gh.enable());
                    g.setColor(new Color(255,0,255,200));
                    g.drawLine(leftPosition[0],leftPosition[1],rightPosition[0],rightPosition[1]);
                } else {
                    ghs.forEach(gh -> gh.disable());
                }
                ghs.forEach(gh -> gh.draw(g));
            }
        }
    }

    public void equalSpacingSelectedSlices() {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(slice -> slice.isSelected).collect(Collectors.toList());
        if (sortedSelected.size()>2) {
            SliceSources first = sortedSelected.get(0);
            SliceSources last = sortedSelected.get(sortedSelected.size()-1);
            double totalSpacing = last.slicingAxisPosition-first.slicingAxisPosition;
            double delta = totalSpacing / (sortedSelected.size()-1);
            new MarkActionSequenceBatch().run();
            for (int idx = 1;idx<sortedSelected.size()-1;idx++) {
                moveSlice(sortedSelected.get(idx), first.slicingAxisPosition+((double)idx)*delta);
            }
            new MarkActionSequenceBatch().run();
        }
    }

    @Override
    public void selectedSourcesUpdated(Collection<SourceAndConverter<?>> selectedSources, String triggerMode) {
        boolean changed = false;
        for (SliceSources slice:slices) {
            if (slice.isContainingAny(selectedSources)) {
                if (!slice.isSelected) {
                    changed = true;
                }
                slice.isSelected = true;
            } else {
                if (slice.isSelected) {
                    changed = true;
                }
                slice.isSelected = false;
            }
        }
        if (changed) bdvh.getViewerPanel().requestRepaint();
    }

    @Override
    public void lastSelectionEvent(Collection<SourceAndConverter<?>> lastSelectedSources, String mode, String triggerMode) {
        // Nothing
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
                double slicingAxisPosition = iSliceNoStep*sizePixX*(int) reslicedAtlas.getStep();
                createSlice(sacs.toArray(new SourceAndConverter[0]), slicingAxisPosition, 0.01, Tile.class, new Tile(-1));
            }
        }
    }

    /**
     * Inner class which contains the information necessary for the viewing of the slices:
     *
     */

    public void cancelLastAction() {
        if (userActions.size()>0) {
            CancelableAction action = userActions.get(userActions.size()-1);
            if (action instanceof MarkActionSequenceBatch) {
                action.cancel();
                action = userActions.get(userActions.size()-1);
                while (!(action instanceof MarkActionSequenceBatch)) {
                    action.cancel();
                    action = userActions.get(userActions.size()-1);
                }
                action.cancel();
            } else {
                userActions.get(userActions.size() - 1).cancel();
            }
        } else {
            bdvh.getViewerPanel().showMessage("No action can be cancelled.");
        }
    }

    public void redoAction() {
        if (redoableUserActions.size()>0) {
            CancelableAction action = redoableUserActions.get(redoableUserActions.size()-1);
            if (action instanceof MarkActionSequenceBatch) {
                action.run();
                action = redoableUserActions.get(redoableUserActions.size()-1);
                while (!(action instanceof MarkActionSequenceBatch)) {
                    action.run();
                    action = redoableUserActions.get(redoableUserActions.size()-1);
                }
                action.run();
            } else {
                redoableUserActions.get(redoableUserActions.size() - 1).run();
            }

        } else {
            bdvh.getViewerPanel().showMessage("No action can be redone.");
        }
    }

    public abstract class CancelableAction {

        public void run() {
            userActions.add(this);
            if (redoableUserActions.size()>0) {
                if (redoableUserActions.get(redoableUserActions.size()-1).equals(this)) {
                    redoableUserActions.remove(redoableUserActions.size()-1);
                } else {
                    // different branch : clear redoable actions
                    redoableUserActions.clear();
                }
            }
        }

        public void cancel() {
            if (userActions.get(userActions.size()-1).equals(this)) {
                userActions.remove(userActions.size()-1);
                redoableUserActions.add(this);
            } else {
                System.err.println("Error : cancel not called on the last action");
                return;
            }
        }

    }

    public void moveSlice(SliceSources slice, double axisPosition) {
        new MoveSlice(slice, axisPosition).run();
    }

    public class MoveSlice extends CancelableAction {

        private SliceSources sliceSource;
        private double oldSlicingAxisPosition;
        private double newSlicingAxisPosition;

        public MoveSlice(SliceSources sliceSource, double slicingAxisPosition ) {
            this.sliceSource = sliceSource;
            this.oldSlicingAxisPosition = sliceSource.slicingAxisPosition;
            // int iSliceNoStep = (int) (slicingAxisPosition / sizePixX);
            //double slicingAxisPosition = iSliceNoStep*sizePixX;
            this.newSlicingAxisPosition = slicingAxisPosition;//= iSliceNoStep*sizePixX; //slicingAxisPosition;
        }

        public void run() {
            sliceSource.slicingAxisPosition = newSlicingAxisPosition;
            sliceSource.updatePosition();
            bdvh.getViewerPanel().showMessage("Moving slice to position "+new DecimalFormat("###.##").format(sliceSource.slicingAxisPosition));
            updateDisplay();
            super.run();
        }

        public void cancel() {
            sliceSource.slicingAxisPosition = oldSlicingAxisPosition;
            sliceSource.updatePosition();
            bdvh.getViewerPanel().showMessage("Moving slice to position "+new DecimalFormat("###.##").format(sliceSource.slicingAxisPosition));
            updateDisplay();
            super.cancel();
        }
    }

    public <T extends Entity> List<SliceSources> createSlice(SourceAndConverter[] sacsArray, double slicingAxisPosition, double axisIncrement, final Class< T > attributeClass, T defaultEntity) {
        List<SliceSources> out = new ArrayList<>();
        List<SourceAndConverter<?>> sacs = Arrays.asList(sacsArray);
        if ((sacs.size()>1)&&(attributeClass!=null)) {
            // Check whether the source can be splitted, maybe based
            // Split based on Tile ?

            Map<T, List<SourceAndConverter<?>>> sacsGroups =
                    sacs.stream().collect(Collectors.groupingBy(sac -> {
                        if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac, SPIM_DATA_INFO)!=null) {
                            SourceAndConverterService.SpimDataInfo sdi = (SourceAndConverterService.SpimDataInfo) SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac, SPIM_DATA_INFO);
                            AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>> asd = ( AbstractSpimData<AbstractSequenceDescription<BasicViewSetup,?,?>>)sdi.asd;
                            BasicViewSetup bvs = asd.getSequenceDescription().getViewSetups().get(sdi.setupId);
                            return (T) bvs.getAttribute(attributeClass);
                        } else {
                            return defaultEntity;
                        }
                    }));

            List<T> sortedTiles = new ArrayList<>();

            sortedTiles.addAll(sacsGroups.keySet());

            sortedTiles.sort(Comparator.comparingInt(T::getId));

            new MarkActionSequenceBatch().run();
            for (int i = 0; i<sortedTiles.size();i++) {
                T group = sortedTiles.get(i);
                CreateSlice cs = new CreateSlice(sacsGroups.get(group), slicingAxisPosition + i * axisIncrement);
                cs.run();
                if (cs.getSlice()!=null) {
                    out.add(cs.getSlice());
                }
            }
            new MarkActionSequenceBatch().run();

        } else {
            CreateSlice cs = new CreateSlice(sacs, slicingAxisPosition);
            cs.run();
            if (cs.getSlice()!=null) {
                out.add(cs.getSlice());
            }
        }
        return out;
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
            boolean sacAlreadyPresent = false;
            for (SourceAndConverter sac : sacs) {
                for (SliceSources slice : slices) {
                   for (SourceAndConverter test : slice.original_sacs) {
                       if (test.equals(sac)) {
                           sacAlreadyPresent = true;
                       }
                   }
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
                    System.err.println("A source is already used in the positioner : slice not created.");

                    bdvh.getViewerPanel().showMessage("A source is already used in the positioner : slice not created.");
                    return;
                } else {
                    // Move action:
                    new MoveSlice(zeSlice, slicingAxisPosition).run();
                    return;
                }
            }

            if (sliceSource==null) // for proper redo function
            sliceSource = new SliceSources(sacs.toArray(new SourceAndConverter[sacs.size()]),
                    slicingAxisPosition,
                    MultiSlicePositioner.this);

            slices.add(sliceSource);

            updateDisplay();

            if (currentMode.equals(POSITIONING_MODE)) {
                SourceAndConverterServices.getSourceAndConverterDisplayService()
                        .show(bdvh, sliceSource.relocated_sacs_positioning_mode);
                sliceSource.enableGraphicalHandles();
            } else if (currentMode.equals(REGISTRATION_MODE)) {
                SourceAndConverterServices.getSourceAndConverterDisplayService()
                        .show(bdvh, sliceSource.registered_sacs);
                sliceSource.disableGraphicalHandles();
            }

            bdvh.getViewerPanel().showMessage("Slice added");

            // The line below should be executed only if the action succeeded ... (if it's executed, calling cancel should have the same effect)
            super.run();
        }

        public SliceSources getSlice() {
            return sliceSource;
        }

        @Override
        public void cancel() {
            slices.remove(sliceSource);
            SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .remove(bdvh, sliceSource.relocated_sacs_positioning_mode);
            SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .remove(bdvh, sliceSource.registered_sacs);
            bdvh.getViewerPanel().showMessage("Slice removed");
            super.cancel();
        }
    }

    public class RegisterSlice extends CancelableAction {
        final SliceSources slice;
        final Registration<SourceAndConverter[]> registration;
        final Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed;
        final Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving;

        public RegisterSlice(SliceSources slice,
                             Registration<SourceAndConverter[]> registration,
                             Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed,
                             Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving) {
            this.slice = slice;
            this.registration = registration;
            this.preprocessFixed = preprocessFixed;
            this.preprocessMoving = preprocessMoving;
        }

        @Override
        public void run() { //

            slice.addRegistration(registration, preprocessFixed, preprocessMoving);
            super.run();
        }

        @Override
        public void cancel() {
            if (slice.removeRegistration(registration)) {
            } else {
                System.err.println("Error during registration cancelling");
            }
            bdvh.getViewerPanel().showMessage("Registration cancelled");
            super.cancel();
        }
    }

    public DragBehaviour getSelectedSourceDragBehaviour(SliceSources slice) {
        return new SelectedSliceSourcesDrag(slice);
    }

    public class MarkActionSequenceBatch extends CancelableAction {

    }

    class SelectedSliceSourcesDrag implements DragBehaviour {

        Map<SliceSources,Double> initialAxisPositions = new HashMap<>();

        List<SliceSources> selectedSources = new ArrayList<>();

        RealPoint iniPointBdv = new RealPoint(3);
        double iniSlicePointing;
        double iniSlicingAxisPosition;
        double deltaOrigin;
        boolean perform = false;

        final SliceSources sliceOrigin;

        public SelectedSliceSourcesDrag(SliceSources slice) {
            this.sliceOrigin = slice;
        }

        @Override
        public void init(int x, int y) {

            bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);

            // Computes which slice it corresponds to (useful for overlay redraw)
            iniSlicePointing = iniPointBdv.getDoublePosition(0)/sX+0.5;
            iniSlicingAxisPosition = ((int) iniSlicePointing)*sizePixX*(int) reslicedAtlas.getStep();

            selectedSources =  getSortedSlices().stream().filter(slice -> slice.isSelected).collect(Collectors.toList());
            if ((selectedSources.size()>0)&&(sliceOrigin.isSelected)) {
                perform = true;
                selectedSources.stream().forEach(slice -> {
                    initialAxisPositions.put(slice,slice.slicingAxisPosition);
                });
            } else {
                if (!sliceOrigin.isSelected) {
                    sliceOrigin.isSelected = true;
                    perform = false;
                }
            }
            // Initialize the delta on a step of the zStepper
            if (perform) {
                deltaOrigin = iniSlicingAxisPosition - sliceOrigin.slicingAxisPosition;
                for (SliceSources slice : selectedSources) {
                    if (initialAxisPositions.containsKey(slice)) {
                        slice.slicingAxisPosition = initialAxisPositions.get(slice) + deltaOrigin;
                        slice.updatePosition();
                    }
                }
            }

            updateDisplay();

        }

        @Override
        public void drag(int x, int y) {
            if (perform) {
                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                double currentSlicePointing = currentMousePosition.getDoublePosition(0) / sX+0.5;
                double currentSlicingAxisPosition = ((int) currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
                double deltaAxis = currentSlicingAxisPosition - iniSlicingAxisPosition;

                for (SliceSources slice : selectedSources) {
                    if (initialAxisPositions.containsKey(slice)) {
                        slice.slicingAxisPosition = initialAxisPositions.get(slice) + deltaAxis + deltaOrigin;
                        slice.updatePosition();
                    }
                }

                updateDisplay();
            }
        }

        @Override
        public void end(int x, int y) {
            if (perform) {
                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                double currentSlicePointing = currentMousePosition.getDoublePosition(0) / sX+0.5;
                double currentSlicingAxisPosition = ((int) currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
                double deltaAxis = currentSlicingAxisPosition - iniSlicingAxisPosition;

                new MarkActionSequenceBatch().run();
                for (SliceSources slice : selectedSources) {
                    if (initialAxisPositions.containsKey(slice)) {
                        slice.slicingAxisPosition = initialAxisPositions.get(slice);
                        moveSlice(slice, initialAxisPositions.get(slice) + deltaAxis + deltaOrigin);
                    }
                }
                new MarkActionSequenceBatch().run();

                updateDisplay();
            }
        }
    }

    /*
     * Create BehaviourMap to block behaviours interfering with
     * DraBehaviour. The block map is only active while TODO determine conditions
     */
    private final BehaviourMap blockMap = new BehaviourMap();

    private void block()
    {
        bdvh.getTriggerbindings().addBehaviourMap( BLOCKING_MAP, blockMap );
    }

    private void unblock()
    {
        bdvh.getTriggerbindings().removeBehaviourMap( BLOCKING_MAP );
    }

    Set<GraphicalHandle> ghs = new HashSet<>();

    Set<GraphicalHandle> gh_below_mouse = new HashSet<>();

    @Override
    public synchronized void hover_in(GraphicalHandle gh) {
        gh_below_mouse.add(gh);
        if (gh_below_mouse.size()==1) {
            block();
        }
    }

    @Override
    public synchronized void hover_out(GraphicalHandle gh) {
        gh_below_mouse.remove(gh);
        if (gh_below_mouse.size()==0) {
            unblock();
        }
    }

    @Override
    public synchronized void mouseDragged(MouseEvent e) {
        this.ghs.forEach(gh -> gh.mouseDragged(e));
        this.slices.forEach(slice -> slice.ghs.forEach(gh -> gh.mouseDragged(e)));
    }

    @Override
    public synchronized void mouseMoved(MouseEvent e) {
        this.ghs.forEach(gh -> gh.mouseMoved(e));
        this.slices.forEach(slice -> slice.ghs.forEach(gh -> gh.mouseMoved(e)));
    }

    @Override
    public synchronized void created(GraphicalHandle gh) {

    }

    @Override
    public synchronized void removed(GraphicalHandle gh) {
        if (gh_below_mouse.contains(gh)) {
            gh_below_mouse.remove(gh);
            if (gh_below_mouse.size()==0) unblock();
        }
        ghs.remove(gh);
    }

    private static final String[] DRAG_TOGGLE_EDITOR_KEYS = new String[] { "button1" };

    private void refreshBlockMap() {
        bdvh.getTriggerbindings().removeBehaviourMap( BLOCKING_MAP );

        final Set<InputTrigger> moveCornerTriggers = new HashSet<>();
        for ( final String s : DRAG_TOGGLE_EDITOR_KEYS )
            moveCornerTriggers.add( InputTrigger.getFromString( s ) );

        final Map< InputTrigger, Set< String > > bindings = bdvh.getTriggerbindings().getConcatenatedInputTriggerMap().getAllBindings();
        final Set< String > behavioursToBlock = new HashSet<>();
        for ( final InputTrigger t : moveCornerTriggers )
            behavioursToBlock.addAll( bindings.get( t ) );

        blockMap.clear();
        final Behaviour block = new Behaviour() {};
        for ( final String key : behavioursToBlock )
            blockMap.put( key, block );
    }

    class DragLeft implements DragBehaviour {

        List<SliceSources> slicesDragged;
        Map<SliceSources,Double> initialAxisPositions = new HashMap<>();
        double range;
        double lastAxisPos;
        RealPoint iniPointBdv = new RealPoint(3);
        double iniSlicePointing;
        double iniSlicingAxisPosition;

        @Override
        public void init(int x, int y) {
            slicesDragged = getSortedSlices().stream().filter(slice -> slice.isSelected).collect(Collectors.toList());

            slicesDragged.stream().forEach(slice -> {
                initialAxisPositions.put(slice,slice.slicingAxisPosition);
            });

            range = initialAxisPositions.get(slicesDragged.get(initialAxisPositions.size()-1))-initialAxisPositions.get(slicesDragged.get(0));
            lastAxisPos = initialAxisPositions.get(slicesDragged.get(initialAxisPositions.size()-1));

            // Computes which slice it corresponds to (useful for overlay redraw)
            bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);
            iniSlicePointing = iniPointBdv.getDoublePosition(0)/sX;
            iniSlicingAxisPosition = (iniSlicePointing)*sizePixX*(int) reslicedAtlas.getStep();
        }

        @Override
        public void drag(int x, int y) {
            RealPoint currentMousePosition = new RealPoint(3);
            bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

            int currentSlicePointing = (int) ( currentMousePosition.getDoublePosition(0) / sX + 0.5);
            double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            double ratio = (lastAxisPos-currentSlicingAxisPosition)/range;

            for (SliceSources slice : slicesDragged) {
                slice.slicingAxisPosition = lastAxisPos + (initialAxisPositions.get(slice)-lastAxisPos) * ratio;
                slice.updatePosition();
            }

            updateDisplay();
        }

        @Override
        public void end(int x, int y) {
            RealPoint currentMousePosition = new RealPoint(3);
            bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

            int currentSlicePointing = (int) ( currentMousePosition.getDoublePosition(0) / sX + 0.5);
            double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            double ratio = (lastAxisPos-currentSlicingAxisPosition)/range;

            new MarkActionSequenceBatch().run();
            for (SliceSources slice : slicesDragged) {
                slice.slicingAxisPosition = initialAxisPositions.get(slice);
                moveSlice(slice, lastAxisPos + (initialAxisPositions.get(slice)-lastAxisPos) * ratio);
            }
            new MarkActionSequenceBatch().run();
            updateDisplay();
        }
    }

    class DragRight implements DragBehaviour {

        List<SliceSources> slicesDragged;
        Map<SliceSources,Double> initialAxisPositions = new HashMap<>();
        double range;
        double lastAxisPos;
        RealPoint iniPointBdv = new RealPoint(3);
        double iniSlicePointing;
        double iniSlicingAxisPosition;

        @Override
        public void init(int x, int y) {
            slicesDragged = getSortedSlices().stream().filter(slice -> slice.isSelected).collect(Collectors.toList());

            slicesDragged.stream().forEach(slice -> {
                initialAxisPositions.put(slice,slice.slicingAxisPosition);
            });

            range = initialAxisPositions.get(slicesDragged.get(initialAxisPositions.size()-1))-initialAxisPositions.get(slicesDragged.get(0));
            lastAxisPos = initialAxisPositions.get(slicesDragged.get(0));

            // Computes which slice it corresponds to (useful for overlay redraw)
            bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);
            iniSlicePointing = iniPointBdv.getDoublePosition(0)/sX;
            iniSlicingAxisPosition = (iniSlicePointing)*sizePixX*(int) reslicedAtlas.getStep();
        }

        @Override
        public void drag(int x, int y) {
            RealPoint currentMousePosition = new RealPoint(3);
            bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

            int currentSlicePointing = (int) ( currentMousePosition.getDoublePosition(0) / sX + 0.5);
            double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            double ratio = (lastAxisPos-currentSlicingAxisPosition)/range;

            for (SliceSources slice : slicesDragged) {
                slice.slicingAxisPosition = lastAxisPos - (initialAxisPositions.get(slice)-lastAxisPos) * ratio;
                slice.updatePosition();
            }

            updateDisplay();
        }

        @Override
        public void end(int x, int y) {
            RealPoint currentMousePosition = new RealPoint(3);
            bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

            int currentSlicePointing = (int) ( currentMousePosition.getDoublePosition(0) / sX + 0.5);
            double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            double ratio = (lastAxisPos-currentSlicingAxisPosition)/range;

            new MarkActionSequenceBatch().run();
            for (SliceSources slice : slicesDragged) {
                slice.slicingAxisPosition = initialAxisPositions.get(slice);
                moveSlice(slice, lastAxisPos - (initialAxisPositions.get(slice)-lastAxisPos) * ratio);
            }
            new MarkActionSequenceBatch().run();
            updateDisplay();
        }
    }

}