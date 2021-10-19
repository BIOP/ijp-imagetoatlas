package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.*;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerFrame;
import ch.epfl.biop.ResourcesMonitor;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.DeleteSliceAction;
import ch.epfl.biop.atlas.aligner.action.SliceDefineROICommand;
import ch.epfl.biop.atlas.aligner.command.*;
import ch.epfl.biop.atlas.aligner.gui.MultiSliceContextMenuClickBehaviour;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.AtlasInfoPanel;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.EditPanel;
import ch.epfl.biop.atlas.aligner.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.aligner.plugin.RegistrationPluginHelper;
import ch.epfl.biop.bdv.gui.GraphicalHandle;
import ch.epfl.biop.bdv.gui.GraphicalHandleListener;
import net.imglib2.FinalInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.cache.CacheService;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
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
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import ch.epfl.biop.atlas.aligner.command.*;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
        //BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Change Slice Display Mode [S]",0, this::changeSliceDisplayMode);
        //BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Change Overlap Mode [O]",0, this::toggleOverlap);
        /*BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Show mouse atlas Position",0, this::showAtlasPosition);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Hide mouse atlas Position",0, this::hideAtlasPosition);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Show slice info at mouse position",0, this::showSliceInfo);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Hide slice info at mouse position",0, this::hideSliceInfo);*/
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

        // Help commands
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ABBAForumHelpCommand.class, hierarchyLevelsSkipped);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), DocumentationABBACommand.class, hierarchyLevelsSkipped);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ABBAUserFeedbackCommand.class, hierarchyLevelsSkipped);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), DocumentationDeepSliceCommand.class, hierarchyLevelsSkipped);

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
    public synchronized void sliceVisibilityChanged(SliceSources slice) {
        debug.accept(slice.name+ " visibility changed");
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

    class InnerOverlay extends BdvOverlay {

        @Override
        protected void draw(Graphics2D g) {
            // Gets a copy of the slices to avoid concurrent exception
            //List<SliceSources> slicesCopy = msp.getSlices();

            // Gets current bdv view position
            AffineTransform3D bdvAt3D = new AffineTransform3D();
            bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);

            //drawDragAndDropRectangle(g, bdvAt3D); TODO

            sliceGuiState.values().forEach(sliceGuiState -> sliceGuiState.drawGraphicalHandles(g));

            // drawCurrentSliceOverlay(g, slicesCopy); TODO

            // if (displayMode == POSITIONING_MODE_INT) drawSetOfSliceControls(g, bdvAt3D, slicesCopy); TODO

            if (selectionLayer != null) selectionLayer.draw(g);

            /*if (mso != null) mso.draw(g);

            if (showAtlasPosition) drawAtlasPosition(g);

            if (showSliceInfo) drawSliceInfo(g, slicesCopy); */

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

}
