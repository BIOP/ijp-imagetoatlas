package ch.epfl.biop.atlas.aligner;

import bdv.util.*;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.aligner.commands.*;
import ch.epfl.biop.atlas.aligner.serializers.*;
import ch.epfl.biop.bdv.select.SelectedSourcesListener;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.sourceandconverter.affine.Elastix2DAffineRegistration;
import ch.epfl.biop.registration.sourceandconverter.spline.Elastix2DSplineRegistration;
import ch.epfl.biop.registration.sourceandconverter.spline.SacBigWarp2DRegistration;
import ch.epfl.biop.scijava.ui.bdv.BdvScijavaHelper;
import ch.epfl.biop.scijava.ui.swing.ScijavaSwingUI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Tile;
import net.imglib2.FinalInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.cache.CacheService;
import org.scijava.object.ObjectService;
import org.scijava.ui.behaviour.*;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.behaviour.EditorBehaviourUnInstaller;
import sc.fiji.bdvpg.scijava.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServiceLoader;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.services.serializers.AffineTransform3DAdapter;
import sc.fiji.bdvpg.services.serializers.RuntimeTypeAdapterFactory;
import sc.fiji.bdvpg.services.serializers.plugins.BdvPlaygroundObjectAdapterService;
import sc.fiji.bdvpg.services.serializers.plugins.IClassRuntimeAdapter;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bdv.ui.BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD;
import static bdv.ui.BdvDefaultCards.DEFAULT_VIEWERMODES_CARD;
import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;
import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.errlog;

/**
 * All specific functions and method dedicated to the multislice positioner
 *
 * Let's think a bit:
 * There will be:
 * - a positioning mode
 * - a registration mode
 * - a 3d view mode
 *
 */

public class MultiSlicePositioner extends BdvOverlay implements SelectedSourcesListener, GraphicalHandleListener, MouseMotionListener {

    //
    static public Object manualActionLock = new Object();

    /**
     * BdvHandle displaying everything
     */
    BdvHandle bdvh;

    SourceSelectorBehaviour ssb;

    /**
     * Slicing Model Properties
     */
    int nPixX, nPixY, nPixZ;
    final public double sX, sY, sZ;
    double sizePixX, sizePixY, sizePixZ;

    List<SliceSources> slices = Collections.synchronizedList(new ArrayList<>());//Collections.synchronizedList(); // Thread safety ?

    int totalNumberOfActionsRecorded = 30; // TODO : Implement

    protected List<CancelableAction> userActions = new ArrayList<>();

    protected List<CancelableAction> redoableUserActions = new ArrayList<>();

    /**
     * Current coordinate where Sources are dragged
     */
    int iSliceNoStep;

    Context scijavaCtx;

    public MultiSliceObserver mso;

    /**
     * Shift in Y : control overlay or not of sources
     *
     * @param bdvh
     * @param slicingModel
     */

    public int currentMode = POSITIONING_MODE_INT;

    public final static int POSITIONING_MODE_INT = 0;
    final static String POSITIONING_MODE = "positioning-mode";
    final static String POSITIONING_BEHAVIOURS_KEY = POSITIONING_MODE + "-behaviours";
    Behaviours positioning_behaviours = new Behaviours(new InputTriggerConfig(), POSITIONING_MODE);

    public final static int REGISTRATION_MODE_INT = 1;
    final static String REGISTRATION_MODE = "Registration";
    final static String REGISTRATION_BEHAVIOURS_KEY = REGISTRATION_MODE + "-behaviours";
    Behaviours registration_behaviours = new Behaviours(new InputTriggerConfig(), REGISTRATION_MODE);

    boolean single_slide_display_mode = false;

    final static String COMMON_BEHAVIOURS_KEY = "multipositioner-behaviours";
    Behaviours common_behaviours = new Behaviours(new InputTriggerConfig(), "multipositioner");

    private static final String BLOCKING_MAP = "multipositioner-blocking";

    int iCurrentSlice = 0;

    Integer[] rightPosition = new Integer[]{0, 0, 0};

    Integer[] leftPosition = new Integer[]{0, 0, 0};

    ReslicedAtlas reslicedAtlas;

    BiopAtlas biopAtlas;

    SelectionLayer selectionLayer;

    int previouszStep;

    double roiPX, roiPY, roiSX, roiSY;

    Consumer<String> log = (message) -> {
        getBdvh().getViewerPanel().showMessage(message);
    };

