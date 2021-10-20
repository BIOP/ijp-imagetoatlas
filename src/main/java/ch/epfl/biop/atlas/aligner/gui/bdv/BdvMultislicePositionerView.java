package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.*;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.ResourcesMonitor;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.CancelableAction;
import ch.epfl.biop.atlas.aligner.action.DeleteSliceAction;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.SliceDefineROICommand;
import ch.epfl.biop.atlas.aligner.command.*;
import ch.epfl.biop.atlas.aligner.gui.MultiSliceContextMenuClickBehaviour;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.AtlasAdjustDisplayCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.AtlasInfoPanel;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.EditPanel;
import ch.epfl.biop.atlas.aligner.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.aligner.plugin.RegistrationPluginHelper;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.bdv.gui.GraphicalHandle;
import ch.epfl.biop.bdv.gui.GraphicalHandleListener;
import mpicbg.spim.data.sequence.Tile;
import net.imglib2.*;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.cache.CacheService;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.PluginService;
import org.scijava.ui.behaviour.*;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.BdvScijavaHelper;
import sc.fiji.bdvpg.scijava.ScijavaSwingUI;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import javax.swing.*;
import java.awt.*;
import java.awt.Point;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static bdv.ui.BdvDefaultCards.*;

public class BdvMultislicePositionerView implements MultiSlicePositioner.SliceChangeListener, GraphicalHandleListener {

    public final MultiSlicePositioner msp;
    final BdvHandle bdvh;
    final SourceAndConverterBdvDisplayService displayService;
    Map<SliceSources, SliceGuiState> sliceGuiState = new WeakHashMap<>();

    Consumer<String> debug = System.out::println;

    protected static Logger logger = LoggerFactory.getLogger(MultiSlicePositioner.class);

    int mode = POSITIONING_MODE_INT;

    public final static int POSITIONING_MODE_INT = 0;
    final static String POSITIONING_MODE = "positioning-mode";
    final static String POSITIONING_BEHAVIOURS_KEY = POSITIONING_MODE + "-behaviours";
    Behaviours positioning_behaviours = new Behaviours(new InputTriggerConfig(), POSITIONING_MODE);

    public final static int REVIEW_MODE_INT = 1;
    final static String REVIEW_MODE = "review-mode";
    final static String REVIEW_BEHAVIOURS_KEY = REVIEW_MODE + "-behaviours";
    Behaviours review_behaviours = new Behaviours(new InputTriggerConfig(), REVIEW_MODE);

    final static String COMMON_BEHAVIOURS_KEY = "multipositioner-behaviours";
    Behaviours common_behaviours = new Behaviours(new InputTriggerConfig(), "multipositioner");

    // Selection layer : responsible to listen to mouse drawing events that select sources
    SelectionLayer selectionLayer;

    Runnable atlasSlicingListener;

    // Flag for overlaying the position of the mouse on the atlas + the region name
    boolean showAtlasPosition = true;

    /**
     * Non blocking error message for users
     */
    public BiConsumer<String, String> nonBlockingErrorMessageForUser = (title, message) ->
            logger.error(title+":"+message);

    /**
     * Blocking error message for users
     */
    public BiConsumer<String, String> errorMessageForUser = (title, message) ->
            JOptionPane.showMessageDialog(new JFrame(), message, title, JOptionPane.ERROR_MESSAGE);

    /**
     * Blocking warning message for users
     */
    public BiConsumer<String, String> warningMessageForUser = (title, message) ->
            JOptionPane.showMessageDialog(new JFrame(), message, title, JOptionPane.WARNING_MESSAGE);

    public Consumer<String> errlog = (message) -> {
        logger.error("Multipositioner : "+message);
        errorMessageForUser.accept("Error", message);
    };

