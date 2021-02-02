package ch.epfl.biop.atlas.aligner;

import bdv.util.*;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.ResourcesMonitor;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.aligner.commands.*;
import ch.epfl.biop.atlas.aligner.serializers.*;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.*;
import ch.epfl.biop.bdv.select.SourceSelectorBehaviour;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.sourceandconverter.affine.Elastix2DAffineRegistration;
import ch.epfl.biop.registration.sourceandconverter.spline.Elastix2DSplineRegistration;
import ch.epfl.biop.registration.sourceandconverter.spline.SacBigWarp2DRegistration;
import ch.epfl.biop.scijava.command.Elastix2DAffineRegisterCommand;
import ch.epfl.biop.scijava.command.Elastix2DAffineRegisterServerCommand;
import ch.epfl.biop.scijava.command.Elastix2DSplineRegisterServerCommand;
import ch.epfl.biop.scijava.ui.bdv.BdvScijavaHelper;
import ch.epfl.biop.scijava.ui.swing.ScijavaSwingUI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.Tile;
import net.imglib2.*;
import net.imglib2.converter.Converter;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.cache.CacheService;
import org.scijava.object.ObjectService;
import org.scijava.ui.behaviour.*;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.behaviour.EditorBehaviourUnInstaller;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServiceLoader;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.services.serializers.AffineTransform3DAdapter;
import sc.fiji.bdvpg.services.serializers.RuntimeTypeAdapterFactory;
import sc.fiji.bdvpg.services.serializers.plugins.BdvPlaygroundObjectAdapterService;
import sc.fiji.bdvpg.services.serializers.plugins.IClassRuntimeAdapter;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bdv.ui.BdvDefaultCards.*;
import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * All specific methods dedicated to the multislice positioner
 *
 * Let's think a bit:
 * There will be:
 * - a positioning mode
 * - a review mode
 * - a 3d view mode (todo)
 */

public class MultiSlicePositioner extends BdvOverlay implements  GraphicalHandleListener, MouseMotionListener { // SelectedSourcesListener,

    //
    static public final Object manualActionLock = new Object();

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

    private List<SliceSources> slices = Collections.synchronizedList(new ArrayList<>());

    protected List<CancelableAction> userActions = new ArrayList<>();

    protected List<CancelableAction> redoableUserActions = new ArrayList<>();

    /**
     * Current coordinate where Sources are dragged
     */
    int iSliceNoStep;

    Context scijavaCtx;

    public MultiSliceObserver mso;

    public int displayMode = POSITIONING_MODE_INT;

    public final static int POSITIONING_MODE_INT = 0;
    final static String POSITIONING_MODE = "positioning-mode";
    final static String POSITIONING_BEHAVIOURS_KEY = POSITIONING_MODE + "-behaviours";
    Behaviours positioning_behaviours = new Behaviours(new InputTriggerConfig(), POSITIONING_MODE);

    public final static int REGISTRATION_MODE_INT = 1;
    final static String REGISTRATION_MODE = "Registration";
    final static String REGISTRATION_BEHAVIOURS_KEY = REGISTRATION_MODE + "-behaviours";
    Behaviours registration_behaviours = new Behaviours(new InputTriggerConfig(), REGISTRATION_MODE);

    int sliceDisplayMode = ALL_SLICES_DISPLAY_MODE;

    final public static int NO_SLICE_DISPLAY_MODE = 2; // For faster draw when restoring
    final public static int CURRENT_SLICE_DISPLAY_MODE = 1;
    final public static int ALL_SLICES_DISPLAY_MODE = 0;

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

    public Consumer<String> log = (message) -> {
        System.out.println("Multipositioner : "+message);
        getBdvh().getViewerPanel().showMessage(message);
    };

    public BiConsumer<String, String> errorMessageForUser = (title, message) -> {
        JOptionPane.showMessageDialog(new JFrame(), message, title,
                JOptionPane.ERROR_MESSAGE);
    };

    public BiConsumer<String, String> warningMessageForUser = (title, message) -> {
        JOptionPane.showMessageDialog(new JFrame(), message, title,
                JOptionPane.WARNING_MESSAGE);
    };

    public Consumer<String> errlog = (message) -> {
        System.err.println("Multipositioner : "+message);
        errorMessageForUser.accept("Error", message);
    };

    public Consumer<String> debuglog = (message) -> {};//System.err.println("Multipositioner Debug : "+message);

    public List<SliceSources> getSlices() {
        return new ArrayList<>(slices);
    }