    public MultiSlicePositioner(BdvHandle bdvh, BiopAtlas biopAtlas, ReslicedAtlas reslicedAtlas, Context ctx) {
        this.reslicedAtlas = reslicedAtlas;
        this.biopAtlas = biopAtlas;
        this.bdvh = bdvh;
        this.scijavaCtx = ctx;

        iSliceNoStep = (int) (reslicedAtlas.getStep());

        this.bdvh.getViewerPanel().setTransferHandler(new MultiSlicePositioner.TransferHandler());

        nPixX = (int) reslicedAtlas.slicingModel.getSpimSource().getSource(0, 0).dimension(0);
        nPixY = (int) reslicedAtlas.slicingModel.getSpimSource().getSource(0, 0).dimension(1);
        nPixZ = (int) reslicedAtlas.slicingModel.getSpimSource().getSource(0, 0).dimension(2);

        AffineTransform3D at3D = new AffineTransform3D();
        reslicedAtlas.slicingModel.getSpimSource().getSourceTransform(0, 0, at3D);

        double[] m = at3D.getRowPackedCopy();

        sizePixX = Math.sqrt(m[0] * m[0] + m[4] * m[4] + m[8] * m[8]);
        sizePixY = Math.sqrt(m[1] * m[1] + m[5] * m[5] + m[9] * m[9]);
        sizePixZ = Math.sqrt(m[2] * m[2] + m[6] * m[6] + m[10] * m[10]);

        sX = nPixX * sizePixX;
        sY = nPixY * sizePixY;
        sZ = nPixZ * sizePixZ;

        BdvFunctions.showOverlay(this, "MultiSlice Overlay", BdvOptions.options().addTo(bdvh));

        ssb = (SourceSelectorBehaviour) SourceAndConverterServices.getSourceAndConverterDisplayService().getDisplayMetadata(
                bdvh, SourceSelectorBehaviour.class.getSimpleName());
        new EditorBehaviourUnInstaller(bdvh).run();

        // Disable edit mode by default
        bdvh.getTriggerbindings().removeInputTriggerMap(SourceSelectorBehaviour.SOURCES_SELECTOR_TOGGLE_MAP);

        setPositioningMode();

        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.toggle_display_mode(), "toggle_single_source_mode", "S");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.cancelLastAction(), "cancel_last_action", "ctrl Z");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.redoAction(), "redo_last_action", "ctrl Y", "ctrl shift Z");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.navigateNextSlice(), "navigate_next_slice", "N");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.navigatePreviousSlice(), "navigate_previous_slice", "P"); // P taken for panel
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.navigateCurrentSlice(), "navigate_current_slice", "C");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.nextMode(), "change_mode", "Q");

        bdvh.getTriggerbindings().addBehaviourMap(COMMON_BEHAVIOURS_KEY, common_behaviours.getBehaviourMap());
        bdvh.getTriggerbindings().addInputTriggerMap(COMMON_BEHAVIOURS_KEY, common_behaviours.getInputTriggerMap()); // "transform", "bdv"
        //common_behaviours.install(bdvh.getTriggerbindings(), COMMON_BEHAVIOURS_KEY);

        overrideStandardNavigation();

        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> this.toggleOverlap(), "toggle_superimpose", "O");

        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> {
            if (ssb.isEnabled()) {
                ssb.disable();
                refreshBlockMap();
            } else {
                ssb.enable();
                refreshBlockMap();
            }
        }, "toggle_editormode", "E");

        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> this.equalSpacingSelectedSlices(), "equalSpacingSelectedSlices", "A");
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> {slices.forEach(slice -> slice.select());bdvh.getViewerPanel().getDisplay().repaint();}, "selectAllSlices", "ctrl A");
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> {slices.forEach(slice -> slice.deSelect());bdvh.getViewerPanel().getDisplay().repaint();}, "deselectAllSlices", "ctrl shift A");

        ssb.addSelectedSourcesListener(this);

        List<SourceAndConverter<?>> sacsToAppend = new ArrayList<>();
        for (int i = 0; i < biopAtlas.map.getStructuralImages().length; i++) {
            sacsToAppend.add(reslicedAtlas.extendedSlicedSources[i]);
            sacsToAppend.add(reslicedAtlas.nonExtendedSlicedSources[i]);
        }

        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .show(bdvh, sacsToAppend.toArray(new SourceAndConverter[0]));

        bdvh.getViewerPanel().getDisplay().addHandler(this);

        GraphicalHandle ghRight = new SquareGraphicalHandle(this, new DragRight(), "drag_right", "button1", bdvh.getTriggerbindings(),
                () -> rightPosition, () -> 25, () -> new Integer[]{255, 0, 255, 200});
        GraphicalHandle ghCenter = new CircleGraphicalHandle(this, new SelectedSliceSourcesDrag(), "translate_selected", "button1", bdvh.getTriggerbindings(),
                () -> new Integer[]{(leftPosition[0]+rightPosition[0])/2,(leftPosition[1]+rightPosition[1])/2}, () -> 25, () -> new Integer[]{255, 0, 255, 200});
        GraphicalHandle ghLeft = new SquareGraphicalHandle(this, new DragLeft(), "drag_right", "button1", bdvh.getTriggerbindings(),
                () -> leftPosition, () -> 25, () -> new Integer[]{255, 0, 255, 200});

        ghs.add(ghRight);
        ghs.add(ghCenter);
        ghs.add(ghLeft);

        this.bdvh.getCardPanel().setCardExpanded("Sources", false);
        this.bdvh.getCardPanel().setCardExpanded("Groups", false);

        reslicedAtlas.addListener(() -> {
            recenterBdvh();
            updateDisplay();
        });

        previouszStep = (int) reslicedAtlas.getStep();

        mso = new MultiSliceObserver(this);

        bdvh.getCardPanel().removeCard(DEFAULT_SOURCEGROUPS_CARD);
        bdvh.getCardPanel().removeCard(DEFAULT_VIEWERMODES_CARD);

        BdvScijavaHelper.clearBdvHandleMenuBar(bdvh);

        int hierarchyLevelsSkipped = 4;

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, MSPStateLoadCommand.class, hierarchyLevelsSkipped,"mp", this );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, MSPStateSaveCommand.class, hierarchyLevelsSkipped,"mp", this);

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Undo [Ctrl+Z]",0,() -> this.cancelLastAction());
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Redo [Ctrl+Shift+Z]",0,() -> this.redoAction());
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Select all slices [Ctrl+A]",0,() -> slices.forEach(slice -> slice.select()));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Deselect all slices [Ctrl+Shift+A]",0,() -> slices.forEach(slice -> slice.deSelect()));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Distribute spacing [A]",0,() -> {
            if (this.currentMode == POSITIONING_MODE_INT) this.equalSpacingSelectedSlices();
        });

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>Positioning Mode",0,() -> this.setPositioningMode());
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>Registration Mode",0,() -> this.setRegistrationMode());
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>Change Display Mode [Q]",0,() -> this.toggle_display_mode());
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>Change Overlap Mode [O]",0,() -> this.toggleOverlap());
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>Next Slice [N]",0,() -> this.navigateNextSlice());
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>Previous Slice [P]",0,() -> this.navigatePreviousSlice());
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>Center On Current Slice [C]",0,() -> this.navigateCurrentSlice());

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, ImportQuPathProjectCommand.class, hierarchyLevelsSkipped,"mp", this );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, ImportImagePlusCommand.class, hierarchyLevelsSkipped,"mp", this );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, RegistrationElastixAffineCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, RegistrationElastixSplineCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, RegistrationBigWarpCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, ExportRegionsToFileCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, ExportRegionsToRoiManagerCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, ExportRegionsToQuPathCommand.class, hierarchyLevelsSkipped,"mp", this);

        // TODO BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Registration>Remove Last Registration",0,() -> );


        bdvh.getCardPanel().addCard("Atlas Slicing",
                ScijavaSwingUI.getPanel(scijavaCtx, SlicerAdjusterInteractiveCommand.class, "reslicedAtlas", reslicedAtlas),
                true);

        bdvh.getCardPanel().addCard("Define region of interest",
                ScijavaSwingUI.getPanel(scijavaCtx, RectangleROIDefineInteractiveCommand.class, "mp", this),
                true);

        bdvh.getCardPanel().addCard("Tasks Info", mso.getJPanel(), true);

        // Default registration region = full atlas size
        roiPX = -sX / 2.0;
        roiPY = -sY / 2.0;
        roiSX = sX;
        roiSY = sY;

        BiConsumer<RealLocalizable, UnsignedShortType> fun = (loc,val) -> {
            double px = loc.getFloatPosition(0);
            double py = loc.getFloatPosition(1);

            if (py<-sY/1.9) {val.set(0); return;}
            if (py>sY/1.9) {val.set(0); return;}

            if (currentMode == POSITIONING_MODE_INT) {
                if (Math.IEEEremainder(px+sX*0.5, sX) < roiPX) {val.set(255); return;}
                if (Math.IEEEremainder(px+sX*0.5, sX) > roiPX+roiSX) {val.set(255); return;}
                if (py<roiPY) {val.set(255); return;}
                if (py>roiPY+roiSY) {val.set(255); return;}
                val.set(0);
            }

            if (currentMode == REGISTRATION_MODE_INT) {
                if (loc.getFloatPosition(0) < roiPX) {val.set(255); return;}
                if (loc.getFloatPosition(0) > roiPX+roiSX) {val.set(255); return;}
                if (loc.getFloatPosition(1)<roiPY) {val.set(255); return;}
                if (loc.getFloatPosition(1)>roiPY+roiSY) {val.set(255); return;}
                val.set(0);
            }

        };

        FunctionRealRandomAccessible<UnsignedShortType> roiOverlay = new FunctionRealRandomAccessible(3, fun, () -> new UnsignedShortType());

        BdvStackSource bss = BdvFunctions.show(roiOverlay,
                new FinalInterval(new long[]{0, 0, 0}, new long[]{10, 10, 10}),"ROI", BdvOptions.options().addTo(bdvh));

        bss.setDisplayRangeBounds(0,1600);
        currentMode = REGISTRATION_MODE_INT; // For correct toggling
        setPositioningMode();

        addRightClickActions();

        AffineTransform3D iniView = new AffineTransform3D();

        iniView.translate(-500,5,0);
        iniView.scale(20);
        bdvh.getViewerPanel().state().setViewerTransform(iniView);

        reslicedAtlas.setStep(50);
        reslicedAtlas.setRotateX(0);
        reslicedAtlas.setRotateY(0);

        bdvh.getSplitPanel().setCollapsed(false);
        bdvh.getSplitPanel().setDividerLocation(0.7);

        BdvHandleHelper.setBdvHandleCloseOperation(bdvh, ctx.getService(CacheService.class),
                SourceAndConverterServices.getSourceAndConverterDisplayService(), false,
                    () -> {
                        if (mso!=null) this.mso.clear();
                        if (userActions!=null) this.userActions.clear();
                        if (slices!=null) this.slices.clear();
                        this.biopAtlas = null;
                        this.slices = null;
                        this.userActions = null;
                        ctx.getService(ObjectService.class).removeObject(this);
                        this.mso = null;
                        this.selectionLayer = null;
                        this.common_behaviours = null;
                        this.positioning_behaviours = null;
                        this.registration_behaviours = null;
                        this.ssb = null;
                        this.reslicedAtlas = null;
                        this.info = null;
                    }
                );
    }

    void addRightClickActions() {
       common_behaviours.behaviour(new MultiSliceContextMenuClickBehaviour( this, this::getSelectedSources ), "Slices Context Menu", "button3");
    }

    public void setSingleSliceDisplayMode() {
        if (!single_slide_display_mode) {
            toggle_display_mode();
        }
    }

    public void setAllSliceDisplayMode() {
        if (single_slide_display_mode) {
            toggle_display_mode();
        }
    }

    void toggle_display_mode() {
        single_slide_display_mode = !single_slide_display_mode;
        synchronized (slices) {
            slices.forEach(this::updateSliceDisplay);
        }
    }

    public ReslicedAtlas getReslicedAtlas() {
        return reslicedAtlas;
    }

    public List<SliceSources> getSelectedSources() {
        return getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
    }

    public BdvHandle getBdvh() {
        return bdvh;
    }

    private void overrideStandardNavigation() {
        bdvh.getKeybindings().addInputMap("blocking-multipositioner", new InputMap(), "bdv", "navigation");
        InputTriggerMap itm = new InputTriggerMap();

        itm.put(InputTrigger.getFromString("button3"), "drag translate");
        itm.put(InputTrigger.getFromString("UP"), "zoom in");
        itm.put(InputTrigger.getFromString("shift UP"), "zoom in fast");
        itm.put(InputTrigger.getFromString("ctrl UP"), "zoom in slow");
        itm.put(InputTrigger.getFromString("scroll"), "scroll zoom");

        itm.put(InputTrigger.getFromString("DOWN"), "zoom out");
        itm.put(InputTrigger.getFromString("shift DOWN"), "zoom out fast");
        itm.put(InputTrigger.getFromString("ctrl DOWN"), "zoom out slow");

        selectionLayer = new SelectionLayer(this);
        selectionLayer.addSelectionBehaviours(common_behaviours);
        refreshBlockMap();

        bdvh.getTriggerbindings().addInputTriggerMap("default_navigation", itm, "transform");
    }

    ScrollBehaviour zoomWheel = new ScrollBehaviour() {
        @Override
        public void scroll(double v, boolean b, int i, int i1) {

        }
    };

    public void recenterBdvh() {
        double cur_wcx = bdvh.getViewerPanel().getWidth() / 2.0; // Current Window Center X
        double cur_wcy = bdvh.getViewerPanel().getHeight() / 2.0; // Current Window Center Y

        RealPoint centerScreenCurrentBdv = new RealPoint(new double[]{cur_wcx, cur_wcy, 0});
        RealPoint centerScreenGlobalCoord = new RealPoint(3);

        AffineTransform3D at3D = new AffineTransform3D();
        bdvh.getBdvHandle().getViewerPanel().state().getViewerTransform(at3D);

        at3D.inverse().apply(centerScreenCurrentBdv, centerScreenGlobalCoord);

        // New target
        centerScreenGlobalCoord.setPosition((centerScreenGlobalCoord.getDoublePosition(0) - sX / 2.0) * (double) previouszStep / (double) reslicedAtlas.getStep() + sX / 2.0, 0);

        // How should we translate at3D, such as the screen center is the new one

        // Now compute what should be the matrix in the next bdv frame:
        AffineTransform3D nextAffineTransform = new AffineTransform3D();

        // It should have the same scaling and rotation than the current view
        nextAffineTransform.set(at3D);

        // No Shift
        nextAffineTransform.set(0, 0, 3);
        nextAffineTransform.set(0, 1, 3);
        nextAffineTransform.set(0, 2, 3);

        // But the center of the window should be centerScreenGlobalCoord
        // Let's compute the shift
        double next_wcx = bdvh.getViewerPanel().getWidth() / 2.0; // Next Window Center X
        double next_wcy = bdvh.getViewerPanel().getHeight() / 2.0; // Next Window Center Y

        RealPoint centerScreenNextBdv = new RealPoint(new double[]{next_wcx, next_wcy, 0});
        RealPoint shiftNextBdv = new RealPoint(3);

        nextAffineTransform.inverse().apply(centerScreenNextBdv, shiftNextBdv);

        double sx = -centerScreenGlobalCoord.getDoublePosition(0) + shiftNextBdv.getDoublePosition(0);
        double sy = -centerScreenGlobalCoord.getDoublePosition(1) + shiftNextBdv.getDoublePosition(1);
        double sz = -centerScreenGlobalCoord.getDoublePosition(2) + shiftNextBdv.getDoublePosition(2);

        RealPoint shiftWindow = new RealPoint(new double[]{sx, sy, sz});
        RealPoint shiftMatrix = new RealPoint(3);
        nextAffineTransform.apply(shiftWindow, shiftMatrix);

        nextAffineTransform.set(shiftMatrix.getDoublePosition(0), 0, 3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(1), 1, 3);
        nextAffineTransform.set(shiftMatrix.getDoublePosition(2), 2, 3);

        bdvh.getBdvHandle().getViewerPanel().state().setViewerTransform(nextAffineTransform);
        previouszStep = (int) reslicedAtlas.getStep();
    }

    /**
     * Gets all slices sorted along the slicing axis
     *
     * @return
     */
    public List<SliceSources> getSortedSlices() {
        List<SliceSources> sortedSlices = new ArrayList<>(slices);
        Collections.sort(sortedSlices, Comparator.comparingDouble(s -> s.getSlicingAxisPosition()));
        // Sending index info to slices each time this function is called
        for (int i = 0; i < sortedSlices.size(); i++) {
            sortedSlices.get(i).setIndex(i);
        }
        return sortedSlices;
    }

    // --------------------------------------------------------- SETTING MODES

    /**
     * Toggles between positioning and registration mode
     */
    public void nextMode() {
        switch (currentMode) {
            case POSITIONING_MODE_INT:
                setRegistrationMode();
                break;
            case REGISTRATION_MODE_INT:
                setPositioningMode();
                break;
        }
    }

    /**
     * Set the positioning mode
     */
    public void setPositioningMode() {
        if (!(currentMode == POSITIONING_MODE_INT)) {

            synchronized (slices) {
                reslicedAtlas.unlock();
                currentMode = POSITIONING_MODE_INT;
                ghs.forEach(gh -> gh.enable());
                slices.forEach(slice -> slice.enableGraphicalHandles());
                getSortedSlices().forEach(ss -> {
                    SourceAndConverterUtils.transferColorConverters(ss.registered_sacs, ss.relocated_sacs_positioning_mode);
                    updateSliceDisplay(ss);
                });
            }

            SourceAndConverterUtils.transferColorConverters(reslicedAtlas.nonExtendedSlicedSources, reslicedAtlas.extendedSlicedSources);
            for (int i = 0; i < reslicedAtlas.nonExtendedSlicedSources.length; i++) {
                if (SourceAndConverterServices.getSourceAndConverterDisplayService()
                        .isVisible(reslicedAtlas.nonExtendedSlicedSources[i], bdvh)) {
                    bdvh.getViewerPanel().state().setSourceActive(reslicedAtlas.nonExtendedSlicedSources[i], false);
                    bdvh.getViewerPanel().state().setSourceActive(reslicedAtlas.extendedSlicedSources[i], true);
                } else {
                    bdvh.getViewerPanel().state().setSourceActive(reslicedAtlas.extendedSlicedSources[i], false);
                }
            }

            bdvh.getTriggerbindings().removeInputTriggerMap(REGISTRATION_BEHAVIOURS_KEY);
            bdvh.getTriggerbindings().removeBehaviourMap(REGISTRATION_BEHAVIOURS_KEY);
            positioning_behaviours.install(bdvh.getTriggerbindings(), POSITIONING_BEHAVIOURS_KEY);
            navigateCurrentSlice();
            refreshBlockMap();
        }
    }

    void hide(SliceSources slice) {
        SourceAndConverterServices
                .getSourceAndConverterDisplayService()
                .remove(bdvh, slice.registered_sacs);
        SourceAndConverterServices
                .getSourceAndConverterDisplayService()
                .remove(bdvh, slice.relocated_sacs_positioning_mode);
    }

    void showPositioning(SliceSources slice) {
        SourceAndConverterServices
                .getSourceAndConverterDisplayService()
                .remove(bdvh, slice.registered_sacs);
        SourceAndConverterServices
                .getSourceAndConverterDisplayService()
                .show(bdvh, slice.relocated_sacs_positioning_mode);
    }

    void showRegistration(SliceSources slice) {
        SourceAndConverterServices
                .getSourceAndConverterDisplayService()
                .remove(bdvh, slice.relocated_sacs_positioning_mode);
        SourceAndConverterServices
                .getSourceAndConverterDisplayService()
                .show(bdvh, slice.registered_sacs);
    }

    void updateSliceDisplay(SliceSources slice) {
        if (single_slide_display_mode) {
            if ((iCurrentSlice>=0)&&(iCurrentSlice<slices.size())){
                if (getSortedSlices().get(iCurrentSlice) != slice){
                    hide(slice);
                    return;
                }
            }
        }
        if (currentMode == POSITIONING_MODE_INT) { // TODO : fix potential issue if user switches fast between modes ?
            showPositioning(slice);
        }
        if (currentMode == REGISTRATION_MODE_INT) { // TODO : fix potential issue if user switches fast between modes ?
            showRegistration(slice);
        }
    }

    /**
     * Set the registration mode
     */
    public void setRegistrationMode() {
        if (!(currentMode == REGISTRATION_MODE_INT)) {
            currentMode = POSITIONING_MODE_INT;
            reslicedAtlas.lock();
            currentMode = REGISTRATION_MODE_INT;
            ghs.forEach(gh -> gh.disable());
            synchronized (slices) {
                getSortedSlices().forEach(ss -> {
                    SourceAndConverterUtils.transferColorConverters(ss.relocated_sacs_positioning_mode, ss.registered_sacs);
                    updateSliceDisplay(ss);
                });
            }

            SourceAndConverterUtils.transferColorConverters(reslicedAtlas.extendedSlicedSources, reslicedAtlas.nonExtendedSlicedSources);
            for (int i = 0; i < reslicedAtlas.nonExtendedSlicedSources.length; i++) {
                if (SourceAndConverterServices.getSourceAndConverterDisplayService()
                        .isVisible(reslicedAtlas.extendedSlicedSources[i], bdvh)) {
                    bdvh.getViewerPanel().state().setSourceActive(reslicedAtlas.extendedSlicedSources[i], false);
                    bdvh.getViewerPanel().state().setSourceActive(reslicedAtlas.nonExtendedSlicedSources[i], true);
                } else {
                    bdvh.getViewerPanel().state().setSourceActive(reslicedAtlas.nonExtendedSlicedSources[i], false);
                }
            }

            bdvh.getTriggerbindings().removeInputTriggerMap(POSITIONING_BEHAVIOURS_KEY);
            bdvh.getTriggerbindings().removeBehaviourMap(POSITIONING_BEHAVIOURS_KEY);
            registration_behaviours.install(bdvh.getTriggerbindings(), REGISTRATION_BEHAVIOURS_KEY);
            navigateCurrentSlice();
        }
        refreshBlockMap();
    }

    // -------------------------------------------------------- NAVIGATION ( BOTH MODES )

    /**
     * Center bdv on next slice (iCurrentSlice + 1)
     */
    public void navigateNextSlice() {
        List<SliceSources> sortedSlices = getSortedSlices();
        int previousSliceIndex = iCurrentSlice;
        iCurrentSlice++;
        if (iCurrentSlice >= sortedSlices.size()) {
            iCurrentSlice = 0;
        }
        if (sortedSlices.size() > 0) {
            centerBdvViewOn(sortedSlices.get(iCurrentSlice));
            updateSliceDisplay(sortedSlices.get(previousSliceIndex));
            updateSliceDisplay(sortedSlices.get(iCurrentSlice));
        }
    }

    /**
     * Defines, in physical units, the region that will be used to perform the automated registration
     * @param px
     * @param py
     * @param sx
     * @param sy
     */
    public void setROI(double px, double py, double sx, double sy) {
        if (px<-sX / 2.0) {
            double delta = -(px+sX/2.0);
            px = -sX / 2.0;
            sx = sx-delta;
        }
        if (px > sX/2.0) {px = sX/2.0;}

        if (px+sx>sX / 2.0) {
            sx = sX / 2.0 -px;
        }

        if (py<-sY / 2.0) {
            double delta = -(py+sY/2.0);
            py = -sY / 2.0;
            sy = sy-delta;
        }
        if (py > sY/2.0) {py = sY/2.0;}

        if (py+sy>sY/2.0) {
            sy = sY/2.0-py;
        }

        roiPX = px;
        roiPY = py;
        roiSX = sx;
        roiSY = sy;

    }

    /**
     * Center bdv on current slice (iCurrentSlice)
     */
    public void navigateCurrentSlice() {
        List<SliceSources> sortedSlices = getSortedSlices();

        if (iCurrentSlice >= sortedSlices.size()) {
            iCurrentSlice = 0;
        }

        if (sortedSlices.size() > 0) {
            centerBdvViewOn(sortedSlices.get(iCurrentSlice));
        }
    }

    /**
     * Center bdv on previous slice (iCurrentSlice - 1)
     */
    public void navigatePreviousSlice() {
        int previousSliceIndex = iCurrentSlice;
        iCurrentSlice--;
        List<SliceSources> sortedSlices = getSortedSlices();

        if (iCurrentSlice < 0) {
            iCurrentSlice = sortedSlices.size() - 1;
        }

        if (sortedSlices.size() > 0) {
            centerBdvViewOn(sortedSlices.get(iCurrentSlice));
            updateSliceDisplay(sortedSlices.get(previousSliceIndex));
            updateSliceDisplay(sortedSlices.get(iCurrentSlice));
        }

    }

    public void selectSlice(SliceSources... slices) {
        for (SliceSources slice : slices) {
            slice.select();
        }
    }

    public void selectSlice(List<SliceSources> slices) {
        selectSlice(slices.toArray(new SliceSources[slices.size()]));
    }

    public void deselectSlice(SliceSources... slices) {
        for (SliceSources slice : slices) {
            slice.deSelect();
        }
    }

    public void deselectSlice(List<SliceSources> slices) {
        deselectSlice(slices.toArray(new SliceSources[slices.size()]));
    }

    public void waitForTasks() {
        slices.forEach(slice -> {
            slice.waitForEndOfTasks();
        });
    }

    /**
     * Center bdv on a slice
     *
     * @param slice
     */
    public void centerBdvViewOn(SliceSources slice) {

        RealPoint centerSlice = new RealPoint(3);

        if (currentMode == MultiSlicePositioner.POSITIONING_MODE_INT) {
            centerSlice = slice.getCenterPositionPMode();
        } else if (currentMode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
            centerSlice = slice.getCenterPositionRMode();
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

        bdvh.getViewerPanel().state().setViewerTransform(nextAffineTransform);
        bdvh.getViewerPanel().requestRepaint();

    }

    protected void updateDisplay() {
        // Sort slices along slicing axis

        if (cycleToggle == 0) {
            slices.forEach(slice -> slice.yShift_slicing_mode = 1);
        } else if (cycleToggle == 2) {

            double lastPositionAlongX = -Double.MAX_VALUE;

            int stairIndex = 0;

            for (SliceSources slice : getSortedSlices()) {
                double posX = slice.getCenterPositionPMode().getDoublePosition(0);
                if (posX >= (lastPositionAlongX + sX)) {
                    stairIndex = 0;
                    lastPositionAlongX = posX;
                    slice.yShift_slicing_mode = 1;
                } else {
                    stairIndex++;
                    slice.yShift_slicing_mode = 1 + stairIndex;
                }
            }
        } else if (cycleToggle == 1) {
            slices.forEach(slice -> slice.yShift_slicing_mode = 0);
        }

        for (SliceSources slice : slices) {
            slice.updatePosition();
        }

        bdvh.getViewerPanel().requestRepaint();
    }

    int cycleToggle = 0;

    @Override
    protected synchronized void draw(Graphics2D g) {
        {
            int colorCode = this.info.getColor().get();
            Color color = new Color(ARGBType.red(colorCode), ARGBType.green(colorCode), ARGBType.blue(colorCode), ARGBType.alpha(colorCode));
            g.setColor(color);

            //g.drawString(currentMode, 10, 10);

            RealPoint[][] ptRectWorld = new RealPoint[2][2];
            Point[][] ptRectScreen = new Point[2][2];

            AffineTransform3D bdvAt3D = new AffineTransform3D();

            bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);

            for (int xp = 0; xp < 2; xp++) {
                for (int yp = 0; yp < 2; yp++) {
                    ptRectWorld[xp][yp] = new RealPoint(3);
                    RealPoint pt = ptRectWorld[xp][yp];
                    pt.setPosition(sX * (iSliceNoStep + xp), 0);
                    pt.setPosition(sY * (0.5 + yp), 1);
                    pt.setPosition(0, 2);
                    bdvAt3D.apply(pt, pt);
                    ptRectScreen[xp][yp] = new Point((int) pt.getDoublePosition(0), (int) pt.getDoublePosition(1));
                }
            }

            g.drawLine(ptRectScreen[0][0].x, ptRectScreen[0][0].y, ptRectScreen[1][0].x, ptRectScreen[1][0].y);
            g.drawLine(ptRectScreen[1][0].x, ptRectScreen[1][0].y, ptRectScreen[1][1].x, ptRectScreen[1][1].y);
            g.drawLine(ptRectScreen[1][1].x, ptRectScreen[1][1].y, ptRectScreen[0][1].x, ptRectScreen[0][1].y);
            g.drawLine(ptRectScreen[0][1].x, ptRectScreen[0][1].y, ptRectScreen[0][0].x, ptRectScreen[0][0].y);

            synchronized (slices) {
                for (SliceSources slice : slices) {
                    slice.drawGraphicalHandles(g);
                }
            }

            g.setColor(color);

            if (iCurrentSlice != -1 && slices.size() > iCurrentSlice) {
                SliceSources slice = getSortedSlices().get(iCurrentSlice);
                g.setColor(new Color(255, 255, 255, 128));
                Integer[] coords = slice.getBdvHandleCoords();
                RealPoint sliceCenter = new RealPoint(coords[0], coords[1], 0);
                g.drawOval((int) sliceCenter.getDoublePosition(0) - 15, (int) sliceCenter.getDoublePosition(1) - 15, 30, 30);
            }

            if ((currentMode == POSITIONING_MODE_INT) && slices.stream().anyMatch(slice -> slice.isSelected())) {
                List<SliceSources> sortedSelected = getSortedSlices().stream().filter(slice -> slice.isSelected()).collect(Collectors.toList());
                RealPoint precedentPoint = null;

                for (int i = 0; i < sortedSelected.size(); i++) {
                    SliceSources slice = sortedSelected.get(i);

                    Integer[] coords = slice.getBdvHandleCoords();
                    RealPoint sliceCenter = new RealPoint(coords[0], coords[1], 0);

                    if (precedentPoint != null) {
                        g.drawLine((int) precedentPoint.getDoublePosition(0), (int) precedentPoint.getDoublePosition(1),
                                (int) sliceCenter.getDoublePosition(0), (int) sliceCenter.getDoublePosition(1));
                    } else {
                        precedentPoint = new RealPoint(2);
                    }

                    precedentPoint.setPosition(sliceCenter.getDoublePosition(0), 0);
                    precedentPoint.setPosition(sliceCenter.getDoublePosition(1), 1);

                    bdvAt3D.apply(sliceCenter, sliceCenter);

                    if (i == 0) {
                        RealPoint handleLeftPoint = slice.getCenterPositionPMode();
                        handleLeftPoint.setPosition(-sY, 1);
                        bdvAt3D.apply(handleLeftPoint, handleLeftPoint);

                        leftPosition[0] = (int) handleLeftPoint.getDoublePosition(0);
                        leftPosition[1] = (int) handleLeftPoint.getDoublePosition(1) - 50;
                    }

                    if (i == sortedSelected.size() - 1) {
                        RealPoint handleRightPoint = slice.getCenterPositionPMode();
                        handleRightPoint.setPosition(-sY, 1);
                        bdvAt3D.apply(handleRightPoint, handleRightPoint);

                        rightPosition[0] = (int) handleRightPoint.getDoublePosition(0);
                        rightPosition[1] = (int) handleRightPoint.getDoublePosition(1) - 50;
                    }
                }

                if (sortedSelected.size() > 1) {
                    ghs.forEach(gh -> gh.enable());
                    g.setColor(new Color(255, 0, 255, 200));
                    g.drawLine(leftPosition[0], leftPosition[1], rightPosition[0], rightPosition[1]);
                } else {
                    ghs.forEach(gh -> gh.disable());
                }
                ghs.forEach(gh -> gh.draw(g));
            }
            if (selectionLayer != null) {
                selectionLayer.draw(g);
            }

            if (mso != null) {
                mso.draw(g);
            }
        }
    }

    @Override
    public void selectedSourcesUpdated(Collection<SourceAndConverter<?>> selectedSources, String triggerMode) {
        boolean changed = false;
        for (SliceSources slice : slices) {
            if (slice.isContainingAny(selectedSources)) {
                if (!slice.isSelected()) {
                    changed = true;
                }
                slice.select();
            } else {
                if (slice.isSelected()) {
                    changed = true;
                }
                slice.deSelect();
            }
        }
        if (changed) bdvh.getViewerPanel().getDisplay().repaint();//.requestRepaint();
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
            iSliceNoStep = (int) (pt3d.getDoublePosition(0) / sX);

            //Repaint the overlay only
            bdvh.getViewerPanel().getDisplay().repaint();
        }

        /**
         * When the user drops the data -> import the slices
         *
         * @param support
         * @param sacs
         */
        @Override
        public void importSourcesAndConverters(TransferSupport support, List<SourceAndConverter<?>> sacs) {

            //SwingUtilities.invokeLater(() -> {
                Optional<BdvHandle> bdvh = getBdvHandleFromViewerPanel(((bdv.viewer.ViewerPanel) support.getComponent()));
                if (bdvh.isPresent()) {
                    double slicingAxisPosition = iSliceNoStep * sizePixX * (int) reslicedAtlas.getStep();
                    createSlice(sacs.toArray(new SourceAndConverter[0]), slicingAxisPosition, 0.01, Tile.class, new Tile(-1));
                }
            //});
        }
    }

    //-----------------------------------------

    public SliceSources createSlice(SourceAndConverter[] sacsArray, double slicingAxisPosition) {
        CreateSlice cs = new CreateSlice(this, Arrays.asList(sacsArray), slicingAxisPosition);
        cs.runRequest();
        return cs.getSlice();
    }

    public <T extends Entity> List<SliceSources> createSlice(SourceAndConverter[] sacsArray, double slicingAxisPosition, double axisIncrement, final Class<T> attributeClass, T defaultEntity) {
        List<SliceSources> out = new ArrayList<>();
        List<SourceAndConverter<?>> sacs = Arrays.asList(sacsArray);
        if ((sacs.size() > 1) && (attributeClass != null)) {
            // Check whether the source can be splitted, maybe based
            // Split based on Tile ?

            Map<T, List<SourceAndConverter<?>>> sacsGroups =
                    sacs.stream().collect(Collectors.groupingBy(sac -> {
                        if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac, SPIM_DATA_INFO) != null) {
                            SourceAndConverterService.SpimDataInfo sdi = (SourceAndConverterService.SpimDataInfo) SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac, SPIM_DATA_INFO);
                            AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>> asd = (AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>>) sdi.asd;
                            BasicViewSetup bvs = asd.getSequenceDescription().getViewSetups().get(sdi.setupId);
                            return (T) bvs.getAttribute(attributeClass);
                        } else {
                            return defaultEntity;
                        }
                    }));

            List<T> sortedTiles = new ArrayList<>();

            sortedTiles.addAll(sacsGroups.keySet());

            sortedTiles.sort(Comparator.comparingInt(T::getId));

            new MarkActionSequenceBatch(this).runRequest();
            for (int i = 0; i < sortedTiles.size(); i++) {
                T group = sortedTiles.get(i);
                CreateSlice cs = new CreateSlice(this, sacsGroups.get(group), slicingAxisPosition + i * axisIncrement);
                cs.runRequest();
                if (cs.getSlice() != null) {
                    out.add(cs.getSlice());
                }
            }
            new MarkActionSequenceBatch(this).runRequest();

        } else {
            CreateSlice cs = new CreateSlice(this, sacs, slicingAxisPosition);
            cs.runRequest();
            if (cs.getSlice() != null) {
                out.add(cs.getSlice());
            }
        }
        return out;
    }

    /**
     * Helper function to move a slice
     * @param slice
     * @param axisPosition
     */
    public void moveSlice(SliceSources slice, double axisPosition) {
        new MoveSlice(this, slice, axisPosition).runRequest();
    }

    public void exportSelectedSlicesRegionsToRoiManager(String namingChoice) {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(slice -> slice.isSelected()).collect(Collectors.toList());
        new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();

        for (SliceSources slice : sortedSelected) {
            exportSliceRegionsToRoiManager(slice, namingChoice);
        }

        new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
    }

    public void exportSelectedSlicesRegionsToQuPathProject(boolean erasePreviousFile) {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(slice -> slice.isSelected()).collect(Collectors.toList());
        new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();

        for (SliceSources slice : sortedSelected) {
            exportSliceRegionsToQuPathProject(slice, erasePreviousFile);
        }

        new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
    }

    public void exportSelectedSlicesRegionsToFile(String namingChoice, File dirOutput, boolean erasePreviousFile) {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(slice -> slice.isSelected()).collect(Collectors.toList());
        new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();

        for (SliceSources slice : sortedSelected) {
            exportSliceRegionsToFile(slice, namingChoice, dirOutput, erasePreviousFile);
        }

        new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
    }

    public void exportSliceRegionsToFile(SliceSources slice, String namingChoice, File dirOutput, boolean erasePreviousFile) {
        new ExportSliceRegionsToFile(this, slice, namingChoice, dirOutput, erasePreviousFile).runRequest();
    }

    public void exportSliceRegionsToRoiManager(SliceSources slice, String namingChoice) {
        new ExportSliceRegionsToRoiManager(this, slice, namingChoice).runRequest();
    }

    public void exportSliceRegionsToQuPathProject(SliceSources slice, boolean erasePreviousFile) {
        new ExportSliceRegionsToQuPathProject(this, slice, erasePreviousFile).runRequest();
    }

    /**
     * Equal spacing between selected slices
     */
    public void equalSpacingSelectedSlices() {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(slice -> slice.isSelected()).collect(Collectors.toList());
        if (sortedSelected.size() > 2) {
            SliceSources first = sortedSelected.get(0);
            SliceSources last = sortedSelected.get(sortedSelected.size() - 1);
            double totalSpacing = last.getSlicingAxisPosition() - first.getSlicingAxisPosition();
            double delta = totalSpacing / (sortedSelected.size() - 1);
            new MarkActionSequenceBatch(this).runRequest();
            for (int idx = 1; idx < sortedSelected.size() - 1; idx++) {
                moveSlice(sortedSelected.get(idx), first.getSlicingAxisPosition() + ((double) idx) * delta);
            }
            new MarkActionSequenceBatch(this).runRequest();
        }
    }

    /**
     * Overlap or not of the positioned slices
     */
    public void toggleOverlap() {
        cycleToggle++;
        if (cycleToggle == 3) cycleToggle = 0;
        updateDisplay();
        navigateCurrentSlice();
    }

    public static Function<SourceAndConverter[], SourceAndConverter[]> getChannel(int... channels) {
        return (sacs) -> {
            SourceAndConverter[] sacs_out = new SourceAndConverter[channels.length];
            for (int iChannel = 0 ; iChannel<channels.length ; iChannel++) {
                sacs_out[iChannel] = sacs[channels[iChannel]];
            }
            return sacs_out;
        };
    }

    public void registerElastixAffine(int iChannelFixed, int iChannelMoving) {
        registerElastixAffine(getChannel(iChannelFixed), getChannel(iChannelMoving));
    }

    public void registerBigWarp(int iChannelFixed, int iChannelMoving) {
        registerBigWarp(getChannel(iChannelFixed), getChannel(iChannelMoving));
    }

    public void registerElastixAffine(Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed,
                                      Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving) {
        if (getSelectedSources().size()==0) {
            log.accept("Registration ignored : no slice selected");
        }
        for (SliceSources slice : slices) {
            if (slice.isSelected()) {
                Elastix2DAffineRegistration elastixAffineReg = new Elastix2DAffineRegistration();
                elastixAffineReg.setScijavaContext(scijavaCtx);
                Map<String, Object> params = new HashMap<>();
                params.put("tpFixed", 0);
                params.put("levelFixedSource", 2);
                params.put("tpMoving", 0);
                params.put("levelMovingSource", slice.registered_sacs[0].getSpimSource().getNumMipmapLevels() - 1);
                params.put("pxSizeInCurrentUnit", 0.04);
                params.put("interpolate", false);
                params.put("showImagePlusRegistrationResult", false);// true);
                params.put("px", roiPX);//rpt.getDoublePosition(0));
                params.put("py", roiPY);//rpt.getDoublePosition(1));
                params.put("pz", slice.getSlicingAxisPosition());//rpt.getDoublePosition(2));
                params.put("sx", roiSX);
                params.put("sy", roiSY);
                elastixAffineReg.setScijavaParameters(params);
                new RegisterSlice(this, slice, elastixAffineReg, preprocessFixed, preprocessMoving).runRequest();
            }
        }
    }

    public void registerElastixSpline(int iChannelFixed, int iChannelMoving) {
        registerElastixSpline(getChannel(iChannelFixed), getChannel(iChannelMoving));
    }

    public void registerElastixSpline(Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed,
                                      Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving) {
        if (getSelectedSources().size()==0) {
            log.accept("Registration ignored : no slice selected");
        }
        for (SliceSources slice : slices) {
            if (slice.isSelected()) {
                Elastix2DSplineRegistration elastixSplineReg = new Elastix2DSplineRegistration();
                elastixSplineReg.setScijavaContext(scijavaCtx);
                Map<String, Object> params = new HashMap<>();
                params.put("tpFixed", 0);
                params.put("levelFixedSource", 1);
                params.put("tpMoving", 0);
                params.put("levelMovingSource", slice.registered_sacs[0].getSpimSource().getNumMipmapLevels() - 1);
                params.put("pxSizeInCurrentUnit", 0.02);
                params.put("interpolate", true);
                params.put("showImagePlusRegistrationResult", false);//true);
                params.put("px", roiPX);
                params.put("py", roiPY);
                params.put("pz", slice.getSlicingAxisPosition());
                params.put("sx", roiSX);
                params.put("sy", roiSY);
                elastixSplineReg.setScijavaParameters(params);
                new RegisterSlice(this, slice, elastixSplineReg, preprocessFixed, preprocessMoving).runRequest();
            }
        }
    }

    public void registerBigWarp(Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed,
                                Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving) {
        if (getSelectedSources().size()==0) {
            log.accept("Registration ignored : no slice selected");
        }
        for (SliceSources slice : slices) {
            if (slice.isSelected()) {
                SacBigWarp2DRegistration registration = new SacBigWarp2DRegistration();

                AffineTransform3D at3D = new AffineTransform3D();
                at3D.translate(-this.nPixX / 2.0, -this.nPixY / 2.0, 0);
                at3D.scale(this.sizePixX, this.sizePixY, this.sizePixZ);
                at3D.translate(0, 0, slice.getSlicingAxisPosition());

                // 0 - slicing model : empty source but properly defined in space and resolution
                SourceAndConverter singleSliceModel = new EmptySourceAndConverterCreator("SlicingModel", at3D,
                        nPixX,
                        nPixY,
                        1
                ).get();

                SourceResampler resampler = new SourceResampler(null,
                        singleSliceModel, false, false, false
                );

                AffineTransform3D translateZ = new AffineTransform3D();
                translateZ.translate(0, 0, -slice.getSlicingAxisPosition());

                new RegisterSlice(this, slice, registration, (fixedSacs) ->
                        Arrays.asList(preprocessFixed.apply(fixedSacs))
                                .stream()
                                .map(resampler)
                                .map(sac -> SourceTransformHelper.createNewTransformedSourceAndConverter(translateZ, new SourceAndConverterAndTimeRange(sac, 0)))
                                .collect(Collectors.toList())
                                .toArray(new SourceAndConverter[0]),
                        (movingSacs) ->
                            Arrays.asList(preprocessMoving.apply(movingSacs))
                                    .stream()
                                    .map(sac -> SourceTransformHelper.createNewTransformedSourceAndConverter(translateZ, new SourceAndConverterAndTimeRange(sac, 0)))
                                    .collect(Collectors.toList())
                                    .toArray(new SourceAndConverter[0])).runRequest();
            }
        }
    }

    // --------------------------------- ACTION CLASSES

    /**
     * Cancels last action
     */
    public void cancelLastAction() {
        if (userActions.size() > 0) {
            CancelableAction action = userActions.get(userActions.size() - 1);
            if (action instanceof MarkActionSequenceBatch) {
                action.cancelRequest();
                action = userActions.get(userActions.size() - 1);
                while (!(action instanceof MarkActionSequenceBatch)) {
                    action.cancelRequest();
                    action = userActions.get(userActions.size() - 1);
                }
                action.cancelRequest();
            } else {
                userActions.get(userActions.size() - 1).cancelRequest();
            }
        } else {
            log.accept("No action can be cancelled.");
        }
    }

    /**
     * Redo last action
     */
    public void redoAction() {
        if (redoableUserActions.size() > 0) {
            CancelableAction action = redoableUserActions.get(redoableUserActions.size() - 1);
            if (action instanceof MarkActionSequenceBatch) {
                action.runRequest();
                action = redoableUserActions.get(redoableUserActions.size() - 1);
                while (!(action instanceof MarkActionSequenceBatch)) {
                    action.runRequest();
                    action = redoableUserActions.get(redoableUserActions.size() - 1);
                }
                action.runRequest();
            } else {
                redoableUserActions.get(redoableUserActions.size() - 1).runRequest();
            }

        } else {
            log.accept("No action can be redone.");
        }
    }

    public volatile boolean drawActions = true;

    //------------------------------ Multipositioner Graphical handles

    Set<GraphicalHandle> ghs = new HashSet<>();

    Set<GraphicalHandle> gh_below_mouse = new HashSet<>();

    @Override
    public synchronized void hover_in(GraphicalHandle gh) {
        gh_below_mouse.add(gh);
        if (gh_below_mouse.size() == 1) {
            block();
        }
    }

    @Override
    public synchronized void hover_out(GraphicalHandle gh) {
        gh_below_mouse.remove(gh);
        if (gh_below_mouse.size() == 0) {
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
        for (SliceSources slice : slices)
            slice.ghs.forEach(gh -> gh.mouseMoved(e));
    }

    @Override
    public synchronized void created(GraphicalHandle gh) {

    }

    @Override
    public synchronized void removed(GraphicalHandle gh) {
        if (gh_below_mouse.contains(gh)) {
            gh_below_mouse.remove(gh);
            if (gh_below_mouse.size() == 0) unblock();
        }
        ghs.remove(gh);
    }

    //------------------------------------------ DRAG BEHAVIOURS
    /*
     * Stretching selected slices to the left
     */
    class DragLeft implements DragBehaviour {
        List<SliceSources> slicesDragged;
        Map<SliceSources, Double> initialAxisPositions = new HashMap<>();
        double range;
        double lastAxisPos;
        RealPoint iniPointBdv = new RealPoint(3);
        double iniSlicePointing;
        double iniSlicingAxisPosition;

        @Override
        public void init(int x, int y) {
            slicesDragged = getSortedSlices().stream().filter(slice -> slice.isSelected()).collect(Collectors.toList());

            slicesDragged.stream().forEach(slice -> {
                initialAxisPositions.put(slice, slice.slicingAxisPosition);
            });

            range = initialAxisPositions.get(slicesDragged.get(slicesDragged.size() - 1)) - initialAxisPositions.get(slicesDragged.get(0));
            lastAxisPos = initialAxisPositions.get(slicesDragged.get(slicesDragged.size() - 1));

            // Computes which slice it corresponds to (useful for overlay redraw)
            bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);
            iniSlicePointing = iniPointBdv.getDoublePosition(0) / sX;
            iniSlicingAxisPosition = (iniSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
        }

        @Override
        public void drag(int x, int y) {
            RealPoint currentMousePosition = new RealPoint(3);
            bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

            int currentSlicePointing = (int) (currentMousePosition.getDoublePosition(0) / sX);
            double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            double ratio = (lastAxisPos - currentSlicingAxisPosition) / range;

            for (SliceSources slice : slicesDragged) {
                slice.slicingAxisPosition = lastAxisPos + (initialAxisPositions.get(slice) - lastAxisPos) * ratio;
                slice.updatePosition();
            }

            updateDisplay();
        }

        @Override
        public void end(int x, int y) {
            RealPoint currentMousePosition = new RealPoint(3);
            bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

            int currentSlicePointing = (int) (currentMousePosition.getDoublePosition(0) / sX);
            double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            double ratio = (lastAxisPos - currentSlicingAxisPosition) / range;

            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
            for (SliceSources slice : slicesDragged) {
                slice.slicingAxisPosition = initialAxisPositions.get(slice);
                moveSlice(slice, lastAxisPos + (initialAxisPositions.get(slice) - lastAxisPos) * ratio);
            }
            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
            updateDisplay();
        }
    }

    /*
     * Stretching selected slices to the right
     */
    class DragRight implements DragBehaviour {

        List<SliceSources> slicesDragged;
        Map<SliceSources, Double> initialAxisPositions = new HashMap<>();
        double range;
        double lastAxisPos;
        RealPoint iniPointBdv = new RealPoint(3);
        double iniSlicePointing;
        double iniSlicingAxisPosition;

        @Override
        public void init(int x, int y) {
            slicesDragged = getSortedSlices().stream().filter(slice -> slice.isSelected()).collect(Collectors.toList());

            slicesDragged.stream().forEach(slice -> {
                initialAxisPositions.put(slice, slice.slicingAxisPosition);
            });

            range = initialAxisPositions.get(slicesDragged.get(slicesDragged.size() - 1)) - initialAxisPositions.get(slicesDragged.get(0));
            lastAxisPos = initialAxisPositions.get(slicesDragged.get(0));

            // Computes which slice it corresponds to (useful for overlay redraw)
            bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);
            iniSlicePointing = iniPointBdv.getDoublePosition(0) / sX;
            iniSlicingAxisPosition = (iniSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
        }

        @Override
        public void drag(int x, int y) {
            RealPoint currentMousePosition = new RealPoint(3);
            bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

            int currentSlicePointing = (int) (currentMousePosition.getDoublePosition(0) / sX);
            double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            double ratio = (lastAxisPos - currentSlicingAxisPosition) / range;

            for (SliceSources slice : slicesDragged) {
                slice.slicingAxisPosition = lastAxisPos - (initialAxisPositions.get(slice) - lastAxisPos) * ratio;
                slice.updatePosition();
            }

            updateDisplay();
        }

        @Override
        public void end(int x, int y) {
            RealPoint currentMousePosition = new RealPoint(3);
            bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

            int currentSlicePointing = (int) (currentMousePosition.getDoublePosition(0) / sX);
            double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            double ratio = (lastAxisPos - currentSlicingAxisPosition) / range;

            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
            for (SliceSources slice : slicesDragged) {
                slice.slicingAxisPosition = initialAxisPositions.get(slice);
                moveSlice(slice, lastAxisPos - (initialAxisPositions.get(slice) - lastAxisPos) * ratio);
            }
            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
            updateDisplay();
        }
    }

    /*
     * Single slice dragging
     */
    class SliceSourcesDrag implements DragBehaviour {

        Map<SliceSources, Double> initialAxisPositions = new HashMap<>();

        RealPoint iniPointBdv = new RealPoint(3);
        double iniSlicePointing;
        double iniSlicingAxisPosition;
        double deltaOrigin;
        boolean perform = false;

        final SliceSources sliceOrigin;

        public SliceSourcesDrag(SliceSources slice) {
            this.sliceOrigin = slice;
        }

        @Override
        public void init(int x, int y) {
            bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);

            // Computes which slice it corresponds to (useful for overlay redraw)
            iniSlicePointing = iniPointBdv.getDoublePosition(0) / sX + 0.5;
            iniSlicingAxisPosition = ((int) iniSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            if ((sliceOrigin.isSelected())) {
                perform = true;
                initialAxisPositions.put(sliceOrigin, sliceOrigin.getSlicingAxisPosition());
            } else {
                if (!sliceOrigin.isSelected()) {
                    sliceOrigin.select();
                    perform = false;
                }
            }

            if (currentMode != POSITIONING_MODE_INT) {
                perform = false;
            }

            // Initialize the delta on a step of the zStepper
            if (perform) {
                deltaOrigin = iniSlicingAxisPosition - sliceOrigin.getSlicingAxisPosition();
                if (initialAxisPositions.containsKey(sliceOrigin)) {
                    sliceOrigin.slicingAxisPosition = initialAxisPositions.get(sliceOrigin) + deltaOrigin;
                    sliceOrigin.updatePosition();
                }
            }

            updateDisplay();

        }

        @Override
        public void drag(int x, int y) {

            if (perform) {
                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                double currentSlicePointing = currentMousePosition.getDoublePosition(0) / sX;
                double currentSlicingAxisPosition = ((int) currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
                double deltaAxis = currentSlicingAxisPosition - iniSlicingAxisPosition;

                if (initialAxisPositions.containsKey(sliceOrigin)) {
                    sliceOrigin.slicingAxisPosition = initialAxisPositions.get(sliceOrigin) + deltaAxis + deltaOrigin;
                    sliceOrigin.updatePosition();
                }

                updateDisplay();
            }
        }

        @Override
        public void end(int x, int y) {
            if (perform) {
                {
                    RealPoint currentMousePosition = new RealPoint(3);
                    bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                    double currentSlicePointing = currentMousePosition.getDoublePosition(0) / sX;
                    double currentSlicingAxisPosition = ((int) currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
                    double deltaAxis = currentSlicingAxisPosition - iniSlicingAxisPosition;

                    if (initialAxisPositions.containsKey(sliceOrigin)) {
                        sliceOrigin.slicingAxisPosition = initialAxisPositions.get(sliceOrigin);
                        moveSlice(sliceOrigin, initialAxisPositions.get(sliceOrigin) + deltaAxis + deltaOrigin);
                    }
                    updateDisplay();
                }
            }
        }
    }

    public DragBehaviour getSelectedSourceDragBehaviour(SliceSources slice) {
        return new SliceSourcesDrag(slice);
    }

    /*
     * Drag selected sources
     */
    class SelectedSliceSourcesDrag implements DragBehaviour {

        Map<SliceSources, Double> initialAxisPositions = new HashMap<>();

        List<SliceSources> selectedSources = new ArrayList<>();

        RealPoint iniPointBdv = new RealPoint(3);
        double iniSlicePointing;
        double iniSlicingAxisPosition;
        double deltaOrigin;
        boolean perform = false;

        public SelectedSliceSourcesDrag(){ }

        @Override
        public void init(int x, int y) {
            bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);

            // Computes which slice it corresponds to (useful for overlay redraw)
            iniSlicePointing = iniPointBdv.getDoublePosition(0) / sX + 0.5;
            iniSlicingAxisPosition = ((int) iniSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            selectedSources = getSortedSlices().stream().filter(slice -> slice.isSelected()).collect(Collectors.toList());
            if ((selectedSources.size() > 0)) {// && (sliceOrigin.isSelected())) {
                perform = true;
                selectedSources.stream().forEach(slice -> {
                    initialAxisPositions.put(slice, slice.slicingAxisPosition);
                });
            }

            if (currentMode != POSITIONING_MODE_INT) {
                perform = false;
            }

            // Initialize the delta on a step of the zStepper
            if (perform) {
                deltaOrigin = 0;
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

                double currentSlicePointing = currentMousePosition.getDoublePosition(0) / sX;
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
                {

                    RealPoint currentMousePosition = new RealPoint(3);
                    bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                    double currentSlicePointing = currentMousePosition.getDoublePosition(0) / sX;
                    double currentSlicingAxisPosition = ((int) currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
                    double deltaAxis = currentSlicingAxisPosition - iniSlicingAxisPosition;

                    new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
                    for (SliceSources slice : selectedSources) {
                        if (initialAxisPositions.containsKey(slice)) {
                            slice.slicingAxisPosition = initialAxisPositions.get(slice);
                            moveSlice(slice, initialAxisPositions.get(slice) + deltaAxis + deltaOrigin);
                        }
                    }
                    new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();

                    updateDisplay();
                }

            }
        }
    }

    // ------------------------------------------- Block map

    /*
     * Create BehaviourMap to block behaviours interfering with
     * DragBehaviours. The block map is only active if the mouse hovers over any of the Graphical handle
     */
    private final BehaviourMap blockMap = new BehaviourMap();

    private void block() {
        bdvh.getTriggerbindings().addBehaviourMap(BLOCKING_MAP, blockMap);
    }

    private void unblock() {
        bdvh.getTriggerbindings().removeBehaviourMap(BLOCKING_MAP);
    }

    private static final String[] DRAG_TOGGLE_EDITOR_KEYS = new String[]{"button1"};

    private void refreshBlockMap() {
        bdvh.getTriggerbindings().removeBehaviourMap(BLOCKING_MAP);

        final Set<InputTrigger> moveCornerTriggers = new HashSet<>();
        for (final String s : DRAG_TOGGLE_EDITOR_KEYS)
            moveCornerTriggers.add(InputTrigger.getFromString(s));

        final Map<InputTrigger, Set<String>> bindings = bdvh.getTriggerbindings().getConcatenatedInputTriggerMap().getAllBindings();
        final Set<String> behavioursToBlock = new HashSet<>();
        for (final InputTrigger t : moveCornerTriggers)
            behavioursToBlock.addAll(bindings.get(t));

        blockMap.clear();
        final Behaviour block = new Behaviour() {
        };
        for (final String key : behavioursToBlock)
            blockMap.put(key, block);
    }

    // ------------------------------------------------ Serialization / Deserialization
    /*
        Register the RealTransformAdapters from Bdv-playground
     */
    void registerTransformAdapters(final GsonBuilder gsonbuilder) {
        // AffineTransform3D seroalization
        gsonbuilder.registerTypeAdapter(AffineTransform3D.class, new AffineTransform3DAdapter());

        // Realtransforms see package sc.fiji.bdvpg.services.serializers.plugins;
        Map<Class, List<Class>> runTimeAdapters = new HashMap<>();
        scijavaCtx.getService(BdvPlaygroundObjectAdapterService.class)
                .getAdapters(IClassRuntimeAdapter.class)
                .forEach(pi -> {
                            try {
                                IClassRuntimeAdapter adapter = pi.createInstance();
                                if (runTimeAdapters.containsKey(adapter.getBaseClass())) {
                                    runTimeAdapters.get(adapter.getBaseClass()).add(adapter.getRunTimeClass());
                                } else {
                                    List<Class> subClasses = new ArrayList<>();
                                    subClasses.add(adapter.getRunTimeClass());
                                    runTimeAdapters.put(adapter.getBaseClass(), subClasses);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                );

        scijavaCtx.getService(BdvPlaygroundObjectAdapterService.class)
                .getAdapters(IClassRuntimeAdapter.class)
                .forEach(pi -> {
                    try {
                        IClassRuntimeAdapter adapter = pi.createInstance();
                        if (adapter.getBaseClass().equals(RealTransform.class)) {
                            gsonbuilder.registerTypeHierarchyAdapter(adapter.getRunTimeClass(), adapter);
                        }
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                    }
                });


        log.accept("IRunTimeClassAdapters : ");
        runTimeAdapters.keySet().forEach(baseClass -> {
            if (baseClass.equals(RealTransform.class)) {
                log.accept("\t " + baseClass);
                RuntimeTypeAdapterFactory factory = RuntimeTypeAdapterFactory.of(baseClass);
                runTimeAdapters.get(baseClass).forEach(subClass -> {
                    factory.registerSubtype(subClass);
                    log.accept("\t \t " + subClass);
                });
                gsonbuilder.registerTypeAdapterFactory(factory);
            }
        });
    }

    Gson getGsonStateSerializer(List<SourceAndConverter> serialized_sources) {
        GsonBuilder gsonbuider = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(SourceAndConverter.class, new IndexedSourceAndConverterAdapter(serialized_sources))
                .registerTypeAdapter(SourceAndConverter[].class, new IndexedSourceAndConverterArrayAdapter(serialized_sources));

        // Now gets all custom serializers for RealTransform.class, using Scijava extensibility plugin
        // Most of the adapters comes from Bdv-Playground

        registerTransformAdapters(gsonbuider);

        // For actions serialization
        RuntimeTypeAdapterFactory factoryActions = RuntimeTypeAdapterFactory.of(CancelableAction.class);

        factoryActions.registerSubtype(CreateSlice.class);
        factoryActions.registerSubtype(MoveSlice.class);
        factoryActions.registerSubtype(RegisterSlice.class);

        gsonbuider.registerTypeAdapterFactory(factoryActions);
        gsonbuider.registerTypeHierarchyAdapter(CreateSlice.class, new CreateSliceAdapter(this));
        gsonbuider.registerTypeHierarchyAdapter(MoveSlice.class, new MoveSliceAdapter(this, this::currentSliceGetter));
        gsonbuider.registerTypeHierarchyAdapter(RegisterSlice.class, new RegisterSliceAdapter(this, this::currentSliceGetter));

        // For registration registration
        RuntimeTypeAdapterFactory factoryRegistrations = RuntimeTypeAdapterFactory.of(Registration.class);

        factoryRegistrations.registerSubtype(Elastix2DAffineRegistration.class);
        factoryRegistrations.registerSubtype(Elastix2DSplineRegistration.class);
        factoryRegistrations.registerSubtype(SacBigWarp2DRegistration.class);

        gsonbuider.registerTypeAdapterFactory(factoryRegistrations);
        gsonbuider.registerTypeHierarchyAdapter(Elastix2DAffineRegistration.class, new Elastix2DAffineRegistrationAdapter());
        gsonbuider.registerTypeHierarchyAdapter(Elastix2DSplineRegistration.class, new Elastix2DSplineRegistrationAdapter());
        gsonbuider.registerTypeHierarchyAdapter(SacBigWarp2DRegistration.class, new SacBigWarp2DRegistrationAdapter());
        gsonbuider.registerTypeHierarchyAdapter(AlignerState.SliceSourcesState.class, new SliceSourcesStateDeserializer((slice) -> currentSerializedSlice = slice));

        return gsonbuider.create();
    }

    public void saveState(File stateFile, boolean overwrite) {

        // Wait patiently for all tasks to be performed
        this.getSortedSlices().forEach(slice -> {
            slice.waitForEndOfTasks();
        });

        synchronized (this) {

            // First save all sources required in the state
            List<SourceAndConverter> allSacs = new ArrayList<>();

            this.getSortedSlices().forEach(sliceSource -> {
                allSacs.addAll(Arrays.asList(sliceSource.getOriginalSources()));
            });

            String fileNoExt = FilenameUtils.removeExtension(stateFile.getAbsolutePath());
            File sacsFile = new File(fileNoExt+"_sources.json");

            if (sacsFile.exists()&&(!overwrite)) {
                System.err.println("File "+sacsFile.getAbsolutePath()+" already exists. Abort command");
                return;
            }

            SourceAndConverterServiceSaver sacss = new SourceAndConverterServiceSaver(sacsFile,this.scijavaCtx,allSacs);
            sacss.run();
            List<SourceAndConverter> serialized_sources = new ArrayList<>();

            sacss.getSacToId().values().stream().sorted().forEach(i -> {
                System.out.println(i);
                serialized_sources.add(sacss.getIdToSac().get(i));
            });

            try {
                FileWriter writer = new FileWriter(stateFile.getAbsolutePath());
                getGsonStateSerializer(serialized_sources).toJson(new AlignerState(this), writer);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    volatile private SliceSources currentSerializedSlice = null;

    SliceSources currentSliceGetter() {
        return currentSerializedSlice;
    }

    public void loadState(File stateFile) {
        // TODO : add a clock as an overlay
        this.getSortedSlices().forEach(slice -> {
            slice.waitForEndOfTasks();
        });

        String fileNoExt = FilenameUtils.removeExtension(stateFile.getAbsolutePath());
        File sacsFile = new File(fileNoExt+"_sources.json");

        if (!sacsFile.exists()) {
            errlog.accept("File "+sacsFile.getAbsolutePath()+" not found!");
            return;
        }

        SourceAndConverterServiceLoader sacsl = new SourceAndConverterServiceLoader(sacsFile.getAbsolutePath(), sacsFile.getParent(), this.scijavaCtx, false);
        sacsl.run();
        List<SourceAndConverter> serialized_sources = new ArrayList<>();

        sacsl.getSacToId().values().stream().sorted().forEach(i -> {
            serialized_sources.add(sacsl.getIdToSac().get(i));
        });

        Gson gson = getGsonStateSerializer(serialized_sources);

        if (stateFile.exists()) {
            try {

                FileReader fileReader = new FileReader(stateFile);

                AlignerState state = gson.fromJson(fileReader, AlignerState.class); // actions are executed during deserialization
                fileReader.close();

                state.slices_state_list.forEach(sliceState -> {
                    sliceState.slice.waitForEndOfTasks();
                    sliceState.slice.setVisible(sliceState.isVisible);
                    sliceState.slice.setDisplaysettings(sliceState.settings_per_channel);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            errlog.accept("Error : file "+stateFile.getAbsolutePath()+" not found!");
        }

    }

}