    void installCommonBehaviours() {
        //common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.changeSliceDisplayMode(), "toggle_single_source_mode", "S");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> msp.cancelLastAction(), "cancel_last_action", "ctrl Z", "meta Z");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> msp.redoAction(), "redo_last_action", "ctrl Y", "ctrl shift Z", "meta Y", "ctrl meta Z");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.navigateNextSlice(), "navigate_next_slice", "RIGHT");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.navigatePreviousSlice(), "navigate_previous_slice",  "LEFT"); // P taken for panel
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.navigateCurrentSlice(), "navigate_current_slice", "C");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.nextMode(), "change_mode", "R");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> {msp.getSlices().forEach(SliceSources::select);bdvh.getViewerPanel().getDisplay().repaint();}, "selectAllSlices", "ctrl A", "meta A");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> {msp.getSlices().forEach(SliceSources::deSelect);bdvh.getViewerPanel().getDisplay().repaint();}, "deselectAllSlices", "ctrl shift A", "meta shift A");

        common_behaviours.behaviour((ClickBehaviour) (x, y) -> this.printBindings(), "print_bindings", "K");

        bdvh.getTriggerbindings().addBehaviourMap(COMMON_BEHAVIOURS_KEY, common_behaviours.getBehaviourMap());
        bdvh.getTriggerbindings().addInputTriggerMap(COMMON_BEHAVIOURS_KEY, common_behaviours.getInputTriggerMap()); // "transform", "bdv"
    }

    void clearBdvDefaults() {
        //bdvh.getCardPanel().removeCard(DEFAULT_SOURCES_CARD); // Cannot do this : errors
        bdvh.getCardPanel().removeCard(DEFAULT_SOURCEGROUPS_CARD);
        bdvh.getCardPanel().removeCard(DEFAULT_VIEWERMODES_CARD);
        bdvh.getCardPanel().setCardExpanded(DEFAULT_SOURCES_CARD, false);
        BdvScijavaHelper.clearBdvHandleMenuBar(bdvh);
    }

    void installBdvMenu(int hierarchyLevelsSkipped) {

        // Skips 4 levels of hierarchy in scijava command path (Plugins>BIOP>Atlas>Multi Image To Atlas>)
        // And uses the rest to make the hierarchy of the top menu in the bdv window

        // Load and Save state
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ABBAStateLoadCommand.class, hierarchyLevelsSkipped,"mp", msp );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ABBAStateSaveCommand.class, hierarchyLevelsSkipped,"mp", msp );

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Undo [Ctrl+Z]",0, msp::cancelLastAction);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Redo [Ctrl+Shift+Z]",0, msp::redoAction);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Select all slices [Ctrl+A]",0,() -> msp.getSlices().forEach(SliceSources::select));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Deselect all slices [Ctrl+Shift+A]",0,() -> msp.getSlices().forEach(SliceSources::deSelect));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Remove selected slices",0,() ->
                msp.getSortedSlices()
                        .stream()
                        .filter(SliceSources::isSelected)
                        .forEach(slice -> new DeleteSliceAction(msp, slice).runRequest())
        );

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Distribute spacing [D]",0,() -> {
            if (this.mode == POSITIONING_MODE_INT) msp.equalSpacingSelectedSlices();
        });

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Positioning Mode",0, () -> setDisplayMode(POSITIONING_MODE_INT));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Review Mode",0, () -> setDisplayMode(REVIEW_MODE_INT));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Change Slice Display Mode [S]",0, this::changeSliceDisplayMode);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Change Overlap Mode [O]",0, this::toggleOverlap);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Show mouse atlas Position",0, this::showAtlasPosition);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Hide mouse atlas Position",0, this::hideAtlasPosition);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Show slice info at mouse position",0, this::showSliceInfo);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Hide slice info at mouse position",0, this::hideSliceInfo);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>ABBA - Next Slice [Right]",0, this::navigateNextSlice);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>ABBA - Previous Slice [Left]",0, this::navigatePreviousSlice);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>ABBA - Center On Current Slice [C]",0, this::navigateCurrentSlice);

        // Slice importer
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ImportQuPathProjectCommand.class, hierarchyLevelsSkipped,"mp", msp );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ImportImagePlusCommand.class, hierarchyLevelsSkipped,"mp", msp );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ImportImageCommand.class, hierarchyLevelsSkipped,"mp", msp );

        logger.debug("Installing DeepSlice command");
        //DeepSliceCommand
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), RegistrationDeepSliceCommand.class, hierarchyLevelsSkipped,"mp", msp);

        // Adds registration plugin commands : discovered via scijava plugin autodiscovery mechanism

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), RegistrationEditLastCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), RegistrationEditLastCommand.class, hierarchyLevelsSkipped,"mp", msp ); // TODO
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportRegionsToRoiManagerCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportToQuPathProjectCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportSlicesToBDVJsonDatasetCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportSlicesToBDVCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportSlicesToImageJStackCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportSlicesOriginalDataToImageJCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportDeformationFieldToImageJCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportAtlasToImageJStackCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportSlicesToQuickNIIDatasetCommand.class, hierarchyLevelsSkipped,"mp", msp);

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), SliceRotateCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), SliceAffineTransformCommand.class, hierarchyLevelsSkipped,"mp", msp);

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), SliceThicknessCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), SliceThicknessMatchNeighborsCommand.class, hierarchyLevelsSkipped,"mp", msp);

        // Cards commands
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Cards>Expand Card Panel",0, () -> bdvh.getSplitPanel().setCollapsed(false));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Cards>Collapse Card Panel",0, () -> bdvh.getSplitPanel().setCollapsed(true));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Cards>Add Resources Monitor",0, this::addResourcesMonitor);

        // Help commands
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ABBAForumHelpCommand.class, hierarchyLevelsSkipped);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), DocumentationABBACommand.class, hierarchyLevelsSkipped);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ABBAUserFeedbackCommand.class, hierarchyLevelsSkipped);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), DocumentationDeepSliceCommand.class, hierarchyLevelsSkipped);

    }

    private void changeSliceDisplayMode() {
        // TODO
    }

    void installRegistrationPluginUI(int hierarchyLevelsSkipped) {
        PluginService pluginService = msp.getContext().getService(PluginService.class);

        pluginService.getPluginsOfType(IABBARegistrationPlugin.class).forEach(registrationPluginClass -> {
            IABBARegistrationPlugin plugin = pluginService.createInstance(registrationPluginClass);
            for (Class<? extends Command> commandUI: RegistrationPluginHelper.userInterfaces(plugin)) {
                logger.info("Registration plugin "+commandUI.getSimpleName()+" discovered");
                BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), commandUI, hierarchyLevelsSkipped,"mp", msp);
            }
        });

        logger.debug("Installing external registration plugins ui");
        msp.getExternalRegistrationPluginsUI().keySet().forEach(externalRegistrationType -> {
            msp.getExternalRegistrationPluginsUI().get(externalRegistrationType).forEach( ui -> {
                        logger.info("External registration plugin "+ui+" added in bdv user interface");
                        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh, "Align>"+ui, 0, () -> {
                            (msp.getContext().getService(CommandService.class)).run(ui, true, "mp", msp);
                        });
                    }
            );
        });
    }

    void installBigDataViewerCards() {

        bdvh.getCardPanel().addCard("Atlas Information", new AtlasInfoPanel(msp).getPanel(), true);

        bdvh.getCardPanel().addCard("Atlas Display", ScijavaSwingUI.getPanel(msp.getContext(), AtlasAdjustDisplayCommand.class, "view", this), true);

        //bdvh.getCardPanel().addCard("Slices Display", new SliceDisplayPanel(this).getPanel(), true); TODO

        bdvh.getCardPanel().addCard("Display & Navigation", new NavigationPanel(this).getPanel(), true);

        bdvh.getCardPanel().addCard("Edit Selected Slices", new EditPanel(msp).getPanel(), true);

        bdvh.getCardPanel().addCard("Atlas Slicing", ScijavaSwingUI.getPanel(msp.getContext(), AtlasSlicingAdjusterCommand.class, "reslicedAtlas", msp.getReslicedAtlas()), true);

        bdvh.getCardPanel().addCard("Define region of interest",
                ScijavaSwingUI.getPanel(msp.getContext(), SliceDefineROICommand.class, "mp", msp, "view", this),
                false);
    }

    void displayAtlas() {
        List<SourceAndConverter<?>> sacsToAppend = new ArrayList<>();
        for (int i = 0; i < msp.getAtlas().getMap().getStructuralImages().size(); i++) {
            sacsToAppend.add(msp.getReslicedAtlas().extendedSlicedSources[i]);
            sacsToAppend.add(msp.getReslicedAtlas().nonExtendedSlicedSources[i]);
        }
        SourceAndConverterServices.getBdvDisplayService()
                .show(bdvh, sacsToAppend.toArray(new SourceAndConverter[0]));
    }

    void addRoiOverlaySource() {

        logger.debug("Adding user ROI source");

        BiConsumer<RealLocalizable, UnsignedShortType> fun = (loc, val) -> {
            double px = loc.getFloatPosition(0);
            double py = loc.getFloatPosition(1);

            if (py<-sY/1.9) {val.set(0); return;}
            if (py>sY/1.9) {val.set(0); return;}

            if (mode == POSITIONING_MODE_INT) {
                final double v = Math.IEEEremainder(px + sX * 0.5, sX);
                if (v < roiPX) {val.set(255); return;}
                if (v > roiPX+roiSX) {val.set(255); return;}
                if (py<roiPY) {val.set(255); return;}
                if (py>roiPY+roiSY) {val.set(255); return;}
                val.set(0);
            }

            if (mode == REVIEW_MODE_INT) {
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
    }

    void addCleanUpHook() {
        // Close hook to try to release as many resources as possible -> proven avoiding mem leaks
        BdvHandleHelper.setBdvHandleCloseOperation(bdvh, msp.getContext().getService(CacheService.class),
                SourceAndConverterServices.getBdvDisplayService(), false,
                () -> {
                    logger.info("Closing multipositioner bdv window, releasing some resources.");
                    this.selectionLayer = null;
                    this.common_behaviours = null;
                    this.positioning_behaviours = null;
                    this.review_behaviours = null;
                    logger.debug("Removing listeners");
                    msp.getReslicedAtlas().removeListener(atlasSlicingListener);
                    msp.removeSliceListener(this);
                }
        );
    }

    void addRightClickActions() {
        common_behaviours.behaviour(new MultiSliceContextMenuClickBehaviour( msp, this, msp::getSelectedSources ), "Slices Context Menu", "button3", "ctrl button1", "meta button1");
    }

    public void showAtlasPosition() {
        showAtlasPosition = true;
    }

    public void hideAtlasPosition() {
        showAtlasPosition = false;
    }

    boolean showSliceInfo;

    public void showSliceInfo() {
        showSliceInfo = true;
    }

    public void hideSliceInfo() {
        showSliceInfo = false;
    }

    final double sX, sY;
    double roiPX, roiPY, roiSX, roiSY;

    public BdvMultislicePositionerView(MultiSlicePositioner msp, BdvHandle bdvh) {
        // Final variable initialization
        this.bdvh = bdvh;
        this.msp = msp;
        this.sX = msp.sX;
        this.sY = msp.sY;
        roiChanged(); // initialize roi
        this.displayService = msp.getContext().getService(SourceAndConverterBdvDisplayService.class);

        excludedKeys.add("X");
        excludedKeys.add("Y");
        excludedKeys.add("Z");
        excludedKeys.add("Left Right");

        // Other variable initialization
        previouszStep = (int) msp.getReslicedAtlas().getStep();

        // Creates previously exiting slices TODO check that it works!
        msp.addSliceListener(this);
        msp.getSlices().forEach(slice -> {
            sliceCreated(slice);
        });

        setDisplayMode(POSITIONING_MODE_INT);

        logger.debug("Installing behaviours : common");
        installCommonBehaviours();

        logger.debug("Overriding standard navigation commands");
        overrideStandardNavigation();

        logger.debug("Clearing default cards and bdv functionalities of BigDataViewer");
        clearBdvDefaults();

        logger.debug("Installing menu");
        int hierarchyLevelsSkipped = 4;
        installBdvMenu(hierarchyLevelsSkipped);

        logger.debug("Installing java registration plugins ui");
        installRegistrationPluginUI(hierarchyLevelsSkipped);

        logger.debug("Installing multislice overlay");
        BdvFunctions.showOverlay(new InnerOverlay(), "MultiSlice Overlay", BdvOptions.options().addTo(bdvh));

        logger.debug("Defining atlas slicing listener");
        atlasSlicingListener = this::atlasSlicingChanged;
        msp.getReslicedAtlas().addListener(atlasSlicingListener);

        logger.debug("Adding bigdataviewer cards");
        installBigDataViewerCards();

        logger.debug("Displaying Altas");
        displayAtlas();

        logger.debug("Adding ROI Overlay source");
        addRoiOverlaySource();

        logger.debug("Add right click actions");
        addRightClickActions();

        logger.debug("Adding close window cleaning hook");
        addCleanUpHook();

        logger.debug("SplitPanel Expanded");
        bdvh.getSplitPanel().setCollapsed(false);

        logger.debug("Adding Drag and Drop Handler");
        this.bdvh.getViewerPanel().setTransferHandler(new BdvMultislicePositionerView.TransferHandler());
        iSliceNoStep = (int) (msp.getReslicedAtlas().getStep());
    }

    public synchronized void iniSlice(SliceSources slice) {
        debug.accept("Initializing "+slice.name);
        sliceGuiState.put(slice, new SliceGuiState(this, slice, bdvh));
        sliceGuiState.get(slice).created();
        if (sliceGuiState.values().size()==1) {
            iCurrentSlice = 0;
            navigateCurrentSlice();
        }
    }

    int previouszStep;

    void atlasSlicingChanged() {
        recenterBdvh();
        updateDisplay();
    }

    int overlapMode = 0;

    public void updateDisplay() {
        // Sort slices along slicing axis
        if (overlapMode == 0) {
            sliceGuiState.values().forEach(sliceGuiState -> sliceGuiState.setYShift(1));
        } else if (overlapMode == 2) {

            double lastPositionAlongX = -Double.MAX_VALUE;

            int stairIndex = 0;

            for (SliceSources slice : msp.getSortedSlices()) {
                SliceGuiState guiState = sliceGuiState.get(slice);
                double posX = getSliceCenterPosition(slice).getDoublePosition(0);
                if (posX >= (lastPositionAlongX + msp.sX)) {
                    stairIndex = 0;
                    lastPositionAlongX = posX;
                    guiState.setYShift(1);
                } else {
                    stairIndex++;
                    guiState.setYShift(1 + stairIndex);
                }
            }
        } else if (overlapMode == 1) {
            sliceGuiState.values().forEach(sliceGuiState -> sliceGuiState.setYShift(0));
        }
        bdvh.getViewerPanel().requestRepaint();
    }

    public void recenterBdvh() {
        double cur_wcx = bdvh.getViewerPanel().getWidth() / 2.0; // Current Window Center X
        double cur_wcy = bdvh.getViewerPanel().getHeight() / 2.0; // Current Window Center Y

        RealPoint centerScreenCurrentBdv = new RealPoint(cur_wcx, cur_wcy, 0);
        RealPoint centerScreenGlobalCoord = new RealPoint(3);

        AffineTransform3D at3D = new AffineTransform3D();
        bdvh.getViewerPanel().state().getViewerTransform(at3D);

        at3D.inverse().apply(centerScreenCurrentBdv, centerScreenGlobalCoord);

        // New target
        centerScreenGlobalCoord.setPosition((centerScreenGlobalCoord.getDoublePosition(0) - msp.sX / 2.0) * (double) previouszStep / (double) msp.getReslicedAtlas().getStep() + msp.sX / 2.0, 0);

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

        bdvh.getViewerPanel().state().setViewerTransform(nextAffineTransform);
        previouszStep = (int) msp.getReslicedAtlas().getStep();
    }

    @Override
    public synchronized void sliceCreated(SliceSources slice) {
        debug.accept(slice.name+ " created");
        iniSlice(slice);
    }

    @Override
    public synchronized void sliceDeleted(SliceSources slice) {
        debug.accept(slice.name+ " deleted");
        sliceGuiState.get(slice).deleted();
        sliceGuiState.remove(slice);
    }

    @Override
    public synchronized void sliceZPositionChanged(SliceSources slice) {
        debug.accept(slice.name+ " z position changed");
        sliceGuiState.get(slice).slicePositionChanged();
    }

    @Override
    public synchronized void sliceSelected(SliceSources slice) {
        debug.accept(slice.name+ " selected");
        bdvh.getViewerPanel().getDisplay().repaint();
    }

    @Override
    public synchronized void sliceDeselected(SliceSources slice) {
        debug.accept(slice.name+ " deselected");
        bdvh.getViewerPanel().getDisplay().repaint();
    }

    public boolean isCurrentSlice(SliceSources slice) {
        List<SliceSources> sortedSlices = msp.getSortedSlices();
        if (iCurrentSlice >= sortedSlices.size()) {
            iCurrentSlice = 0;
        }

        if (sortedSlices.size() > 0) {
            return slice.equals(sortedSlices.get(iCurrentSlice));
        } else {
            return false;
        }
    }

    @Override
    public synchronized void sliceSourcesChanged(SliceSources slice) {
        debug.accept(slice.name+ " slices changed");
        sliceGuiState.get(slice).sourcesChanged();
    }

    @Override
    public void slicePretransformChanged(SliceSources sliceSources) {
        if (sliceGuiState.get(sliceSources).isVisible()) {
            bdvh.getViewerPanel().requestRepaint();
        }
    }

    @Override
    public void roiChanged() {
        double[] currentRoi = msp.getROI();
        roiPX = currentRoi[0];
        roiPY = currentRoi[1];
        roiSX = currentRoi[2];
        roiSY = currentRoi[3];
        bdvh.getViewerPanel().requestRepaint();
    }

    @Override
    public void actionEnqueue(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionStarted(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionFinished(SliceSources slice, CancelableAction action, boolean result) {

    }

    @Override
    public void actionCancelEnqueue(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionCancelStarted(SliceSources slice, CancelableAction action) {

    }

    @Override
    public void actionCancelFinished(SliceSources slice, CancelableAction action, boolean result) {

    }

    // --------------------------------------------------------- SETTING MODES

    /**
     * Toggles between positioning and registration mode
     */
    public void nextMode() {
        setDisplayMode(1-mode);
    }

    public synchronized void setDisplayMode(int mode) {
        if (this.mode!=mode) {
            int oldMode = mode;
            this.mode = mode;
            modeChanged(mode, oldMode);
        }
    }

    ResourcesMonitor rm = null;

    public void addResourcesMonitor() {
        if ((rm == null) && (bdvh!=null)) {
            try {
                rm = new ResourcesMonitor();
                bdvh.getCardPanel().addCard("Resources Monitor", rm, false);
            } catch (Exception e) {
                rm = null;
                logger.debug("Could not start Resources Monitor");
            }
        } else {
            if (bdvh == null) {
                errlog.accept("No Graphical User Interface.");
            }
            if (rm!=null) {
                warningMessageForUser.accept("Warning", "Resource Monitor is already present");
            }
        }
    }

    private synchronized void modeChanged(int mode, int oldMode) {
        sliceGuiState.values().forEach(sliceGuiState -> sliceGuiState.slicePositionChanged()); // Force displacement of slice

        if (mode == POSITIONING_MODE_INT) {
            //slices.forEach(slice -> slice.getGUIState().enableGraphicalHandles()); // TODO
            bdvh.getTriggerbindings().removeInputTriggerMap(REVIEW_BEHAVIOURS_KEY);
            bdvh.getTriggerbindings().removeBehaviourMap(REVIEW_BEHAVIOURS_KEY);
            positioning_behaviours.install(bdvh.getTriggerbindings(), POSITIONING_BEHAVIOURS_KEY);
            navigateCurrentSlice();
            refreshBlockMap();
        }

        if (mode == REVIEW_MODE_INT) {
            //slices.forEach(slice -> slice.getGUIState().enableGraphicalHandles()); // TODO
            bdvh.getTriggerbindings().removeInputTriggerMap(POSITIONING_BEHAVIOURS_KEY);
            bdvh.getTriggerbindings().removeBehaviourMap(POSITIONING_BEHAVIOURS_KEY);
            review_behaviours.install(bdvh.getTriggerbindings(), REVIEW_BEHAVIOURS_KEY);
            navigateCurrentSlice();
            refreshBlockMap();
        }

        modeListeners.forEach(modeListener -> modeListener.modeChanged(this, oldMode, mode));
    }

    public RealPoint getSliceCenterPosition(SliceSources slice) {
        if (mode==POSITIONING_MODE_INT) {
            double slicingAxisSnapped = (((int) ((slice.getSlicingAxisPosition()) / msp.sizePixX)) * msp.sizePixX);
            double posX = (slicingAxisSnapped / msp.sizePixX * msp.sX / msp.getReslicedAtlas().getStep()) + 0.5 * (msp.sX);
            double posY = msp.sY * sliceGuiState.get(slice).getYShift();
            return new RealPoint(posX, posY, 0);
        } else if (mode==REVIEW_MODE_INT) {
            return new RealPoint(0, 0, slice.getSlicingAxisPosition());
        } else {
            return new RealPoint(0, 0, 0);
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

    private static final String BLOCKING_MAP = "multipositioner-blocking";

    private void refreshBlockMap() {
        logger.debug("Refresh Block Map called");
        bdvh.getTriggerbindings().removeBehaviourMap(BLOCKING_MAP);

        final Set<InputTrigger> moveCornerTriggers = new HashSet<>();

        for (final String s : DRAG_TOGGLE_EDITOR_KEYS) {
            moveCornerTriggers.add(InputTrigger.getFromString(s));
        }

        final Map<InputTrigger, Set<String>> bindings = bdvh.getTriggerbindings().getConcatenatedInputTriggerMap().getAllBindings();

        final Set<String> behavioursToBlock = new HashSet<>();

        for (final InputTrigger t : moveCornerTriggers) {
            behavioursToBlock.addAll(bindings.get(t));
        }

        blockMap.clear();

        final Behaviour block = new Behaviour() {};

        for (final String key : behavioursToBlock) {
            blockMap.put(key, block);
        }
    }

    private void printBindings() {
        BdvHandleHelper.printBindings(bdvh, logger::debug);
    }

    int iCurrentSlice = 0;

    /**
     * Center bdv on current slice (iCurrentSlice)
     */
    public void navigateCurrentSlice() {
        List<SliceSources> sortedSlices = msp.getSortedSlices();
        if (iCurrentSlice >= sortedSlices.size()) {
            iCurrentSlice = 0;
        }
        if (sortedSlices.size() > 0) {
            SliceSources slice = sortedSlices.get(iCurrentSlice);
            if ((slice!=null)&&(sliceGuiState.get(slice)!=null)) {
                sliceGuiState.get(slice).isCurrent();
                centerBdvViewOn(sortedSlices.get(iCurrentSlice));
            }
        }
    }


    /**
     * Center bdv on next slice (iCurrentSlice + 1)
     */
    public void navigateNextSlice() {
        List<SliceSources> sortedSlices = msp.getSortedSlices();
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
                sliceGuiState.get(sortedSlices.get(previousSliceIndex)).isNotCurrent();
            }
            sliceGuiState.get(sortedSlices.get(iCurrentSlice)).isCurrent();
        }
    }


    /**
     * Center bdv on previous slice (iCurrentSlice - 1)
     */
    public void navigatePreviousSlice() {
        int previousSliceIndex = iCurrentSlice;
        iCurrentSlice--;
        List<SliceSources> sortedSlices = msp.getSortedSlices();

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
                sliceGuiState.get(sortedSlices.get(previousSliceIndex)).isNotCurrent();
            }
            sliceGuiState.get(sortedSlices.get(iCurrentSlice)).isCurrent();
        }
    }

    public void centerBdvViewOn(SliceSources slice) {
        centerBdvViewOn(slice, false, null);
    }

    // Center bdv on a slice
    public void centerBdvViewOn(SliceSources current_slice, boolean maintainoffset, SliceSources previous_slice) {

        RealPoint offset = new RealPoint(3);

        if ((maintainoffset)&&(previous_slice!=null)) {

            RealPoint oldCenter = getSliceCenterPosition(previous_slice);

            RealPoint centerScreen = getCurrentBdvCenter();
            offset.setPosition(-oldCenter.getDoublePosition(0) + centerScreen.getDoublePosition(0), 0);
            offset.setPosition(-oldCenter.getDoublePosition(1) + centerScreen.getDoublePosition(1), 1);
            //offset.setPosition(-oldCenter.getDoublePosition(2) + centerScreen.getDoublePosition(2), 2); // hmm no reason to maintain offset in z

            if (Math.abs(offset.getDoublePosition(0))>msp.sX/2.0) {maintainoffset = false;}
            if (Math.abs(offset.getDoublePosition(1))>msp.sY/2.0) {maintainoffset = false;}

        } else {
            maintainoffset = false;
        }

        RealPoint centerSlice = getSliceCenterPosition(current_slice);

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

    public int getCurrentSliceIndex() {
        return iCurrentSlice;
    }

    //------------------------------------------ DRAG BEHAVIOURS

    private AtomicBoolean dragActionInProgress = new AtomicBoolean(false);

    synchronized boolean startDragAction() {
        boolean result = dragActionInProgress.compareAndSet(false, true);
        logger.debug("Attempt to do a drag action : successful = "+result);
        return result;
    }

    synchronized void stopDragAction() {
        logger.debug("Stopping a drag action");
        dragActionInProgress.set(false);
    }


    //------------------------------ Multipositioner Graphical handles

    Set<GraphicalHandle> ghs = new HashSet<>();

    /*GraphicalHandle stretchLeft, center, stretchRight;

    Set<GraphicalHandle> ghs_tool_tip = new HashSet<>();*/

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

    /*@Override
    public synchronized void mouseDragged(MouseEvent e) {
        this.ghs.forEach(gh -> gh.mouseDragged(e));
        this.slices.forEach(slice -> slice.getGUIState().ghs.forEach(gh -> gh.mouseDragged(e)));
    }

    @Override
    public synchronized void mouseMoved(MouseEvent e) {
        this.ghs.forEach(gh -> gh.mouseMoved(e));
        slices.forEach(slice -> slice.getGUIState().ghs.forEach(gh -> gh.mouseMoved(e)));
    }*/

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


    public Object getCurrentSlice() {
        List<SliceSources> sortedSlices = msp.getSortedSlices();

        if (sortedSlices.size()>0) {
            if (iCurrentSlice >= sortedSlices.size()) {
                iCurrentSlice = 0;
            }
            return sortedSlices.get(iCurrentSlice);
        } else {
            return new Object();
        }
    }

    public BdvHandle getBdvh() {
        return bdvh;
    }

    public SourceAndConverter<?>[] getDisplayedAtlasSources() {
        switch (mode) {
            case POSITIONING_MODE_INT:
                return msp.getReslicedAtlas().extendedSlicedSources;
            case REVIEW_MODE_INT:
                return msp.getReslicedAtlas().nonExtendedSlicedSources;
            default:
                return null;
        }
    }

    public static List<String> excludedKeys = new ArrayList<>();

    public boolean includedKey(String key) {
        return !(excludedKeys.contains(key));
    }

    public int getDisplayMode() {
        return mode;
    }

    public void setSliceInvisible(SliceSources slice) {
    }

    public void setSliceVisible(SliceSources slice) {
    }


    private void drawSliceInfo(Graphics2D g, List<SliceSources> slicesCopy) {
        RealPoint mouseWindowPosition = new RealPoint(2);
        bdvh.getViewerPanel().getMouseCoordinates(mouseWindowPosition);
        // Slice info is displayed if the mouse is over the round slice handle
        Optional<SliceSources> optSlice = slicesCopy.stream()
                .filter(slice -> {
                    Integer[] coords = sliceGuiState.get(slice).getSliceHandleCoords();
                    if (coords==null) return false;
                    int radius = sliceGuiState.get(slice).getBdvHandleRadius();
                    double dx = coords[0]-mouseWindowPosition.getDoublePosition(0);
                    double dy = coords[1]-mouseWindowPosition.getDoublePosition(1);
                    double dist = Math.sqrt(dx*dx+dy*dy);
                    return dist<radius;
                }).findFirst();

        if (optSlice.isPresent()) {
            SliceSources slice = optSlice.get();
            String info = slice.getInfo();
            g.setFont(new Font("TimesRoman", Font.BOLD, 16));
            g.setColor(new Color(32, 125, 49, 220));
            Point mouseLocation = bdvh.getViewerPanel().getMousePosition();
            if ((info!=null)&&(mouseLocation!=null)) {
                drawString(g,info,mouseLocation.x,mouseLocation.y+40);
            }
        }

    }

    private void drawString(Graphics2D g, String info, int x, int y) {
        int lineHeight = g.getFontMetrics().getHeight();
        for (String line: info.split("\n"))
            g.drawString(line,x, y += lineHeight);
    }

    class InnerOverlay extends BdvOverlay {

        @Override
        protected void draw(Graphics2D g) {
            // Gets a copy of the slices to avoid concurrent exception
            List<SliceSources> slicesCopy = msp.getSlices();

            // Gets current bdv view position
            AffineTransform3D bdvAt3D = new AffineTransform3D();
            bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);

            drawDragAndDropRectangle(g, bdvAt3D);

            sliceGuiState.values().forEach(sliceGuiState -> sliceGuiState.drawGraphicalHandles(g));

            drawCurrentSliceOverlay(g, slicesCopy);

            if (mode == POSITIONING_MODE_INT) drawSetOfSliceControls(g, bdvAt3D, slicesCopy);

            if (selectionLayer != null) selectionLayer.draw(g);

            /*if (mso != null) mso.draw(g);*/

            if (showAtlasPosition) drawAtlasPosition(g);

            if (showSliceInfo) drawSliceInfo(g, slicesCopy);

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

    private void drawAtlasPosition(Graphics2D g) {
        ReslicedAtlas reslicedAtlas = msp.getReslicedAtlas();
        RealPoint globalMouseCoordinates = new RealPoint(3);
        bdvh.getViewerPanel().getGlobalMouseCoordinates(globalMouseCoordinates);
        int labelValue;
        int leftRight;
        float[] coords = new float[3];
        if (mode==POSITIONING_MODE_INT) {

            SourceAndConverter label = reslicedAtlas.extendedSlicedSources[reslicedAtlas.getLabelSourceIndex()]; // By convention the label image is the last one (OK)
            labelValue = ((IntegerType<?>) getSourceValueAt(label, globalMouseCoordinates)).getInteger();
            SourceAndConverter lrSource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.getLeftRightSourceIndex()]; // By convention the left right indicator image is the next to last one
            leftRight = ((IntegerType<?>) getSourceValueAt(lrSource, globalMouseCoordinates)).getInteger();

            SourceAndConverter<FloatType> xSource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.getCoordinateSourceIndex(0)]; // 0 = X
            SourceAndConverter<FloatType> ySource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.getCoordinateSourceIndex(1)]; // By convention the left right indicator image is the next to last one
            SourceAndConverter<FloatType> zSource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.getCoordinateSourceIndex(2)]; // By convention the left right indicator image is the next to last one

            coords[0] = ((FloatType) getSourceValueAt(xSource, globalMouseCoordinates)).get();
            coords[1] = ((FloatType) getSourceValueAt(ySource, globalMouseCoordinates)).get();
            coords[2] = ((FloatType) getSourceValueAt(zSource, globalMouseCoordinates)).get();
        } else {
            assert mode == REVIEW_MODE_INT;
            SourceAndConverter label = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.getLabelSourceIndex()]; // By convention the label image is the last one
            labelValue = ((IntegerType) getSourceValueAt(label, globalMouseCoordinates)).getInteger();
            SourceAndConverter lrSource = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.getLeftRightSourceIndex()]; // By convention the left right indicator image is the next to last one
            leftRight = ((IntegerType) getSourceValueAt(lrSource, globalMouseCoordinates)).getInteger();

            SourceAndConverter<FloatType> xSource = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.getCoordinateSourceIndex(0)]; // 0 = X
            SourceAndConverter<FloatType> ySource = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.getCoordinateSourceIndex(1)]; // By convention the left right indicator image is the next to last one
            SourceAndConverter<FloatType> zSource = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.getCoordinateSourceIndex(2)]; // By convention the left right indicator image is the next to last one

            coords[0] = ((FloatType) getSourceValueAt(xSource, globalMouseCoordinates)).get();
            coords[1] = ((FloatType) getSourceValueAt(ySource, globalMouseCoordinates)).get();
            coords[2] = ((FloatType) getSourceValueAt(zSource, globalMouseCoordinates)).get();
        }

        DecimalFormat df = new DecimalFormat("#0.00");
        String coordinates = "["+df.format(coords[0])+";"+df.format(coords[1])+";"+df.format(coords[2])+"]";
        if (leftRight == msp.getAtlas().getMap().labelRight()) {
            coordinates += "(R)";
        }
        if (leftRight == msp.getAtlas().getMap().labelLeft()) {
            coordinates += "(L)";
        }
        StringBuilder ontologyLocation = null;

        AtlasNode node = msp.getAtlas().getOntology().getNodeFromId(labelValue);
        if (node!=null) {
            ontologyLocation = new StringBuilder(node.toString());
            while (node.parent()!=null) {
                node = (AtlasNode) node.parent();
                if (node!=null) {
                    ontologyLocation.append("<").append(node.toString());
                }
            }
        }

        g.setFont(new Font("TimesRoman", Font.BOLD, 16));
        g.setColor(new Color(255, 255, 100, 250));
        Point mouseLocation = bdvh.getViewerPanel().getMousePosition();
        if ((ontologyLocation!=null)&&(mouseLocation!=null)) {
            g.drawString(ontologyLocation.toString(),mouseLocation.x,mouseLocation.y);
        }
        if ((mouseLocation!=null)&&(!coordinates.startsWith("[0.00;0.00;0.00]"))) {
            g.drawString(coordinates, mouseLocation.x, mouseLocation.y - 20);
        }

    }

    // Maximum right position of the selected slices
    Integer[] rightPosition = new Integer[]{0, 0, 0};

    // Maximum left position of the selected slices
    Integer[] leftPosition = new Integer[]{0, 0, 0};

    private void drawSetOfSliceControls(Graphics2D g, AffineTransform3D bdvAt3D, List<SliceSources> slicesCopy) {

        if (slicesCopy.stream().anyMatch(SliceSources::isSelected)) {

            List<SliceSources> sortedSelected = msp.getSortedSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
            RealPoint precedentPoint = null;

            for (int i = 0; i < sortedSelected.size(); i++) {
                SliceSources slice = sortedSelected.get(i);
                SliceGuiState sliceState = sliceGuiState.get(slice);
                Integer[] coords = sliceState.getSliceHandleCoords();
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
                    RealPoint handleLeftPoint = this.getSliceCenterPosition(slice);
                    handleLeftPoint.setPosition(+sY/2.0, 1);
                    bdvAt3D.apply(handleLeftPoint, handleLeftPoint);

                    leftPosition[0] = (int) handleLeftPoint.getDoublePosition(0);
                    leftPosition[1] = (int) handleLeftPoint.getDoublePosition(1);
                }

                if (i == sortedSelected.size() - 1) {
                    RealPoint handleRightPoint = this.getSliceCenterPosition(slice);
                    handleRightPoint.setPosition(+sY/2.0, 1);
                    bdvAt3D.apply(handleRightPoint, handleRightPoint);

                    rightPosition[0] = (int) handleRightPoint.getDoublePosition(0);
                    rightPosition[1] = (int) handleRightPoint.getDoublePosition(1);
                }

            }

            if (sortedSelected.size() > 1) {
                ghs.forEach(GraphicalHandle::enable);
                g.setColor(new Color(255, 0, 255, 200));
                g.drawLine(leftPosition[0], leftPosition[1], rightPosition[0], rightPosition[1]);
            } else if (sortedSelected.size() == 1) {
                g.setColor(new Color(255, 0, 255, 200));
                g.drawLine(leftPosition[0], leftPosition[1], rightPosition[0], rightPosition[1]);
            } else {
                ghs.forEach(GraphicalHandle::disable);
            }
            ghs.forEach(gh -> gh.draw(g));
        }
    }

    private void drawCurrentSliceOverlay(Graphics2D g, List<SliceSources> slicesCopy) {

        if (iCurrentSlice != -1 && slicesCopy.size() > iCurrentSlice) {
            SliceSources slice = msp.getSortedSlices().get(iCurrentSlice);
            //listeners.forEach(listener -> listener.isCurrentSlice(slice));
            g.setColor(new Color(255, 255, 255, 128));
            g.setStroke(new BasicStroke(5));
            Integer[] coords = sliceGuiState.get(slice).getSliceHandleCoords();
            RealPoint sliceCenter = new RealPoint(coords[0], coords[1], 0);
            g.drawOval((int) sliceCenter.getDoublePosition(0) - 15, (int) sliceCenter.getDoublePosition(1) - 15, 29, 29);
            Integer[] c = {255,255,255,128};
            g.setColor(new Color(c[0], c[1], c[2], c[3]));
            g.setFont(new Font("TimesRoman", Font.PLAIN, 16));
            g.drawString("\u25C4 \u25BA", (int) (sliceCenter.getDoublePosition(0) - 15), (int) (sliceCenter.getDoublePosition(1) - 20));
        }
    }

    int sliceDisplayMode = 0;

    final static public int NO_SLICE_DISPLAY_MODE = 2;
    final static public int ALL_SLICES_DISPLAY_MODE = 0;
    final static public int CURRENT_SLICE_DISPLAY_MODE = 1;

    public void setSliceDisplayMode (int sliceDisplayMode) {
        if (this.sliceDisplayMode!=sliceDisplayMode) {
            this.sliceDisplayMode = sliceDisplayMode;
            sliceGuiState.values().forEach(sliceGuiState -> sliceGuiState.sliceDisplayChanged(sliceDisplayMode));
        }
    }

    public void toggleOverlap() {
        // TODO
    }

    List<ModeListener> modeListeners = new ArrayList<>();

    public void addModeListener(ModeListener modeListener) {
        modeListeners.add(modeListener);
    }

    public void removeModeListener(ModeListener modeListener) {
        modeListeners.remove(modeListener);
    }

    public interface ModeListener {
        void modeChanged(BdvMultislicePositionerView mp, int oldmode, int newmode);
        void sliceDisplayModeChanged(BdvMultislicePositionerView mp, int oldmode, int newmode);
    }

    // Current coordinate where Sources are dragged
    int iSliceNoStep;

    void drawDragAndDropRectangle(Graphics2D g, AffineTransform3D bdvAt3D) {
        int colorCode = ARGBType.rgba(120,250,50,128);//this.info.getColor().get();

        Color color = new Color(ARGBType.red(colorCode), ARGBType.green(colorCode), ARGBType.blue(colorCode), ARGBType.alpha(colorCode));

        g.setColor(color);

        RealPoint[][] ptRectWorld = new RealPoint[2][2];

        Point[][] ptRectScreen = new Point[2][2];

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

        g.setColor(color);
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
            Optional<BdvHandle> bdvh = getBdvHandleFromViewerPanel(((bdv.viewer.ViewerPanel) support.getComponent()));
            if (bdvh.isPresent()) {
                double slicingAxisPosition = iSliceNoStep * msp.sizePixX * (int) msp.getReslicedAtlas().getStep();
                msp.createSlice(sacs.toArray(new SourceAndConverter[0]), slicingAxisPosition, msp.getAtlas().getMap().getAtlasPrecisionInMillimeter(), Tile.class, new Tile(-1));
            }
        }
    }

}