    protected List<SliceSources> getPrivateSlices() {
        return slices;
    }

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

        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.changeSliceDisplayMode(), "toggle_single_source_mode", "S");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.cancelLastAction(), "cancel_last_action", "ctrl Z", "meta Z");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.redoAction(), "redo_last_action", "ctrl Y", "ctrl shift Z", "meta Y", "ctrl meta Z");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.navigateNextSlice(), "navigate_next_slice", "RIGHT");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.navigatePreviousSlice(), "navigate_previous_slice",  "LEFT"); // P taken for panel
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.navigateCurrentSlice(), "navigate_current_slice", "C");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.nextMode(), "change_mode", "R");

        bdvh.getTriggerbindings().addBehaviourMap(COMMON_BEHAVIOURS_KEY, common_behaviours.getBehaviourMap());
        bdvh.getTriggerbindings().addInputTriggerMap(COMMON_BEHAVIOURS_KEY, common_behaviours.getInputTriggerMap()); // "transform", "bdv"

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
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> this.stretchRightSelectedSlices(), "stretch_selectedslices_right", "ctrl RIGHT", "meta RIGHT");
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> this.shrinkRightSelectedSlices(), "shrink_selectedslices_right", "ctrl LEFT", "meta LEFT");
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> this.stretchLeftSelectedSlices(), "stretch_selectedslices_left", "ctrl shift LEFT", "meta shift LEFT");
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> this.shrinkLeftSelectedSlices(), "shrink_selectedslices_left", "ctrl shift RIGHT", "meta shift RIGHT");
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> this.shiftUpSelectedSlices(), "shift_selectedslices_up", "ctrl UP", "meta UP");
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> this.shiftDownSelectedSlices(), "shift_selectedslices_down", "ctrl DOWN", "meta DOWN");
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> {slices.forEach(SliceSources::select);bdvh.getViewerPanel().getDisplay().repaint();}, "selectAllSlices", "ctrl A", "meta A");
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> {slices.forEach(SliceSources::deSelect);bdvh.getViewerPanel().getDisplay().repaint();}, "deselectAllSlices", "ctrl shift A", "meta shift A");

        List<SourceAndConverter<?>> sacsToAppend = new ArrayList<>();
        for (int i = 0; i < biopAtlas.map.getStructuralImages().size(); i++) {
            sacsToAppend.add(reslicedAtlas.extendedSlicedSources[i]);
            sacsToAppend.add(reslicedAtlas.nonExtendedSlicedSources[i]);
        }

        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .show(bdvh, sacsToAppend.toArray(new SourceAndConverter[0]));

        bdvh.getViewerPanel().getDisplay().addHandler(this);

        SquareGraphicalHandle ghRight = new SquareGraphicalHandle(this, new DragRight(), "drag_right", "button1", bdvh.getTriggerbindings(),
                () -> new Integer[]{rightPosition[0]+25, rightPosition[1], rightPosition[2]}, () -> 25, () -> new Integer[]{255, 0, 255, 200});

        GraphicalHandleToolTip ghRightToolTip = new GraphicalHandleToolTip(ghRight, "Ctrl + \u25C4 \u25BA ",0,20);

        CircleGraphicalHandle ghCenter = new CircleGraphicalHandle(this, new SelectedSliceSourcesDrag(), "translate_selected", "button1", bdvh.getTriggerbindings(),
                () -> new Integer[]{(leftPosition[0]+rightPosition[0])/2,(leftPosition[1]+rightPosition[1])/2, 0}, () -> 25, () -> new Integer[]{255, 0, 255, 200});

        GraphicalHandleToolTip ghCenterToolTip = new GraphicalHandleToolTip(ghCenter, "Ctrl + \u25B2 \u25BC ",0,0);

        SquareGraphicalHandle ghLeft = new SquareGraphicalHandle(this, new DragLeft(), "drag_right", "button1", bdvh.getTriggerbindings(),
                () -> new Integer[]{leftPosition[0]-25, leftPosition[1], leftPosition[2]}, () -> 25, () -> new Integer[]{255, 0, 255, 200});

        GraphicalHandleToolTip ghLeftToolTip = new GraphicalHandleToolTip(ghLeft, "Ctrl + Shift + \u25C4 \u25BA ", 0,-20);

        stretchRight = ghRight;
        stretchLeft = ghLeft;
        center = ghCenter;

        ghs.add(stretchRight);
        ghs_tool_tip.add(ghRightToolTip);
        ghs_tool_tip.add(ghCenterToolTip);
        ghs_tool_tip.add(ghLeftToolTip);
        ghs.add(center);
        ghs.add(stretchLeft);

        this.bdvh.getCardPanel().setCardExpanded("Sources", false);
        this.bdvh.getCardPanel().setCardExpanded("Groups", false);

        reslicedAtlas.addListener(() -> {
            recenterBdvh();
            updateDisplay();
        });

        previouszStep = (int) reslicedAtlas.getStep();

        mso = new MultiSliceObserver(this);

        //bdvh.getCardPanel().removeCard(DEFAULT_SOURCES_CARD); // Cannot do this : errors
        bdvh.getCardPanel().removeCard(DEFAULT_SOURCEGROUPS_CARD);
        bdvh.getCardPanel().removeCard(DEFAULT_VIEWERMODES_CARD);
        bdvh.getCardPanel().setCardExpanded(DEFAULT_SOURCES_CARD, false);

        BdvScijavaHelper.clearBdvHandleMenuBar(bdvh);

        int hierarchyLevelsSkipped = 4;

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, MSPStateLoadCommand.class, hierarchyLevelsSkipped,"mp", this );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, MSPStateSaveCommand.class, hierarchyLevelsSkipped,"mp", this);

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Undo [Ctrl+Z]",0, this::cancelLastAction);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Redo [Ctrl+Shift+Z]",0, this::redoAction);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Select all slices [Ctrl+A]",0,() -> slices.forEach(SliceSources::select));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Deselect all slices [Ctrl+Shift+A]",0,() -> slices.forEach(SliceSources::deSelect));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Distribute spacing [A]",0,() -> {
            if (this.displayMode == POSITIONING_MODE_INT) this.equalSpacingSelectedSlices();
        });

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>Positioning Mode",0, this::setPositioningMode);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>Review Mode",0, this::setRegistrationMode);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>Change Slice Display Mode [S]",0, this::changeSliceDisplayMode);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>Change Overlap Mode [O]",0, this::toggleOverlap);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>Show Atlas Position On Mouse",0, this::showAtlasPosition);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>Hide Atlas Position On Mouse",0, this::hideAtlasPosition);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>Next Slice [Right]",0, this::navigateNextSlice);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>Previous Slice [Left]",0, this::navigatePreviousSlice);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>Center On Current Slice [C]",0, this::navigateCurrentSlice);

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, ImportQuPathProjectCommand.class, hierarchyLevelsSkipped,"mp", this );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, ImportImagePlusCommand.class, hierarchyLevelsSkipped,"mp", this );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, RegistrationElastixAffineCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, RegistrationElastixSplineCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, RegistrationElastixAffineRemoteCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, RegistrationElastixSplineRemoteCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, RegistrationBigWarpCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, EditLastRegistrationCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, ExportRegionsToFileCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, ExportRegionsToRoiManagerCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, ExportRegionsToQuPathCommand.class, hierarchyLevelsSkipped,"mp", this);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, scijavaCtx, RotateSourcesCommand.class, hierarchyLevelsSkipped,"mp", this);


        // TODO BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Registration>Remove Last Registration",0,() -> );

        AtlasDisplayPanel adp = new AtlasDisplayPanel(this);
        // Hide useless channels on startup -
        adp.getModel().setValueAt(new Boolean(false),0,8); // X Coord
        adp.getModel().setValueAt(new Boolean(false),0,10);// Y Coord
        adp.getModel().setValueAt(new Boolean(false),0,12);// Z Coord
        adp.getModel().setValueAt(new Boolean(false),0,14);// Left Right
        adp.getModel().setValueAt(new Boolean(false),0,16);// Label ?

        bdvh.getCardPanel().addCard("Atlas Display Options", adp.getPanel(), true);

        bdvh.getCardPanel().addCard("Slices Display Options", new SliceDisplayPanel(this).getPanel(), true);

        bdvh.getCardPanel().addCard("Edit Slices", new EditPanel(this).getPanel(), true);

        bdvh.getCardPanel().addCard("Atlas Slicing",
                ScijavaSwingUI.getPanel(scijavaCtx, SlicerAdjusterInteractiveCommand.class, "reslicedAtlas", reslicedAtlas),
                true);

        bdvh.getCardPanel().addCard("Define region of interest",
                ScijavaSwingUI.getPanel(scijavaCtx, RectangleROIDefineInteractiveCommand.class, "mp", this),
                false);

        bdvh.getCardPanel().addCard("Tasks Info", mso.getJPanel(), false);

        bdvh.getCardPanel().addCard("Resources Monitor", new ResourcesMonitor(), false);

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

            if (displayMode == POSITIONING_MODE_INT) {
                if (Math.IEEEremainder(px+sX*0.5, sX) < roiPX) {val.set(255); return;}
                if (Math.IEEEremainder(px+sX*0.5, sX) > roiPX+roiSX) {val.set(255); return;}
                if (py<roiPY) {val.set(255); return;}
                if (py>roiPY+roiSY) {val.set(255); return;}
                val.set(0);
            }

            if (displayMode == REGISTRATION_MODE_INT) {
                if (loc.getFloatPosition(0) < roiPX) {val.set(255); return;}
                if (loc.getFloatPosition(0) > roiPX+roiSX) {val.set(255); return;}
                if (loc.getFloatPosition(1)<roiPY) {val.set(255); return;}
                if (loc.getFloatPosition(1)>roiPY+roiSY) {val.set(255); return;}
                val.set(0);
            }

        };

        FunctionRealRandomAccessible<UnsignedShortType> roiOverlay = new FunctionRealRandomAccessible<>(3, fun, UnsignedShortType::new);

        BdvStackSource<?> bss = BdvFunctions.show(roiOverlay,
                new FinalInterval(new long[]{0, 0, 0}, new long[]{10, 10, 10}),"ROI", BdvOptions.options().addTo(bdvh));

        bss.setDisplayRangeBounds(0,1600);
        displayMode = REGISTRATION_MODE_INT; // For correct toggling
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

    boolean showAtlasPosition = true;

    public void showAtlasPosition() {
        showAtlasPosition = true;
    }

    public void hideAtlasPosition() {
        showAtlasPosition = false;
    }

    void addRightClickActions() {
        common_behaviours.behaviour(new MultiSliceContextMenuClickBehaviour( this, this::getSelectedSources ), "Slices Context Menu", "button3", "ctrl button1", "meta button1");
    }

    public void changeSliceDisplayMode() {
        sliceDisplayMode = 1-sliceDisplayMode;
        setSliceDisplayMode(sliceDisplayMode);
    }

    public void setSliceDisplayMode(int newSliceDisplayMode) {
        modeListeners.forEach(l -> l.sliceDisplayModeChanged(this, this.sliceDisplayMode,newSliceDisplayMode));
        this.sliceDisplayMode = newSliceDisplayMode;
        getSlices().forEach(slice -> slice.getGUIState().sliceDisplayModeChanged());
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
        //itm.put(InputTrigger.getFromString("ctrl UP"), "zoom in slow");
        itm.put(InputTrigger.getFromString("scroll"), "scroll zoom");

        itm.put(InputTrigger.getFromString("DOWN"), "zoom out");
        itm.put(InputTrigger.getFromString("shift DOWN"), "zoom out fast");
        //itm.put(InputTrigger.getFromString("ctrl DOWN"), "zoom out slow");

        selectionLayer = new SelectionLayer(this);
        selectionLayer.addSelectionBehaviours(common_behaviours);
        refreshBlockMap();

        bdvh.getTriggerbindings().addInputTriggerMap("default_navigation", itm, "transform");
    }

    public void recenterBdvh() {
        double cur_wcx = bdvh.getViewerPanel().getWidth() / 2.0; // Current Window Center X
        double cur_wcy = bdvh.getViewerPanel().getHeight() / 2.0; // Current Window Center Y

        RealPoint centerScreenCurrentBdv = new RealPoint(cur_wcx, cur_wcy, 0);
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

        RealPoint centerScreenNextBdv = new RealPoint(next_wcx, next_wcy, 0);
        RealPoint shiftNextBdv = new RealPoint(3);

        nextAffineTransform.inverse().apply(centerScreenNextBdv, shiftNextBdv);

        double sx = -centerScreenGlobalCoord.getDoublePosition(0) + shiftNextBdv.getDoublePosition(0);
        double sy = -centerScreenGlobalCoord.getDoublePosition(1) + shiftNextBdv.getDoublePosition(1);
        double sz = -centerScreenGlobalCoord.getDoublePosition(2) + shiftNextBdv.getDoublePosition(2);

        RealPoint shiftWindow = new RealPoint(sx, sy, sz);
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
     * @return the list of slices present, sorted along the z axis
     */
    public List<SliceSources> getSortedSlices() {
        List<SliceSources> sortedSlices = new ArrayList<>(slices);
        sortedSlices.sort(Comparator.comparingDouble(SliceSources::getSlicingAxisPosition));
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
        switch (displayMode) {
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
        if (!(displayMode == POSITIONING_MODE_INT)) {
            int oldMode = displayMode;
            reslicedAtlas.unlock();
            displayMode = POSITIONING_MODE_INT;
            ghs.forEach(GraphicalHandle::enable);
            slices.forEach(slice -> slice.getGUIState().enableGraphicalHandles());
            getSortedSlices().forEach(slice -> slice.getGUIState().displayModeChanged());

            bdvh.getTriggerbindings().removeInputTriggerMap(REGISTRATION_BEHAVIOURS_KEY);
            bdvh.getTriggerbindings().removeBehaviourMap(REGISTRATION_BEHAVIOURS_KEY);
            positioning_behaviours.install(bdvh.getTriggerbindings(), POSITIONING_BEHAVIOURS_KEY);
            navigateCurrentSlice();
            refreshBlockMap();

            modeListeners.forEach(ml -> ml.modeChanged(this, oldMode, displayMode));
        }
    }

    public void hide(SliceSources slice) {
        slice.getGUIState().setSliceInvisible();
    }

    public void show(SliceSources slice) {
        slice.getGUIState().setSliceVisible();
    }

    /**
     * Set the registration mode
     */
    public void setRegistrationMode() {
        if (!(displayMode == REGISTRATION_MODE_INT)) {
            int oldMode = REGISTRATION_MODE_INT;
            displayMode = POSITIONING_MODE_INT;
            reslicedAtlas.lock();
            displayMode = REGISTRATION_MODE_INT;

            ghs.forEach(GraphicalHandle::disable);

            //synchronized (slices) {
                getSortedSlices().forEach(slice -> slice.getGUIState().displayModeChanged());
            //}

            bdvh.getTriggerbindings().removeInputTriggerMap(POSITIONING_BEHAVIOURS_KEY);
            bdvh.getTriggerbindings().removeBehaviourMap(POSITIONING_BEHAVIOURS_KEY);
            registration_behaviours.install(bdvh.getTriggerbindings(), REGISTRATION_BEHAVIOURS_KEY);
            navigateCurrentSlice();
            refreshBlockMap();
            modeListeners.forEach(ml -> ml.modeChanged(this, oldMode, displayMode));
        }
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

                SliceSources previousSlice = null;
                if ((previousSliceIndex>=0)&&(previousSliceIndex<sortedSlices.size())) {
                    previousSlice = sortedSlices.get(previousSliceIndex);
                }
                centerBdvViewOn(sortedSlices.get(iCurrentSlice), true, previousSlice);
                //centerBdvViewOn(sortedSlices.get(iCurrentSlice), true);
                if ((previousSliceIndex>=0)&&(previousSliceIndex<sortedSlices.size())) { // Could have been deleted
                    sortedSlices.get(previousSliceIndex).getGUIState().isNotCurrent();
                }
                sortedSlices.get(iCurrentSlice).getGUIState().isCurrent();
            }
    }

    /**
     * Defines, in physical units, the region that will be used to perform the automated registration
     * @param px center of the region, x axis
     * @param py center of the region, y axis
     * @param sx size of the region, x axis
     * @param sy size of the region, y axis
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

    public SliceSources getCurrentSlice() {
        List<SliceSources> sortedSlices = getSortedSlices();

            if (sortedSlices.size()>0) {
            if (iCurrentSlice >= sortedSlices.size()) {
                iCurrentSlice = 0;
            }
            return sortedSlices.get(iCurrentSlice);
        } else {
            return null;
        }
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
            SliceSources previousSlice = null;
            if ((previousSliceIndex>=0)&&(previousSliceIndex<sortedSlices.size())) {
                previousSlice = sortedSlices.get(previousSliceIndex);
            }
            centerBdvViewOn(sortedSlices.get(iCurrentSlice), true, previousSlice);
            if ((previousSliceIndex>=0)&&(previousSliceIndex<sortedSlices.size())) { // Could have been deleted
                sortedSlices.get(previousSliceIndex).getGUIState().isNotCurrent();
            }
            sortedSlices.get(iCurrentSlice).getGUIState().isCurrent();
        }
    }

    public void selectSlice(SliceSources... slices) {
        for (SliceSources slice : slices) {
            slice.select();
        }
    }

    public void selectSlice(List<SliceSources> slices) {
        selectSlice(slices.toArray(new SliceSources[0]));
    }

    public void deselectSlice(SliceSources... slices) {
        for (SliceSources slice : slices) {
            slice.deSelect();
        }
    }

    public void deselectSlice(List<SliceSources> slices) {
        deselectSlice(slices.toArray(new SliceSources[0]));
    }

    public void waitForTasks() {
        slices.forEach(SliceSources::waitForEndOfTasks);
    }

    public void centerBdvViewOn(SliceSources slice) {
        centerBdvViewOn(slice, false, null);
    }

    /**
     * Center bdv on a slice
     *
     */
    public void centerBdvViewOn(SliceSources current_slice, boolean maintainoffset, SliceSources previous_slice) {

        RealPoint offset = new RealPoint(3);

        if ((maintainoffset)&&(previous_slice!=null)) {

                RealPoint oldCenter = new RealPoint(3);

                if (displayMode == MultiSlicePositioner.POSITIONING_MODE_INT) {
                    oldCenter = previous_slice.getGUIState().getCenterPositionPMode();
                } else if (displayMode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
                    oldCenter = previous_slice.getGUIState().getCenterPositionRMode();
                }

                RealPoint centerScreen = getCurrentBdvCenter();
                offset.setPosition(-oldCenter.getDoublePosition(0) + centerScreen.getDoublePosition(0), 0);
                offset.setPosition(-oldCenter.getDoublePosition(1) + centerScreen.getDoublePosition(1), 1);
                offset.setPosition(-oldCenter.getDoublePosition(2) + centerScreen.getDoublePosition(2), 2);

                if (Math.abs(offset.getDoublePosition(0))>sX/2.0) {maintainoffset = false;}
                if (Math.abs(offset.getDoublePosition(1))>sY/2.0) {maintainoffset = false;}

        } else {
            maintainoffset = false;
        }

        RealPoint centerSlice = new RealPoint(3);

        if (displayMode == MultiSlicePositioner.POSITIONING_MODE_INT) {
            centerSlice = current_slice.getGUIState().getCenterPositionPMode();
        } else if (displayMode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
            centerSlice = current_slice.getGUIState().getCenterPositionRMode();
        }

        if(maintainoffset) {
            centerSlice.move(offset);
        }
        AffineTransform3D at3d = BdvHandleHelper.getViewerTransformWithNewCenter(bdvh, centerSlice.positionAsDoubleArray());

        bdvh.getViewerPanel().state().setViewerTransform(at3d);
        bdvh.getViewerPanel().requestRepaint();

    }

    RealPoint getCurrentBdvCenter() {
        RealPoint centerBdv = new RealPoint(3);

        double px = bdvh.getViewerPanel().getDisplay().getWidth() / 2.0;
        double py = bdvh.getViewerPanel().getDisplay().getHeight() / 2.0;
        bdvh.getViewerPanel().displayToGlobalCoordinates(px,py,centerBdv);

        return centerBdv;
    }

    protected void updateDisplay() {
        // Sort slices along slicing axis
        if (overlapMode == 0) {
            slices.forEach(slice -> slice.getGUIState().setYShift(1));
        } else if (overlapMode == 2) {

            double lastPositionAlongX = -Double.MAX_VALUE;

            int stairIndex = 0;

            for (SliceSources slice : getSortedSlices()) {
                double posX = slice.getGUIState().getCenterPositionPMode().getDoublePosition(0);
                if (posX >= (lastPositionAlongX + sX)) {
                    stairIndex = 0;
                    lastPositionAlongX = posX;
                    slice.getGUIState().setYShift(1);
                } else {
                    stairIndex++;
                    slice.getGUIState().setYShift(1 + stairIndex);
                }
            }
        } else if (overlapMode == 1) {
            slices.forEach(slice -> slice.getGUIState().setYShift(0));
        }

        bdvh.getViewerPanel().requestRepaint();
    }

    int overlapMode = 0;

    @Override
    protected void draw(Graphics2D g) {
        List<SliceSources> slicesCopy = getSlices();

        int colorCode = this.info.getColor().get();
        Color color = new Color(ARGBType.red(colorCode), ARGBType.green(colorCode), ARGBType.blue(colorCode), ARGBType.alpha(colorCode));
        g.setColor(color);

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

        slicesCopy.forEach(slice -> slice.getGUIState().drawGraphicalHandles(g));

        g.setColor(color);

        if (iCurrentSlice != -1 && slicesCopy.size() > iCurrentSlice) {
            SliceSources slice = getSortedSlices().get(iCurrentSlice);
            listeners.forEach(listener -> listener.isCurrentSlice(slice));
            g.setColor(new Color(255, 255, 255, 128));
            Integer[] coords = slice.getGUIState().getBdvHandleCoords();
            RealPoint sliceCenter = new RealPoint(coords[0], coords[1], 0);
            g.drawOval((int) sliceCenter.getDoublePosition(0) - 15, (int) sliceCenter.getDoublePosition(1) - 15, 30, 30);
            // "Ctrl + \u25C4 \u25BA "
            Integer[] c = {255,255,255,128};//color.get();
            g.setColor(new Color(c[0], c[1], c[2], c[3]));
            g.setFont(new Font("TimesRoman", Font.PLAIN, 16));
            g.drawString("\u25C4 \u25BA", (int) (sliceCenter.getDoublePosition(0) - 15), (int) (sliceCenter.getDoublePosition(1) - 20));

        }

        if ((displayMode == POSITIONING_MODE_INT) && slicesCopy.stream().anyMatch(SliceSources::isSelected)) {
            List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
            RealPoint precedentPoint = null;

            for (int i = 0; i < sortedSelected.size(); i++) {
                SliceSources slice = sortedSelected.get(i);

                Integer[] coords = slice.getGUIState().getBdvHandleCoords();
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
                    RealPoint handleLeftPoint = slice.getGUIState().getCenterPositionPMode();
                    handleLeftPoint.setPosition(-sY, 1);
                    bdvAt3D.apply(handleLeftPoint, handleLeftPoint);

                    leftPosition[0] = (int) handleLeftPoint.getDoublePosition(0);
                    leftPosition[1] = (int) handleLeftPoint.getDoublePosition(1) - 50;
                }

                if (i == sortedSelected.size() - 1) {
                    RealPoint handleRightPoint = slice.getGUIState().getCenterPositionPMode();
                    handleRightPoint.setPosition(-sY, 1);
                    bdvAt3D.apply(handleRightPoint, handleRightPoint);

                    rightPosition[0] = (int) handleRightPoint.getDoublePosition(0);
                    rightPosition[1] = (int) handleRightPoint.getDoublePosition(1) - 50;
                }
            }

            if (sortedSelected.size() > 1) {
                center.enable();
                stretchLeft.enable();
                stretchRight.enable();
                ghs.forEach(GraphicalHandle::enable);
                g.setColor(new Color(255, 0, 255, 200));
                g.drawLine(leftPosition[0], leftPosition[1], rightPosition[0], rightPosition[1]);
            } else if (sortedSelected.size() == 1) {
                center.enable();//ghs.forEach(GraphicalHandle::enable);
                stretchLeft.disable();
                stretchRight.disable();
                g.setColor(new Color(255, 0, 255, 200));
                g.drawLine(leftPosition[0], leftPosition[1], rightPosition[0], rightPosition[1]);
            } else {
                ghs.forEach(GraphicalHandle::disable);
            }
            ghs.forEach(gh -> gh.draw(g));
            ghs_tool_tip.forEach(gh -> gh.draw(g));
        }

        if (selectionLayer != null) {
            selectionLayer.draw(g);
        }

        if (mso != null) {
            mso.draw(g);
        }

        if (showAtlasPosition) {
            RealPoint globalMouseCoordinates = new RealPoint(3);
            bdvh.getViewerPanel().getGlobalMouseCoordinates(globalMouseCoordinates);
            int labelValue;
            int leftRight;
            float[] coords = new float[3];
            if (displayMode==POSITIONING_MODE_INT) {
                SourceAndConverter label = reslicedAtlas.extendedSlicedSources[reslicedAtlas.extendedSlicedSources.length-1]; // By convention the label image is the last one
                labelValue = ((UnsignedShortType) getSourceValueAt(label, globalMouseCoordinates)).get();
                SourceAndConverter lrSource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.extendedSlicedSources.length-2]; // By convention the left right indicator image is the next to last one
                leftRight = ((UnsignedShortType) getSourceValueAt(lrSource, globalMouseCoordinates)).get();

                SourceAndConverter xSource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.extendedSlicedSources.length-5]; // (bad) convention TODO : safer indexing
                SourceAndConverter ySource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.extendedSlicedSources.length-4]; // By convention the left right indicator image is the next to last one
                SourceAndConverter zSource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.extendedSlicedSources.length-3]; // By convention the left right indicator image is the next to last one

                coords[0] = ((FloatType) getSourceValueAt(xSource, globalMouseCoordinates)).get();
                coords[1] = ((FloatType) getSourceValueAt(ySource, globalMouseCoordinates)).get();
                coords[2] = ((FloatType) getSourceValueAt(zSource, globalMouseCoordinates)).get();
            } else {
                assert displayMode == REGISTRATION_MODE_INT;
                SourceAndConverter label = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.nonExtendedSlicedSources.length-1]; // By convention the label image is the last one
                labelValue = ((UnsignedShortType) getSourceValueAt(label, globalMouseCoordinates)).get();
                SourceAndConverter lrSource = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.nonExtendedSlicedSources.length-2]; // By convention the left right indicator image is the next to last one
                leftRight = ((UnsignedShortType) getSourceValueAt(lrSource, globalMouseCoordinates)).get();

                SourceAndConverter xSource = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.nonExtendedSlicedSources.length-5]; // (bad) convention TODO : safer indexing
                SourceAndConverter ySource = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.nonExtendedSlicedSources.length-4]; // By convention the left right indicator image is the next to last one
                SourceAndConverter zSource = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.nonExtendedSlicedSources.length-3]; // By convention the left right indicator image is the next to last one

                coords[0] = ((FloatType) getSourceValueAt(xSource, globalMouseCoordinates)).get();
                coords[1] = ((FloatType) getSourceValueAt(ySource, globalMouseCoordinates)).get();
                coords[2] = ((FloatType) getSourceValueAt(zSource, globalMouseCoordinates)).get();
            }

            DecimalFormat df = new DecimalFormat("#0.00");
            String coordinates = "["+df.format(coords[0])+";"+df.format(coords[1])+";"+df.format(coords[2])+"]";
            if (leftRight == 255) {
                coordinates += "(R)";
            }
            if (leftRight == 0) {
                coordinates += "(L)";
            }
            String ontologyLocation = null;
            if (labelValue!=0) {
                ontologyLocation = biopAtlas.ontology.getProperties(labelValue).get("acronym");
                while (labelValue!=biopAtlas.ontology.getRootIndex()) {
                    labelValue = biopAtlas.ontology.getParent(labelValue);
                    if (labelValue!=biopAtlas.ontology.getRootIndex())
                    ontologyLocation = ontologyLocation+"<"+biopAtlas.ontology.getProperties(labelValue).get("acronym");
                }

            }

            g.setFont(new Font("TimesRoman", Font.BOLD, 16));
            g.setColor(new Color(255, 255, 100, 250));
            Point mouseLocation = bdvh.getViewerPanel().getMousePosition();
            if ((ontologyLocation!=null)&&(mouseLocation!=null)) {
                g.drawString(ontologyLocation,mouseLocation.x,mouseLocation.y);
            }
            if ((mouseLocation!=null)&&(!coordinates.startsWith("[0.00;0.00;0.00]"))) {
                g.drawString(coordinates, mouseLocation.x, mouseLocation.y - 20);
            }
        }

    }

    Object getSourceValueAt(SourceAndConverter sac, RealPoint pt) {
        RealRandomAccessible rra_ible = sac.getSpimSource().getInterpolatedSource(0, 0, Interpolation.NEARESTNEIGHBOR);
        if (rra_ible != null) {
            AffineTransform3D sourceTransform = new AffineTransform3D();
            sac.getSpimSource().getSourceTransform(0, 0, sourceTransform);
            RealRandomAccess rra = rra_ible.realRandomAccess();
            RealPoint iPt = new RealPoint(3);
            sourceTransform.inverse().apply(pt, iPt);
            rra.setPosition(iPt);
            return rra.get();
        } else {
            return null;
        }
    }

    public String getUndoMessage() {
        if (userActions.size()==0) {
            return "(None)";
        } else {
            CancelableAction lastAction = userActions.get(userActions.size()-1);
            if (lastAction instanceof MarkActionSequenceBatch) {
                CancelableAction lastlastAction = userActions.get(userActions.size()-2);
                return "("+lastlastAction.actionClassString()+" [batch])";
            } else {
                return "("+lastAction.actionClassString()+")";
            }
        }
    }

    public String getRedoMessage() {
        if (redoableUserActions.size()==0) {
            return "(None)";
        } else {
            CancelableAction lastAction = redoableUserActions.get(redoableUserActions.size()-1);
            if (lastAction instanceof MarkActionSequenceBatch) {
                CancelableAction lastlastAction = redoableUserActions.get(redoableUserActions.size()-2);
                return "("+lastlastAction.actionClassString()+" [batch])";
            } else {
                return "("+lastAction.actionClassString()+")";
            }
        }
    }

    public void rotateSlices(int axis, double angle_rad) {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        for (SliceSources slice : sortedSelected) {
            slice.rotateSourceOrigin(axis, angle_rad);
        }
        bdvh.getViewerPanel().requestRepaint();
    }

    public int getDisplayMode() {
        return displayMode;
    }

    public void setDisplayMode(int mode) {
        switch (mode) {
            case REGISTRATION_MODE_INT :
                setRegistrationMode();
                break;
            case POSITIONING_MODE_INT :
                setPositioningMode();
                break;
            default:
                errlog.accept("Unknown display mode "+mode);
        }
    }

    public int getSliceDisplayMode() {
        return sliceDisplayMode;
    }

    public void showAllSlices() {
        for (SliceSources slice : getSortedSlices()) {
            slice.getGUIState().setSliceVisible();
        }
    }

    public void showCurrentSlice() {
        getSortedSlices().get(iCurrentSlice).getGUIState().setSliceVisible();
    }

    protected void removeSlice(SliceSources sliceSource) {
        listeners.forEach(listener -> {
            System.out.println(listener);
            listener.sliceDeleted(sliceSource);
        });
        slices.remove(sliceSource);
        sliceSource.getGUIState().sliceDeleted();
    }

    public boolean isCurrentSlice(SliceSources slice) {
        List<SliceSources> sortedSlices = getSortedSlices();
        if (iCurrentSlice >= sortedSlices.size()) {
            iCurrentSlice = 0;
        }

        if (sortedSlices.size() > 0) {
            return slice.equals(sortedSlices.get(iCurrentSlice));
        } else {
            return false;
        }
    }

    protected void createSlice(SliceSources sliceSource) {
        slices.add(sliceSource);
        listeners.forEach(listener -> listener.sliceCreated(sliceSource));
    }

    public void positionZChanged(SliceSources slice) {
        listeners.forEach(listener -> listener.sliceZPositionChanged(slice));
    }

    public void sliceVisibilityChanged(SliceSources slice) {
        listeners.forEach(listener -> listener.sliceVisibilityChanged(slice));
    }

    public void sliceSelected(SliceSources slice) {
        listeners.forEach(listener -> listener.sliceSelected(slice));
    }

    public void sliceDeselected(SliceSources slice) {
        listeners.forEach(listener -> listener.sliceDeselected(slice));
    }

    List<ModeListener> modeListeners = new ArrayList<>();

    public void addModeListener(ModeListener modeListener) {
        modeListeners.add(modeListener);
    }

    public void removeModeListener(ModeListener modeListener) {
        modeListeners.remove(modeListener);
    }

    public BiopAtlas getAtlas() {
        return biopAtlas;
    }

    public int getNumberOfAtlasChannels() {
        return reslicedAtlas.nonExtendedSlicedSources.length;
    }

    public int getNumberOfSelectedSources() {
        return getSelectedSources().size();
    }

    public int getChannelBoundForSelectedSlices() {
        List<SliceSources> sources = getSelectedSources();
        if (sources.size()==0) {
            return 0;
        } else {
            return sources.stream()
                    .mapToInt(slice -> slice.nChannels)
                    .min().getAsInt();
        }
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
         * @param support weird stuff for swing drag and drop TODO : link proper documentation
         * @param sacs list of source and converter to import
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
            // Split based on attribute

            Map<T, List<SourceAndConverter<?>>> sacsGroups =
                    sacs.stream().collect(Collectors.groupingBy(sac -> {
                        if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac, SPIM_DATA_INFO) != null) {
                            SourceAndConverterService.SpimDataInfo sdi = (SourceAndConverterService.SpimDataInfo) SourceAndConverterServices.getSourceAndConverterService().getMetadata(sac, SPIM_DATA_INFO);
                            AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>> asd = (AbstractSpimData<AbstractSequenceDescription<BasicViewSetup, ?, ?>>) sdi.asd;
                            BasicViewSetup bvs = asd.getSequenceDescription().getViewSetups().get(sdi.setupId);
                            return bvs.getAttribute(attributeClass);
                        } else {
                            return defaultEntity;
                        }
                    }));

            List<T> sortedTiles = new ArrayList<>(sacsGroups.keySet());

            sortedTiles.sort(Comparator.comparingInt(T::getId));

            new MarkActionSequenceBatch(this).runRequest();
            for (int i = 0; i < sortedTiles.size(); i++) {
                T group = sortedTiles.get(i);
                if (group.getId()!=-1) {
                    CreateSlice cs = new CreateSlice(this, sacsGroups.get(group), slicingAxisPosition + i * axisIncrement);
                    cs.runRequest();
                    if (cs.getSlice() != null) {
                        out.add(cs.getSlice());
                    }
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
     * @param slice the slice you want to move
     * @param axisPosition the axis at which it should be
     */
    public void moveSlice(SliceSources slice, double axisPosition) {
        new MoveSlice(this, slice, axisPosition).runRequest();
    }

    public void exportSelectedSlicesRegionsToRoiManager(String namingChoice) {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
        for (SliceSources slice : sortedSelected) {
            exportSliceRegionsToRoiManager(slice, namingChoice);
        }
        new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
    }

    public void exportSelectedSlicesRegionsToQuPathProject(boolean erasePreviousFile) {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
        for (SliceSources slice : sortedSelected) {
            exportSliceRegionsToQuPathProject(slice, erasePreviousFile);
        }
        new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
    }

    public void exportSelectedSlicesRegionsToFile(String namingChoice, File dirOutput, boolean erasePreviousFile) {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
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
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
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
     * Stretch right spacing between selected slices
     */
    public void stretchRightSelectedSlices() {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        double startAxis = sortedSelected.get(0).getSlicingAxisPosition();
        double endAxis = sortedSelected.get(sortedSelected.size()-1).getSlicingAxisPosition();
        double range = endAxis - startAxis;
        if (range!=0) {
            double stepSize = sizePixX * (int) reslicedAtlas.getStep();
            double ratio = (range + stepSize) / range;
            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
            for (SliceSources slice : sortedSelected) {
                double dist = slice.getSlicingAxisPosition() - startAxis;
                moveSlice(slice,startAxis + dist * ratio );
            }
            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
        }
    }

    /**
     * Stretch right spacing between selected slices
     */
    public void shrinkRightSelectedSlices() {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        double startAxis = sortedSelected.get(0).getSlicingAxisPosition();
        double endAxis = sortedSelected.get(sortedSelected.size()-1).getSlicingAxisPosition();
        double range = endAxis - startAxis;
        if (range!=0) {
            double stepSize = sizePixX * (int) reslicedAtlas.getStep();
            double ratio = (range - stepSize) / range;
            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
            for (SliceSources slice : sortedSelected) {
                double dist = slice.getSlicingAxisPosition() - startAxis;
                moveSlice(slice,startAxis + dist * ratio );
            }
            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
        }
    }

    /**
     * Stretch right spacing between selected slices
     */
    public void stretchLeftSelectedSlices() {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        double startAxis = sortedSelected.get(0).getSlicingAxisPosition();
        double endAxis = sortedSelected.get(sortedSelected.size()-1).getSlicingAxisPosition();
        double range = endAxis - startAxis;
        if (range!=0) {
            double stepSize = sizePixX * (int) reslicedAtlas.getStep();
            double ratio = (range + stepSize) / range;
            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
            for (SliceSources slice : sortedSelected) {
                double dist = endAxis - slice.getSlicingAxisPosition();
                moveSlice(slice,endAxis - dist * ratio );
            }
            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
        }
    }

    /**
     * Stretch right spacing between selected slices
     */
    public void shrinkLeftSelectedSlices() {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        double startAxis = sortedSelected.get(0).getSlicingAxisPosition();
        double endAxis = sortedSelected.get(sortedSelected.size()-1).getSlicingAxisPosition();
        double range = endAxis - startAxis;
        if (range!=0) {
            double stepSize = sizePixX * (int) reslicedAtlas.getStep();
            double ratio = (range - stepSize) / range;
            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
            for (SliceSources slice : sortedSelected) {
                double dist = endAxis - slice.getSlicingAxisPosition();
                moveSlice(slice,endAxis - dist * ratio );
            }
            new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
        }
    }

    /**
     * Shift position of all slices to the right (further along the slicing axis)
     */
    public void shiftUpSelectedSlices() {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (sortedSelected.size()>1) new MarkActionSequenceBatch(this).runRequest();
        double shift = sizePixX * (int) reslicedAtlas.getStep();
        for (int idx = 0; idx < sortedSelected.size(); idx++) {
            SliceSources slice = sortedSelected.get(idx);
            moveSlice(slice, slice.getSlicingAxisPosition() + shift);
        }
        if (sortedSelected.size()>1) new MarkActionSequenceBatch(this).runRequest();
    }

    /**
     * Shift position of all slices to the right (nearer along the slicing axis)
     */
    public void shiftDownSelectedSlices() {
        List<SliceSources> sortedSelected = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (sortedSelected.size()>1) new MarkActionSequenceBatch(this).runRequest();
        double shift = sizePixX * (int) reslicedAtlas.getStep();
        for (int idx = 0; idx < sortedSelected.size(); idx++) {
            SliceSources slice = sortedSelected.get(idx);
            moveSlice(slice, slice.getSlicingAxisPosition() - shift);
        }
        if (sortedSelected.size()>1) new MarkActionSequenceBatch(this).runRequest();
    }

    /**
     * Overlap or not of the positioned slices
     */
    public void toggleOverlap() {
        overlapMode++;
        if (overlapMode == 3) overlapMode = 0;
        updateDisplay();
        navigateCurrentSlice();
    }

    private void setOverlapMode(int overlapMode) {
        if (overlapMode<0) overlapMode=0;
        if (overlapMode>2) overlapMode=2;
        this.overlapMode = overlapMode;
        updateDisplay();
        navigateCurrentSlice();
    }

    public int getOverlapMode() {
        return overlapMode;
    }

    public static SourcesProcessor getChannel(int... channels) {
        return new SourcesChannelsSelect(channels);
    }

    public void editLastRegistration() {
        if (getSelectedSources().size()==0) {
            warningMessageForUser.accept("No selected slice", "Please select the slice you want to edit");
            log.accept("Edit registration ignored : no slice selected");
        } else {
            for (SliceSources slice : slices) {
                if (slice.isSelected()) {
                    new EditLastRegistration(this, slice).runRequest();
                }
            }
        }
    }

    public void registerBigWarp(int iChannelFixed, int iChannelMoving) {
        registerBigWarp(getChannel(iChannelFixed), getChannel(iChannelMoving));
    }

    public void registerBigWarp(SourcesProcessor preprocessFixed,
                                SourcesProcessor preprocessMoving) {
        if (getSelectedSources().size()==0) {
            warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
            log.accept("Registration ignored : no slice selected");
        }
        for (SliceSources slice : slices) {
            if (slice.isSelected()) {
                SacBigWarp2DRegistration registration = new SacBigWarp2DRegistration();

                AffineTransform3D at3D = new AffineTransform3D();
                at3D.translate(-this.nPixX / 2.0, -this.nPixY / 2.0, 0);
                at3D.scale(this.sizePixX, this.sizePixY, this.sizePixZ);
                at3D.translate(0, 0, slice.getSlicingAxisPosition());

                AffineTransform3D translateZ = new AffineTransform3D();
                translateZ.translate(0, 0, -slice.getSlicingAxisPosition());
                SourcesProcessor fixedProcess = SourcesProcessorHelper.compose(
                        new SourcesAffineTransformer(translateZ),
                        preprocessFixed
                );
                SourcesProcessor movingProcess = SourcesProcessorHelper.compose(
                        new SourcesAffineTransformer(translateZ),
                        preprocessMoving
                );

                new RegisterSlice(this, slice, registration, fixedProcess, movingProcess).runRequest();
            }
        }
    }

    public void registerElastixAffine(int iChannelFixed, int iChannelMoving, boolean showIJ1Result) {
        registerElastixAffine(getChannel(iChannelFixed), getChannel(iChannelMoving), showIJ1Result);
    }

    public void registerElastixAffine(SourcesProcessor preprocessFixed,
                                      SourcesProcessor preprocessMoving, boolean showIJ1Result) {
        if (getSelectedSources().size()==0) {
            warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
            log.accept("Registration ignored : no slice selected");
        }
        for (SliceSources slice : slices) {
            if (slice.isSelected()) {
                Elastix2DAffineRegistration elastixAffineReg = new Elastix2DAffineRegistration();
                elastixAffineReg.setRegistrationCommand(Elastix2DAffineRegisterCommand.class);
                elastixAffineReg.setScijavaContext(scijavaCtx);
                Map<String, Object> params = new HashMap<>();
                params.put("tpFixed", 0);
                params.put("levelFixedSource", 2);
                params.put("tpMoving", 0);
                params.put("levelMovingSource", slice.getAdaptedMipMapLevel(0.04));
                params.put("pxSizeInCurrentUnit", 0.04);
                params.put("interpolate", false);
                params.put("showImagePlusRegistrationResult", showIJ1Result);
                params.put("px", roiPX);
                params.put("py", roiPY);
                params.put("pz", slice.getSlicingAxisPosition());
                params.put("sx", roiSX);
                params.put("sy", roiSY);
                elastixAffineReg.setScijavaParameters(params);
                new RegisterSlice(this, slice, elastixAffineReg, preprocessFixed, preprocessMoving).runRequest();
            }
        }
    }

    public void registerElastixSpline(int iChannelFixed, int iChannelMoving, int nbControlPointsX, boolean showIJ1Result) {
        registerElastixSpline(getChannel(iChannelFixed), getChannel(iChannelMoving), nbControlPointsX,  showIJ1Result);
    }

    public void registerElastixSpline(SourcesProcessor preprocessFixed,
                                      SourcesProcessor preprocessMoving, int nbControlPointsX, boolean showIJ1Result) {
        if (getSelectedSources().size()==0) {
            warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
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
                params.put("levelMovingSource", slice.getAdaptedMipMapLevel(0.02));
                params.put("pxSizeInCurrentUnit", 0.02);
                params.put("interpolate", true);
                params.put("showImagePlusRegistrationResult", showIJ1Result);
                params.put("px", roiPX);
                params.put("py", roiPY);
                params.put("pz", 0);
                params.put("sx", roiSX);
                params.put("sy", roiSY);
                params.put("nbControlPointsX", nbControlPointsX);
                elastixSplineReg.setScijavaParameters(params);

                AffineTransform3D at3d = new AffineTransform3D();
                at3d.translate(0,0,-slice.getSlicingAxisPosition());
                SourcesAffineTransformer z_zero = new SourcesAffineTransformer(at3d);

                new RegisterSlice(this, slice, elastixSplineReg, SourcesProcessorHelper.compose(z_zero, preprocessFixed), SourcesProcessorHelper.compose(z_zero, preprocessMoving)).runRequest();
            }
        }
    }

    public void registerElastixAffineRemote(String serverURL, int iChannelFixed, int iChannelMoving) {
        registerElastixAffineRemote(serverURL, getChannel(iChannelFixed), getChannel(iChannelMoving));
    }

    public void registerElastixAffineRemote(String serverURL, SourcesProcessor preprocessFixed,
                                      SourcesProcessor preprocessMoving) {
        if (getSelectedSources().size()==0) {
            warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
            log.accept("Registration ignored : no slice selected");
        }
        for (SliceSources slice : slices) {
            if (slice.isSelected()) {
                Elastix2DAffineRegistration elastixAffineReg = new Elastix2DAffineRegistration();
                elastixAffineReg.setRegistrationCommand(Elastix2DAffineRegisterServerCommand.class);
                elastixAffineReg.setScijavaContext(scijavaCtx);
                Map<String, Object> params = new HashMap<>();
                params.put("tpFixed", 0);
                params.put("levelFixedSource", 2);
                params.put("tpMoving", 0);
                params.put("levelMovingSource", slice.getAdaptedMipMapLevel(0.04));
                params.put("pxSizeInCurrentUnit", 0.04);
                params.put("interpolate", false);
                params.put("showImagePlusRegistrationResult", false);
                params.put("px", roiPX);
                params.put("py", roiPY);
                params.put("pz", slice.getSlicingAxisPosition());
                params.put("sx", roiSX);
                params.put("sy", roiSY);
                params.put("serverURL", serverURL);
                params.put("taskInfo", "");
                elastixAffineReg.setScijavaParameters(params);
                new RegisterSlice(this, slice, elastixAffineReg, preprocessFixed, preprocessMoving).runRequest();
            }
        }
    }

    public void registerElastixSplineRemote(String serverURL, int iChannelFixed, int iChannelMoving, int nbControlPointsX) {
        registerElastixSplineRemote(serverURL, getChannel(iChannelFixed), getChannel(iChannelMoving), nbControlPointsX);
    }

    public void registerElastixSplineRemote(String serverURL, SourcesProcessor preprocessFixed,
                                      SourcesProcessor preprocessMoving, int nbControlPointsX) {
        if (getSelectedSources().size()==0) {
            warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
            log.accept("Registration ignored : no slice selected");
        }
        for (SliceSources slice : slices) {
            if (slice.isSelected()) {
                Elastix2DSplineRegistration elastixSplineReg = new Elastix2DSplineRegistration();
                elastixSplineReg.setScijavaContext(scijavaCtx);
                elastixSplineReg.setRegistrationCommand(Elastix2DSplineRegisterServerCommand.class);
                Map<String, Object> params = new HashMap<>();
                params.put("tpFixed", 0);
                params.put("levelFixedSource", 1);
                params.put("tpMoving", 0);
                params.put("levelMovingSource", slice.getAdaptedMipMapLevel(0.02));
                params.put("pxSizeInCurrentUnit", 0.02);
                params.put("interpolate", true);
                params.put("showImagePlusRegistrationResult", false);
                params.put("px", roiPX);
                params.put("py", roiPY);
                params.put("pz", 0);
                params.put("sx", roiSX);
                params.put("sy", roiSY);
                params.put("nbControlPointsX", nbControlPointsX);
                params.put("serverURL", serverURL);
                params.put("taskInfo", "");
                elastixSplineReg.setScijavaParameters(params);

                AffineTransform3D at3d = new AffineTransform3D();
                at3d.translate(0,0,-slice.getSlicingAxisPosition());
                SourcesAffineTransformer z_zero = new SourcesAffineTransformer(at3d);

                new RegisterSlice(this, slice, elastixSplineReg, SourcesProcessorHelper.compose(z_zero, preprocessFixed), SourcesProcessorHelper.compose(z_zero, preprocessMoving)).runRequest();
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

    GraphicalHandle stretchLeft, center, stretchRight;

    Set<GraphicalHandle> ghs_tool_tip = new HashSet<>();

    Set<GraphicalHandle> gh_below_mouse = new HashSet<>();

    @Override
    public void disabled(GraphicalHandle gh) {

    }

    @Override
    public void enabled(GraphicalHandle gh) {

    }

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
        this.slices.forEach(slice -> slice.getGUIState().ghs.forEach(gh -> gh.mouseDragged(e)));
    }

    @Override
    public synchronized void mouseMoved(MouseEvent e) {
        this.ghs.forEach(gh -> gh.mouseMoved(e));
        slices.forEach(slice -> slice.getGUIState().ghs.forEach(gh -> gh.mouseMoved(e)));
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

    AtomicBoolean dragActionInProgress = new AtomicBoolean(false);

    synchronized boolean startDragAction() {
        if (dragActionInProgress.get()) {
            return false;
        } else {
            dragActionInProgress.set(true);
            return true;
        }
    }

    synchronized void stopDragAction() {
        dragActionInProgress.set(false);
    }

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
        boolean perform;

        @Override
        public void init(int x, int y) {
            debuglog.accept(" DragLeft start ("+x+":"+y+")");
            perform = startDragAction();

            if (perform) {
                slicesDragged = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

                slicesDragged.forEach(slice -> initialAxisPositions.put(slice, slice.getSlicingAxisPosition()));

                range = initialAxisPositions.get(slicesDragged.get(slicesDragged.size() - 1)) - initialAxisPositions.get(slicesDragged.get(0));

                lastAxisPos = initialAxisPositions.get(slicesDragged.get(slicesDragged.size() - 1));

                // Computes which slice it corresponds to (useful for overlay redraw)
                bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);
                iniSlicePointing = iniPointBdv.getDoublePosition(0) / sX;
                iniSlicingAxisPosition = (iniSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
            }
            debuglog.accept(" DragLeft perform : ("+perform+")");
        }

        @Override
        public void drag(int x, int y) {
            if (perform) {
                debuglog.accept(" DragLeft drag (" + x + ":" + y + ")");
                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                int currentSlicePointing = (int) (currentMousePosition.getDoublePosition(0) / sX);
                double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

                double ratio;

                if (range == 0) {
                    ratio = 1.0 / (slicesDragged.size()-1);
                    for (SliceSources slice : slicesDragged) {
                        slice.setSlicingAxisPosition(lastAxisPos + (currentSlicingAxisPosition - lastAxisPos) * ratio * slicesDragged.indexOf(slice));
                    }
                } else {
                    ratio = (lastAxisPos - currentSlicingAxisPosition) / range;
                    for (SliceSources slice : slicesDragged) {
                        slice.setSlicingAxisPosition(lastAxisPos + (initialAxisPositions.get(slice) - lastAxisPos) * ratio);
                    }
                }

                updateDisplay();
            }
        }

        @Override
        public void end(int x, int y) {
            if (perform) {
                debuglog.accept(" DragLeft end (" + x + ":" + y + ")");
                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                int currentSlicePointing = (int) (currentMousePosition.getDoublePosition(0) / sX);
                double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

                double ratio;

                new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
                if (range == 0) {
                    ratio = 1.0 / (slicesDragged.size()-1);
                    for (SliceSources slice : slicesDragged) {
                        slice.setSlicingAxisPosition(initialAxisPositions.get(slice));
                        moveSlice(slice, lastAxisPos + (currentSlicingAxisPosition - lastAxisPos) * ratio * slicesDragged.indexOf(slice));
                    }
                } else {
                    ratio = (lastAxisPos - currentSlicingAxisPosition) / range;
                    for (SliceSources slice : slicesDragged) {
                        slice.setSlicingAxisPosition(initialAxisPositions.get(slice));
                        moveSlice(slice, lastAxisPos + (initialAxisPositions.get(slice) - lastAxisPos) * ratio);
                    }
                }
                new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();

                updateDisplay();
                stopDragAction();
            }
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
        boolean perform;

        @Override
        public void init(int x, int y) {

            debuglog.accept(" DragRight start ("+x+":"+y+")");
            perform = startDragAction();
            if (perform) {
                slicesDragged = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());

                slicesDragged.forEach(slice -> initialAxisPositions.put(slice, slice.getSlicingAxisPosition()));

                range = initialAxisPositions.get(slicesDragged.get(slicesDragged.size() - 1)) - initialAxisPositions.get(slicesDragged.get(0));
                lastAxisPos = initialAxisPositions.get(slicesDragged.get(0));

                // Computes which slice it corresponds to (useful for overlay redraw)
                bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);
                iniSlicePointing = iniPointBdv.getDoublePosition(0) / sX;
                iniSlicingAxisPosition = (iniSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
            }
        }

        @Override
        public void drag(int x, int y) {
            if (perform) {
                debuglog.accept(" DragRight drag (" + x + ":" + y + ")");

                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                int currentSlicePointing = (int) (currentMousePosition.getDoublePosition(0) / sX);
                double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

                double ratio;

                if (range == 0) {
                    ratio = 1.0 / (slicesDragged.size()-1);
                    for (SliceSources slice : slicesDragged) {
                        slice.setSlicingAxisPosition(lastAxisPos + (currentSlicingAxisPosition - lastAxisPos) * ratio * slicesDragged.indexOf(slice));
                    }
                } else {
                    ratio = (lastAxisPos - currentSlicingAxisPosition) / range;
                    for (SliceSources slice : slicesDragged) {
                        slice.setSlicingAxisPosition(lastAxisPos - (initialAxisPositions.get(slice) - lastAxisPos) * ratio);
                    }
                }


                updateDisplay();
            }
        }

        @Override
        public void end(int x, int y) {
            if (perform) {
                debuglog.accept(" DragRight end (" + x + ":" + y + ")");

                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                int currentSlicePointing = (int) (currentMousePosition.getDoublePosition(0) / sX);
                double currentSlicingAxisPosition = (currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

                double ratio;

                new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
                if (range == 0) {
                    ratio = 1.0 / (slicesDragged.size()-1);
                    for (SliceSources slice : slicesDragged) {
                        slice.setSlicingAxisPosition(initialAxisPositions.get(slice));
                        moveSlice(slice, lastAxisPos + (currentSlicingAxisPosition - lastAxisPos) * ratio * slicesDragged.indexOf(slice));
                    }
                } else {
                    ratio = (lastAxisPos - currentSlicingAxisPosition) / range;
                    for (SliceSources slice : slicesDragged) {
                        slice.setSlicingAxisPosition(initialAxisPositions.get(slice));
                        moveSlice(slice, lastAxisPos - (initialAxisPositions.get(slice) - lastAxisPos) * ratio);
                    }
                }
                new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
                updateDisplay();
                stopDragAction();
            }
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
            debuglog.accept(" SliceSourcesDrag start ("+x+":"+y+")");
            bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);
            // Computes which slice it corresponds to (useful for overlay redraw)
            iniSlicePointing = iniPointBdv.getDoublePosition(0) / sX + 0.5;
            iniSlicingAxisPosition = ((int) iniSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            if ((sliceOrigin.isSelected())) {
                initialAxisPositions.put(sliceOrigin, sliceOrigin.getSlicingAxisPosition());
            } else {
                if (!sliceOrigin.isSelected()) {
                    sliceOrigin.select();
                    perform = false;
                }
            }

            if (displayMode != POSITIONING_MODE_INT) {
                perform = false;
            }

            perform = perform && startDragAction(); // ensure unicity of drag action

            // Initialize the delta on a step of the zStepper

            if (perform) {
                deltaOrigin = iniSlicingAxisPosition - sliceOrigin.getSlicingAxisPosition();
                if (initialAxisPositions.containsKey(sliceOrigin)) {
                    sliceOrigin.setSlicingAxisPosition( initialAxisPositions.get(sliceOrigin) + deltaOrigin );
                }
                updateDisplay();
            }
            debuglog.accept(" SliceSourcesDrag perform : ("+perform+")");
        }

        @Override
        public void drag(int x, int y) {
            if (perform) {
                debuglog.accept(" SliceSourcesDrag drag ("+x+":"+y+")");
                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                double currentSlicePointing = currentMousePosition.getDoublePosition(0) / sX;
                double currentSlicingAxisPosition = ((int) currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
                double deltaAxis = currentSlicingAxisPosition - iniSlicingAxisPosition;

                if (initialAxisPositions.containsKey(sliceOrigin)) {
                    sliceOrigin.setSlicingAxisPosition( initialAxisPositions.get(sliceOrigin) + deltaAxis + deltaOrigin );
                }
                updateDisplay();
            }
        }

        @Override
        public void end(int x, int y) {
            if (perform) {
                debuglog.accept(" SliceSourcesDrag end ("+x+":"+y+")");
                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);
                double currentSlicePointing = currentMousePosition.getDoublePosition(0) / sX;
                double currentSlicingAxisPosition = ((int) currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
                double deltaAxis = currentSlicingAxisPosition - iniSlicingAxisPosition;

                if (initialAxisPositions.containsKey(sliceOrigin)) {
                    sliceOrigin.setSlicingAxisPosition( initialAxisPositions.get(sliceOrigin) );
                    moveSlice(sliceOrigin, initialAxisPositions.get(sliceOrigin) + deltaAxis + deltaOrigin);
                }
                updateDisplay();
                stopDragAction();
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

            debuglog.accept(" SelectedSliceSourcesDrag init ("+x+":"+y+")");

            bdvh.getViewerPanel().getGlobalMouseCoordinates(iniPointBdv);

            // Computes which slice it corresponds to (useful for overlay redraw)
            iniSlicePointing = iniPointBdv.getDoublePosition(0) / sX + 0.5;
            iniSlicingAxisPosition = ((int) iniSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();

            selectedSources = getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
            if ((selectedSources.size() > 0)) {// && (sliceOrigin.isSelected())) {
                perform = true;
                selectedSources.forEach(slice -> initialAxisPositions.put(slice, slice.getSlicingAxisPosition()));
            }

            if (displayMode != POSITIONING_MODE_INT) {
                perform = false;
            }

            perform = perform && startDragAction(); // ensure unicity of drag action

            // Initialize the delta on a step of the zStepper
            if (perform) {
                deltaOrigin = 0;
                for (SliceSources slice : selectedSources) {
                    if (initialAxisPositions.containsKey(slice)) {
                        slice.setSlicingAxisPosition( initialAxisPositions.get(slice) + deltaOrigin );
                    }
                }
                updateDisplay();
            }
            debuglog.accept(" SelectedSliceSourcesDrag perform : ("+perform+")");
        }

        @Override
        public void drag(int x, int y) {
            if (perform) {
                debuglog.accept(" SelectedSliceSourcesDrag drag ("+x+":"+y+")");
                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                double currentSlicePointing = currentMousePosition.getDoublePosition(0) / sX;
                double currentSlicingAxisPosition = ((int) currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
                double deltaAxis = currentSlicingAxisPosition - iniSlicingAxisPosition;

                for (SliceSources slice : selectedSources) {
                    if (initialAxisPositions.containsKey(slice)) {
                        slice.setSlicingAxisPosition( initialAxisPositions.get(slice) + deltaAxis + deltaOrigin );
                    }
                }
                updateDisplay();
            }
        }

        @Override
        public void end(int x, int y) {
            if (perform) {
                debuglog.accept(" SelectedSliceSourcesDrag end ("+x+":"+y+")");
                RealPoint currentMousePosition = new RealPoint(3);
                bdvh.getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

                double currentSlicePointing = currentMousePosition.getDoublePosition(0) / sX;
                double currentSlicingAxisPosition = ((int) currentSlicePointing) * sizePixX * (int) reslicedAtlas.getStep();
                double deltaAxis = currentSlicingAxisPosition - iniSlicingAxisPosition;

                new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
                for (SliceSources slice : selectedSources) {
                    if (initialAxisPositions.containsKey(slice)) {
                        slice.setSlicingAxisPosition( initialAxisPositions.get(slice) );
                        moveSlice(slice, initialAxisPositions.get(slice) + deltaAxis + deltaOrigin);
                    }
                }
                new MarkActionSequenceBatch(MultiSlicePositioner.this).runRequest();
                updateDisplay();
                stopDragAction();
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
        // AffineTransform3D serialization
        gsonbuilder.registerTypeAdapter(AffineTransform3D.class, new AffineTransform3DAdapter());

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

        // For sources processor

        RuntimeTypeAdapterFactory factorySourcesProcessor = RuntimeTypeAdapterFactory.of(SourcesProcessor.class);

        factorySourcesProcessor.registerSubtype(SourcesAffineTransformer.class);
        factorySourcesProcessor.registerSubtype(SourcesChannelsSelect.class);
        factorySourcesProcessor.registerSubtype(SourcesProcessComposer.class);
        factorySourcesProcessor.registerSubtype(SourcesResampler.class);
        factorySourcesProcessor.registerSubtype(SourcesIdentity.class);

        gsonbuider.registerTypeAdapterFactory(factorySourcesProcessor);
        gsonbuider.registerTypeHierarchyAdapter(SourcesChannelsSelect.class, new SourcesChannelSelectAdapter());
        gsonbuider.registerTypeHierarchyAdapter(SourcesAffineTransformer.class, new SourcesAffineTransformerAdapter());
        gsonbuider.registerTypeHierarchyAdapter(SourcesResampler.class, new SourcesResamplerAdapter());
        gsonbuider.registerTypeHierarchyAdapter(SourcesProcessComposer.class, new SourcesComposerAdapter());

        return gsonbuider.create();
    }

    public void saveState(File stateFile, boolean overwrite) {
        slices.get(0).waitForEndOfTasks();

        // Wait patiently for all tasks to be performed
        this.getSortedSlices().forEach(SliceSources::waitForEndOfTasks);

        // First save all sources required in the state
        List<SourceAndConverter> allSacs = new ArrayList<>();

        this.getSortedSlices().forEach(sliceSource -> allSacs.addAll(Arrays.asList(sliceSource.getOriginalSources())));

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

    public void loadState(File stateFile) {
        // TODO : add a clock as an overlay
        this.getSortedSlices().forEach(SliceSources::waitForEndOfTasks);

        String fileNoExt = FilenameUtils.removeExtension(stateFile.getAbsolutePath());
        File sacsFile = new File(fileNoExt+"_sources.json");

        if (!sacsFile.exists()) {
            errlog.accept("File "+sacsFile.getAbsolutePath()+" not found!");
            return;
        }

        SourceAndConverterServiceLoader sacsl = new SourceAndConverterServiceLoader(sacsFile.getAbsolutePath(), sacsFile.getParent(), this.scijavaCtx, false);
        sacsl.run();
        List<SourceAndConverter> serialized_sources = new ArrayList<>();

        sacsl.getSacToId().values().stream().sorted().forEach(i -> serialized_sources.add(sacsl.getIdToSac().get(i)));

        Gson gson = getGsonStateSerializer(serialized_sources);

        if (stateFile.exists()) {
            try {
                FileReader fileReader = new FileReader(stateFile);

                setSliceDisplayMode(NO_SLICE_DISPLAY_MODE);

                AlignerState state = gson.fromJson(fileReader, AlignerState.class); // actions are executed during deserialization
                fileReader.close();

                String warningMessageForUser = "";

                DecimalFormat df = new DecimalFormat("###.000");

                Function<Double, String> a = (d) -> df.format(d*180/Math.PI);

                if (state.rotationX!=reslicedAtlas.getRotateX()) {
                    warningMessageForUser+="Current X Angle : "+a.apply(reslicedAtlas.getRotateX())+" has been updated to "+a.apply(state.rotationX)+"\n";
                    reslicedAtlas.setRotateX(state.rotationX);
                }

                if (state.rotationY!=reslicedAtlas.getRotateY()) {
                    warningMessageForUser+="Current Y Angle : "+a.apply(reslicedAtlas.getRotateY())+" has been updated to "+a.apply(state.rotationY)+"\n";
                    reslicedAtlas.setRotateY(state.rotationY);
                }

                if (!warningMessageForUser.equals("")) {
                   this.warningMessageForUser.accept("Warning", warningMessageForUser);
                }

                setDisplayMode(state.displayMode);
                setOverlapMode(state.overlapMode);

                bdvh.getViewerPanel().state().setViewerTransform(state.bdvView);

                state.slices_state_list.forEach(sliceState -> {
                    sliceState.slice.waitForEndOfTasks();
                    sliceState.slice.getGUIState().setChannelsVisibility(sliceState.channelsVisibility); // TODO : restore
                    sliceState.slice.getGUIState().setDisplaysettings(sliceState.settings_per_channel);
                    sliceState.slice.transformSourceOrigin(sliceState.preTransform);
                });

                setSliceDisplayMode(state.sliceDisplayMode);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            errlog.accept("Error : file "+stateFile.getAbsolutePath()+" not found!");
        }
    }

    volatile private SliceSources currentSerializedSlice = null;

    //-------------------- Event listeners

    SliceSources currentSliceGetter() {
        return currentSerializedSlice;
    }

    List<SliceChangeListener> listeners = new ArrayList<>();

    public void addSliceListener(SliceChangeListener listener) {
        System.out.println("listener :"+listener);
        listeners.add(listener);
    }

    public void removeSliceListener(SliceChangeListener listener) {
        listeners.remove(listener);
    }

    interface SliceChangeListener {
        void sliceDeleted(SliceSources slice);
        void sliceCreated(SliceSources slice);
        void sliceZPositionChanged(SliceSources slice);
        void sliceVisibilityChanged(SliceSources slice);
        void sliceSelected(SliceSources slice);
        void sliceDeselected(SliceSources slice);
        void isCurrentSlice(SliceSources slice);
    }

    interface ModeListener {
        void modeChanged(MultiSlicePositioner mp, int oldmode, int newmode);
        void sliceDisplayModeChanged(MultiSlicePositioner mp, int oldmode, int newmode);
    }

}