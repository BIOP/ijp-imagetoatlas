package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.*;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import ch.epfl.biop.ResourcesMonitor;
import ch.epfl.biop.atlas.aligner.*;
import ch.epfl.biop.atlas.aligner.adapter.AlignerState;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.*;
import ch.epfl.biop.atlas.aligner.command.*;
import ch.epfl.biop.atlas.aligner.gui.MultiSliceContextMenuClickBehaviour;
import ch.epfl.biop.atlas.aligner.plugin.ABBACommand;
import ch.epfl.biop.atlas.aligner.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.aligner.plugin.RegistrationPluginHelper;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.command.AllenBrainAdultMouseAtlasCCF2017Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.bdv.gui.graphicalhandle.GraphicalHandle;
import ch.epfl.biop.bdv.gui.graphicalhandle.GraphicalHandleListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ij.IJ;
import mpicbg.spim.data.sequence.Tile;
import net.imglib2.*;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.scijava.MenuPath;
import org.scijava.cache.CacheService;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
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
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import spimdata.util.Displaysettings;

import javax.swing.*;
import java.awt.*;
import java.awt.Point;
import java.awt.event.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static bdv.ui.BdvDefaultCards.*;

public class BdvMultislicePositionerView implements MultiSlicePositioner.SliceChangeListener, GraphicalHandleListener, MouseMotionListener, MultiSlicePositioner.MultiSlicePositionerListener {

    public MultiSlicePositioner msp; // TODO : make accessor
    final BdvHandle bdvh;
    final SourceAndConverterBdvDisplayService displayService;

    TableView tableView;

    Consumer<String> debug = System.out::println;

    protected static Logger logger = LoggerFactory.getLogger(MultiSlicePositioner.class);

    protected int mode;

    public final static int POSITIONING_MODE_INT = 0;
    final static String POSITIONING_MODE = "positioning-mode";
    final static String POSITIONING_BEHAVIOURS_KEY = POSITIONING_MODE + "-behaviours";
    private Behaviours positioning_behaviours = new Behaviours(new InputTriggerConfig(), POSITIONING_MODE);

    public final static int REVIEW_MODE_INT = 1;
    final static String REVIEW_MODE = "review-mode";
    final static String REVIEW_BEHAVIOURS_KEY = REVIEW_MODE + "-behaviours";
    private Behaviours review_behaviours = new Behaviours(new InputTriggerConfig(), REVIEW_MODE);

    final static String COMMON_BEHAVIOURS_KEY = "multipositioner-behaviours";
    private Behaviours common_behaviours = new Behaviours(new InputTriggerConfig(), "multipositioner");

    // Selection layer : responsible to listen to mouse drawing events that select sources
    private SelectionLayer selectionLayer;

    private Runnable atlasSlicingListener;

    protected SynchronizedSliceGuiState guiState = new SynchronizedSliceGuiState();

    private MultiSliceContextMenuClickBehaviour mscClick;

    private boolean showSliceInfo; // use accessors

    // Flag for overlaying the position of the mouse on the atlas + the region name
    private boolean showAtlasPosition = true; // use accessors

    private final double sX, sY;

    private double roiPX, roiPY, roiSX, roiSY;

    private TransferHandler transferHandler;

    private ViewerPanel vp;

    private int previouszStep;

    List<Runnable> extraCleanUp = new ArrayList<>();

    private int iCurrentSlice = 0;

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


    /**
     * Blocking warning message for users
     */
    public BiConsumer<String, String> infoMessageForUser = (title, message) ->
            JOptionPane.showMessageDialog(new JFrame(), message, title, JOptionPane.INFORMATION_MESSAGE);

    public Consumer<String> errlog = (message) -> {
        logger.error("Multipositioner : "+message);
        errorMessageForUser.accept("Error", message);
    };

    // Maximum right position of the selected slices
    Integer[] rightPosition = new Integer[]{0, 0, 0};

    // Maximum left position of the selected slices
    Integer[] leftPosition = new Integer[]{0, 0, 0};

    private int sliceDisplayMode = 0;

    final static public int NO_SLICE_DISPLAY_MODE = 2;
    final static public int ALL_SLICES_DISPLAY_MODE = 0;
    final static public int CURRENT_SLICE_DISPLAY_MODE = 1;

    // Current coordinate where Sources are dragged
    private int iSliceNoStep;

    protected int overlapMode = 0;

    //------------------------------ Multipositioner Graphical handles

    Set<GraphicalHandle> ghs = new HashSet<>();

    Set<GraphicalHandle> gh_below_mouse = new HashSet<>();

    // --------------- METHODS FOR INITIALISATION

    private void installCommonBehaviours() {
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

        common_behaviours.behaviour((ClickBehaviour) (x, y) -> viewPreviousRegistration(), "viewPreviousRegistration", "P");
        common_behaviours.behaviour((ClickBehaviour) (x, y) -> viewNextRegistration(), "viewNextRegistration", "N");

        bdvh.getTriggerbindings().addBehaviourMap(COMMON_BEHAVIOURS_KEY, common_behaviours.getBehaviourMap());
        bdvh.getTriggerbindings().addInputTriggerMap(COMMON_BEHAVIOURS_KEY, common_behaviours.getInputTriggerMap()); // "transform", "bdv"
    }

    int registrationStepBack = 0;

    public void viewPreviousRegistration() {
        // So : how many registrations max ?
        int maxNRegistrations = msp.getSlices().stream().mapToInt(SliceSources::getNumberOfRegistrations).max().getAsInt();
        if (registrationStepBack<maxNRegistrations) {
            registrationStepBack++;
            guiState.forEachSlice(guiState -> guiState.setRegistrationStepBack(registrationStepBack));
        }
    }

    public void viewNextRegistration() {
        if (registrationStepBack>0) {
            registrationStepBack--;
            guiState.forEachSlice(guiState -> guiState.setRegistrationStepBack(registrationStepBack));
        }
    }

    private void installPositioningBehaviours() {
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> this.toggleOverlap(), "toggle_superimpose", "O");
        positioning_behaviours.behaviour((ClickBehaviour) (x, y) -> msp.equalSpacingSelectedSlices(), "equalSpacingSelectedSlices", "D");
    }

    private void installReviewBehaviours() {
        // None
    }

    private void clearBdvDefaults() {
        //bdvh.getCardPanel().removeCard(DEFAULT_SOURCES_CARD); // Cannot do this : errors
        bdvh.getCardPanel().removeCard(DEFAULT_SOURCEGROUPS_CARD);
        bdvh.getCardPanel().removeCard(DEFAULT_VIEWERMODES_CARD);
        bdvh.getCardPanel().setCardExpanded(DEFAULT_SOURCES_CARD, false);
        BdvScijavaHelper.clearBdvHandleMenuBar(bdvh);
    }

    private void installBdvMenu(int hierarchyLevelsSkipped) {

        // Skips 4 levels of hierarchy in scijava command path (Plugins>BIOP>Atlas>Multi Image To Atlas>)
        // And uses the rest to make the hierarchy of the top menu in the bdv window

        // Load and Save state
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"File>ABBA - Save State (+View)",0, () -> new Thread(() -> saveState()).start());
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"File>ABBA - Load State (+View)",0, () -> new Thread(() -> loadState()).start());

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Positioning Mode",0, () -> setDisplayMode(POSITIONING_MODE_INT));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Review Mode",0, () -> setDisplayMode(REVIEW_MODE_INT));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Change Overlap Mode [O]",0, this::toggleOverlap);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Show mouse atlas Position",0, this::showAtlasPosition);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Hide mouse atlas Position",0, this::hideAtlasPosition);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Show slice info at mouse position",0, this::showSliceInfo);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Display>ABBA - Hide slice info at mouse position",0, this::hideSliceInfo);

        // Cards commands
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Cards>Expand Card Panel",0, () -> bdvh.getSplitPanel().setCollapsed(false));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Cards>Collapse Card Panel",0, () -> bdvh.getSplitPanel().setCollapsed(true));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Cards>Add Resources Monitor",0, this::addResourcesMonitor);

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>ABBA - Next Slice [Right]",0, this::navigateNextSlice);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>ABBA - Previous Slice [Left]",0, this::navigatePreviousSlice);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Navigate>ABBA - Center On Current Slice [C]",0, this::navigateCurrentSlice);

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Undo [Ctrl+Z]",0, msp::cancelLastAction);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Redo [Ctrl+Shift+Z]",0, msp::redoAction);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), SliceMinMaxDisplaySetCommand.class, hierarchyLevelsSkipped,"view", this );
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Select all slices [Ctrl+A]",0,() -> msp.getSlices().forEach(SliceSources::select));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Deselect all slices [Ctrl+Shift+A]",0,() -> msp.getSlices().forEach(SliceSources::deSelect));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Remove selected slices",0,() ->
                msp.getSlices()
                        .stream()
                        .filter(SliceSources::isSelected)
                        .forEach(slice -> new DeleteSliceAction(msp, slice).runRequest())
        );

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>ABBA - Distribute spacing [D]",0,() -> {
            if (this.mode == POSITIONING_MODE_INT) msp.equalSpacingSelectedSlices();
        });

        // Slice importer
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ImportQuPathProjectCommand.class, hierarchyLevelsSkipped,"mp", msp );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ImportImagePlusCommand.class, hierarchyLevelsSkipped,"mp", msp );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ImportImageCommand.class, hierarchyLevelsSkipped,"mp", msp );

        logger.debug("Installing DeepSlice command");
        //DeepSliceCommand
        if ((msp.getAtlas() instanceof AllenBrainAdultMouseAtlasCCF2017Command)) {
            BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), RegistrationDeepSliceCommand.class, hierarchyLevelsSkipped, "mp", msp);
        }

        logger.debug("Installing java registration plugins ui");
        installRegistrationPluginUI(hierarchyLevelsSkipped);
        // Adds registration plugin commands : discovered via scijava plugin autodiscovery mechanism

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), RegistrationEditLastCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), RegistrationRemoveLastCommand.class, hierarchyLevelsSkipped,"mp", msp ); // TODO
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportRegionsToRoiManagerCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportToQuPathProjectCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportSlicesToBDVJsonDatasetCommand.class, hierarchyLevelsSkipped,"mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ExportResampledSlicesToBDVSourceCommand.class, hierarchyLevelsSkipped,"mp", msp);
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

    private void installRegistrationPluginUI(int hierarchyLevelsSkipped) {
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


        logger.debug("Adding interactive transform");
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh, "Align>ABBA - Interactive Transform", 0, () -> {
            (msp.getContext().getService(CommandService.class)).run(SliceAffineTransformCommand.class, true, "mp", msp);
        });
        //BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), SliceAffineTransformCommand.class, hierarchyLevelsSkipped,"mp", msp);

    }

    private void installBigDataViewerCards() {

        bdvh.getCardPanel().addCard("Atlas Information", new AtlasInfoPanel(msp).getPanel(), true);

        bdvh.getCardPanel().addCard("Atlas Display", ScijavaSwingUI.getPanel(msp.getContext(), AtlasAdjustDisplayCommand.class, "view", this), true);

        logger.debug("Adding table view");
        addTableView();

        bdvh.getCardPanel().addCard("Display & Navigation", new NavigationPanel(this).getPanel(), true);

        bdvh.getCardPanel().addCard("Edit Selected Slices", new EditPanel(msp).getPanel(), true);

        bdvh.getCardPanel().addCard("Atlas Slicing", ScijavaSwingUI.getPanel(msp.getContext(), AtlasSlicingAdjusterCommand.class, "reslicedAtlas", msp.getReslicedAtlas()), true);

        bdvh.getCardPanel().addCard("Define region of interest",
                ScijavaSwingUI.getPanel(msp.getContext(), SliceDefineROICommand.class, "mp", msp, "view", this),
                false);
        addToCleanUpHook(() -> {
            if (bdvh.getCardPanel()!=null) {
                bdvh.getCardPanel().removeCard("Atlas Information");
                bdvh.getCardPanel().removeCard("Atlas Display");
                bdvh.getCardPanel().removeCard("Display & Navigation");
                bdvh.getCardPanel().removeCard("Edit Selected Slices");
                bdvh.getCardPanel().removeCard("Atlas Slicing");
                bdvh.getCardPanel().removeCard("Define region of interest");
            }
        });
    }

    private void displayAtlas() {
        List<SourceAndConverter<?>> sacsToAppend = new ArrayList<>();
        for (int i = 0; i < msp.getAtlas().getMap().getStructuralImages().size(); i++) {
            sacsToAppend.add(msp.getReslicedAtlas().extendedSlicedSources[i]);
            sacsToAppend.add(msp.getReslicedAtlas().nonExtendedSlicedSources[i]);
        }
        SourceAndConverterServices.getBdvDisplayService()
                .show(bdvh, sacsToAppend.toArray(new SourceAndConverter[0]));
    }

    public void addToCleanUpHook(Runnable runnable) {
        extraCleanUp.add(runnable);
    }

    private void addRoiOverlaySource() {

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
                if (loc.getFloatPosition(1) < roiPY) {val.set(255); return;}
                if (loc.getFloatPosition(1) > roiPY+roiSY) {val.set(255); return;}
                val.set(0);
            }
        };

        FunctionRealRandomAccessible<UnsignedShortType> roiOverlay = new FunctionRealRandomAccessible<>(3, fun, UnsignedShortType::new);

        BdvStackSource<?> bss = BdvFunctions.show(roiOverlay,
                new FinalInterval(new long[]{0, 0, 0}, new long[]{10, 10, 10}),"ROI", BdvOptions.options().addTo(bdvh));

        bss.setDisplayRangeBounds(0,1600);
    }

    private void addCleanUpHook() {
        // Close hook to try to release as many resources as possible -> proven avoiding mem leaks
        BdvHandleHelper.setBdvHandleCloseOperation(bdvh, msp.getContext().getService(CacheService.class),
                SourceAndConverterServices.getBdvDisplayService(), false,
                () -> {
                    logger.info("Closing multipositioner bdv window, releasing some resources.");

                    logger.debug("Removing listeners");

                    if (guiState!=null) {
                        guiState.clear();
                        guiState.clear();
                    }
                    if (vp!=null) {
                        vp.setTransferHandler(new javax.swing.TransferHandler("Dummy"));
                        transferHandler = null;
                        vp.getDisplay().removeHandler(this);
                        vp = null;
                    }
                    if (rm!=null) {
                        rm.stop();
                        rm = null;
                    }
                    if (mscClick!=null) {
                        mscClick.clear();
                        mscClick = null;
                    }
                    extraCleanUp.forEach(Runnable::run);

                    if (msp!=null) {
                        // NPE!!, and not necessary anyway
                        /*bdvh.getTriggerbindings().removeInputTriggerMap(REVIEW_BEHAVIOURS_KEY);
                        bdvh.getTriggerbindings().removeBehaviourMap(REVIEW_BEHAVIOURS_KEY);
                        bdvh.getTriggerbindings().removeInputTriggerMap(POSITIONING_BEHAVIOURS_KEY);
                        bdvh.getTriggerbindings().removeBehaviourMap(POSITIONING_BEHAVIOURS_KEY);
                        bdvh.getTriggerbindings().removeInputTriggerMap(COMMON_BEHAVIOURS_KEY);
                        bdvh.getTriggerbindings().removeBehaviourMap(COMMON_BEHAVIOURS_KEY);*/
                    }

                    if (msp!=null) {
                        this.common_behaviours = null;
                        this.positioning_behaviours = null;
                        this.review_behaviours = null;
                        this.selectionLayer = null;
                        if (msp.getReslicedAtlas()!=null) {
                            msp.getReslicedAtlas().removeListener(atlasSlicingListener);
                        }
                        msp.removeSliceListener(this);
                        msp = null;
                    }
                }
        );
    }

    private void addRightClickActions() {
        mscClick = new MultiSliceContextMenuClickBehaviour( msp, this, msp::getSelectedSlices);
        common_behaviours.behaviour(mscClick, "Slices Context Menu", "button3", "ctrl button1", "meta button1");
    }

    private void addFrameIcon() {
        // Set ABBA Icon in Window
        JFrame frame = ((BdvHandleFrame)bdvh).getBigDataViewer().getViewerFrame();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setIconImage((new ImageIcon(MultiSlicePositioner.class.getResource("/graphics/ABBAFrame.jpg"))).getImage());
    }

    private void addTableView() {
        tableView = new TableView(this);
        msp.addSliceListener(tableView);
        bdvh.getCardPanel().addCard("Slices Display", tableView.getPanel(), true);
        addToCleanUpHook(() -> {
            tableView.cleanup();
            if (msp!=null) { // Because cleanup is called 2 times. TODO fix double call
                msp.removeSliceListener(tableView);
            }
        });
    }

    private void addDnDHandler() {
        transferHandler = new BdvMultislicePositionerView.TransferHandler();
        this.bdvh.getViewerPanel().setTransferHandler(transferHandler);
        iSliceNoStep = (int) (msp.getReslicedAtlas().getStep());
    }

    private void addModificationMonitor() {
        BdvHandleHelper.setWindowTitle(bdvh, getViewName());

        modificationMonitorThread = new Thread(this::modificationMonitor);
        modificationMonitorThread.start();

        addToCleanUpHook(() -> stopMonitoring = true);
    }

    private boolean closeAlreadyActivated = false;
    private boolean cleanAllOnExit = false;

    private void addCleanAllHook() {
        addToCleanUpHook(() -> {
            if (cleanAllOnExit) {
                if (msp!=null) {
                    Context ctx = msp.getContext();
                    Atlas atlas = msp.getAtlas();
                    msp.close();
                    ctx.getService(ObjectService.class).removeObject(msp);
                    ctx.getService(ObjectService.class).removeObject(atlas);

                    // Remove all sources - TODO : make this more specific!
                    SourceAndConverterServices
                            .getSourceAndConverterService()
                            .remove(
                                    SourceAndConverterServices
                                            .getSourceAndConverterService().getSourceAndConverters().toArray(new SourceAndConverter[0])
                            );

                    System.gc();
                }
            }
        });
    }

    private void addConfirmationCloseHook() {
        JFrame frame = BdvHandleHelper.getJFrame(bdvh);
        WindowListener[] listeners = frame.getWindowListeners();

        for (WindowListener listener:listeners) {
            frame.removeWindowListener(listener);
        }

        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                if (!closeAlreadyActivated) {
                    String message = "Are you sure you want to exit ABBA?";

                    if (msp.isModifiedSinceLastSave()) {
                        message+= " Your last modifications have not been saved.";
                    }
                    int confirmed = JOptionPane.showConfirmDialog(frame,
                            message, "Close ABBA",
                            JOptionPane.YES_NO_OPTION);
                    if (confirmed == JOptionPane.YES_OPTION) {


                        int clearMemory = JOptionPane.showConfirmDialog(frame,
                                "Close session and clear memory ?", "Close ABBA session completely",
                                JOptionPane.YES_NO_OPTION);

                        if (clearMemory == JOptionPane.YES_OPTION) {
                            cleanAllOnExit = true;
                        }

                        closeAlreadyActivated = true;
                        for (WindowListener listener : listeners) {
                            listener.windowClosing(e);
                        }
                    } else {
                        frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
                    }
                }
            }
        });
    }


    private void addExtraABBACommands() {
        msp.getContext().getService(PluginService.class)
                .getPluginsOfType(ABBACommand.class)
                .forEach(ci -> {
                    logger.debug("- ABBA command "+ci.getMenuPath().getLeaf().toString()+" ["+ci.getMenuPath()+"]");
                    MenuPath menuPath = ci.getMenuPath();
                    try {
                        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh,
                                msp.getContext(),
                                ci.loadClass(),
                                menuPath.size() - 2, "mp", msp);
                    } catch (Exception e) {
                        msp.errorMessageForUser.accept("Error while installing "+ci.getMenuPath().getLeaf(), e.getMessage());
                    }
                });
    }

    // --------------- METHODS FOR INITIALISATION - END

    public BdvMultislicePositionerView(MultiSlicePositioner msp, BdvHandle bdvh) {

        // Final variable initialization
        this.bdvh = bdvh;
        this.vp = bdvh.getViewerPanel();
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

        addFrameIcon();

        logger.debug("Installing behaviours : common");
        installCommonBehaviours();

        logger.debug("Installing behaviours : positioning");
        installPositioningBehaviours();

        logger.debug("Installing behaviours : review");
        installReviewBehaviours();

        mode = REVIEW_MODE_INT;
        setDisplayMode(POSITIONING_MODE_INT);
        toggleOverlap();

        logger.debug("Overriding standard navigation commands");
        overrideStandardNavigation();

        logger.debug("Clearing default cards and bdv functionalities of BigDataViewer");
        clearBdvDefaults();

        logger.debug("Installing menu");
        int hierarchyLevelsSkipped = 4;
        installBdvMenu(hierarchyLevelsSkipped);

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
        addDnDHandler();

        logger.debug("Adding mouse motion listener / handler");
        bdvh.getViewerPanel().getDisplay().addHandler(this);

        logger.debug("Adding modification monitor");
        addModificationMonitor();

        logger.debug("Adding confirmation close hook");
        addConfirmationCloseHook();

        logger.debug("Add clean all hook");
        addCleanAllHook();

        logger.debug("Installing extra ABBA plugin");
        addExtraABBACommands();

        logger.debug("Add closing listener");
        msp.addMultiSlicePositionerListener(this);

        // Creates previously exiting slices for this view and the linked tableview
        msp.addSliceListener(this);
        msp.getSlices().forEach(this::sliceCreated);
        msp.getSlices().forEach(tableView::sliceCreated);

        logger.debug("Register bdv view in the object service");
        msp.getContext().getService(ObjectService.class).addObject(this);
        addToCleanUpHook(() -> msp.getContext().getService(ObjectService.class).removeObject(this)); // Cleans object when view is closed
    }

    /**
     * Saves the current view on top of the state file
     */
    public void loadState() {

        try {
            CommandModule cm = msp.getContext().getService(CommandService.class)
                    .run(ABBAStateLoadCommand.class, true,"mp", msp).get();


            msp.waitForTasks();

            File f = (File) cm.getInput("state_file");
            String fileNoExt = FilenameUtils.removeExtension(f.getAbsolutePath());
            File viewFile = new File(fileNoExt+"_bdv_view.json");

            if (viewFile.exists()) {
                FileReader fileReader = new FileReader(viewFile);
                ViewState vs = new Gson().fromJson(fileReader, ViewState.class); // actions are executed during deserialization
                fileReader.close();

                msp.getReslicedAtlas().setStep(vs.atlasSlicingStep);
                this.mode = -1;
                this.setDisplayMode(vs.bdvViewMode);
                this.sliceDisplayMode = -1;
                this.setSliceDisplayMode(vs.bdvSliceViewMode);
                if (vs.overlapFactorX!=0) {
                    this.overlapFactorX = vs.overlapFactorX;
                }
                if (vs.overlapFactorY!=0) {
                    this.overlapFactorY = vs.overlapFactorY;
                }
                this.overlapMode = vs.overlapMode; updateOverlapMode();
                double[] rowPackedCopy = vs.bdvView;

                if (vs.showInfo) {showSliceInfo();} else {hideSliceInfo();}

                List<SliceSources> slices = msp.getSlices();
                if (vs.slicesStates.size() == msp.getSlices().size()) {
                    for ( int i = 0; i<vs.slicesStates.size(); i++) {
                        SliceGuiState.State state = vs.slicesStates.get(i);
                        guiState.runSlice(slices.get(i), sliceGuiState -> sliceGuiState.setState(state));
                    }
                    tableView.updateTable();
                } else {
                    System.err.println("Cannot restore display slices because other slices are present"); // TODO : could be improved
                }

                if (vs.iCurrentSlice!=null) {
                    iCurrentSlice = vs.iCurrentSlice;
                    navigateCurrentSlice();
                }

                AffineTransform3D view = new AffineTransform3D();
                view.set(rowPackedCopy);
                bdvh.getViewerPanel().state().setViewerTransform(view);

                updateSliceDisplayedPosition(null);

            } else {
                // No view -> nothing to be done
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveState() {
        //BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ABBAStateSaveCommand.class, hierarchyLevelsSkipped,"mp", msp );
        try {
            CommandModule cm = msp.getContext().getService(CommandService.class)
                    .run(ABBAStateSaveCommand.class, true,"mp", msp).get();

            msp.waitForTasks();

            boolean success = (Boolean) (cm.getOutput("success"));

            if (!success) {
                errorMessageForUser.accept("State not saved!", "Something went wrong");
                return;
            }

            File f = (File) cm.getInput("state_file");
            String fileNoExt = FilenameUtils.removeExtension(f.getAbsolutePath());
            File viewFile = new File(fileNoExt+"_bdv_view.json");

            // Ok, let's save the view File

            //serializeView(viewFile);
            List<SliceGuiState.State> states = new ArrayList<>();
            guiState.forEachSlice(sliceState -> states.add(new SliceGuiState.State(sliceState)));
            ViewState vs = new ViewState();
            vs.slicesStates = states;
            vs.showInfo = showSliceInfo;
            vs.bdvViewMode = mode;
            vs.bdvSliceViewMode = sliceDisplayMode;
            vs.overlapMode = overlapMode;
            vs.bdvView = bdvh.getViewerPanel().state().getViewerTransform().getRowPackedCopy();
            vs.atlasSlicingStep = (int) msp.getReslicedAtlas().getStep();
            vs.iCurrentSlice = iCurrentSlice;

            FileWriter writer = new FileWriter(viewFile.getAbsolutePath());
            new GsonBuilder().setPrettyPrinting().create().toJson(vs, writer);
            writer.flush();
            writer.close();

            if ((f.exists())&& (Files.size(Paths.get(f.getAbsolutePath()))>0)) {
                infoMessageForUser.accept("State saved", "Path:" + f.getAbsolutePath());
            } else {
                errorMessageForUser.accept("State not saved!", "Something went wrong");
            }

        } catch (Exception e) {
            errorMessageForUser.accept("State not saved!", e.getMessage());
            e.printStackTrace();
        }
    }

    Thread modificationMonitorThread;
    boolean stopMonitoring = false;

    private void modificationMonitor() {
        boolean previousStateModification = msp.isModifiedSinceLastSave();
        boolean previousTimeReady = msp.getNumberOfTasks()==0;
        while ((!stopMonitoring)&&(bdvh!=null)) {
            MultiSlicePositioner current_msp = this.msp;
            if (current_msp!=null) {
                if (previousStateModification != current_msp.isModifiedSinceLastSave()) {
                    previousStateModification = current_msp.isModifiedSinceLastSave();
                    BdvHandleHelper.setWindowTitle(bdvh, getViewName());
                }
                if (current_msp.getNumberOfTasks()>0) {
                    if (bdvh!=null) {
                        bdvh.getViewerPanel().getDisplay().repaint();
                        previousTimeReady = false;
                    }
                } else {
                    if (!previousTimeReady) {
                        if (bdvh!=null) {
                            bdvh.getViewerPanel().getDisplay().repaint();
                            previousTimeReady = true;
                        }
                    }
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug("Bdv view modification monitoring stopped");
    }

    public String getViewName() {
        String name = "Aligning Big Brains and Atlases - "+msp.getAtlas().getName();
        if (msp.isModifiedSinceLastSave()) {
            return name+"* (modified)";
        } else {
            return name;
        }
    }

    public void showAtlasPosition() {
        showAtlasPosition = true;
    }

    public void hideAtlasPosition() {
        showAtlasPosition = false;
    }

    public void showSliceInfo() {
        showSliceInfo = true;
    }

    public void hideSliceInfo() {
        showSliceInfo = false;
    }

    public void iniSlice(SliceSources slice) {
        debug.accept("Initializing "+slice.getName());
        guiState.created(slice);
        if (guiState.nSlices()==1) {
            iCurrentSlice = 0;
            navigateCurrentSlice();
        }
        sliceZPositionChanged(slice);
        guiState.runSlice(slice, this::updateSliceDisplayedPosition);
        if (slice.isSelected()) {
            this.sliceSelected(slice);
        } else {
            this.sliceDeselected(slice);
        }
    }

    void atlasSlicingChanged() {
        recenterBdvh();
        guiState.forEachSlice(this::updateSliceDisplayedPosition);
    }

    protected void updateSliceDisplayedPosition(SliceGuiState sliceGuiState) {
        // Sort slices along slicing axis
        if ((overlapMode == 0)&&(sliceGuiState!=null)) {
            sliceGuiState.setYShift(0);
        } else if ((overlapMode == 1)&&(sliceGuiState!=null)) {
            sliceGuiState.setYShift(1);
        } else if (overlapMode == 2) {
            // N^2 algo! Take care TODO improve
            double lastPositionAlongX = -Double.MAX_VALUE;
            double stairIndex = 0;
            List<SliceSources> slices = msp.getSlices();
            synchronized (this) { // synchronize after getting the slices to avoid deadlock
                int current = iCurrentSlice;
                if ((current > 0) && (current < slices.size())) {
                    for (int i = current; i < slices.size(); i++) {
                        SliceSources slice = slices.get(i);
                        if (slice != null) {
                            RealPoint pt = getDisplayedCenter(slice);
                            if (pt != null) {
                                double posX = pt.getDoublePosition(0);
                                if (posX >= (lastPositionAlongX + msp.sX / overlapFactorX)) {
                                    stairIndex = 0;
                                    lastPositionAlongX = posX;
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(1));
                                } else {
                                    stairIndex += overlapFactorY;
                                    final double finalStairIndex = stairIndex;
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(1 + finalStairIndex));
                                }
                            }
                        }
                    }
                    lastPositionAlongX = Double.MAX_VALUE;
                    stairIndex = 0;
                    for (int i = current; i >= 0; i--) {
                        SliceSources slice = slices.get(i);
                        if (slice != null) {
                            RealPoint pt = getDisplayedCenter(slice);
                            if (pt != null) {
                                double posX = pt.getDoublePosition(0);
                                if (posX <= (lastPositionAlongX - msp.sX / overlapFactorX)) {
                                    stairIndex = 0;
                                    lastPositionAlongX = posX;
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(1));
                                } else {
                                    stairIndex += overlapFactorY;
                                    final double finalStairIndex = stairIndex;
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(1 + finalStairIndex));
                                }
                            }
                        }
                    }
                } else {
                    // SOMETHING'S FAILING BELOW!!
                    for (SliceSources slice : slices) {
                        if (slice != null) {
                            RealPoint rp = getDisplayedCenter(slice);
                            if (rp != null) {
                                double posX = rp.getDoublePosition(0);
                                if (posX >= (lastPositionAlongX + msp.sX / overlapFactorX)) {
                                    stairIndex = 0;
                                    lastPositionAlongX = posX;
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(1));
                                } else {
                                    stairIndex += overlapFactorY;
                                    final double finalStairIndex = stairIndex;
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(1 + finalStairIndex));
                                }
                            }
                        }
                    }
                }
            }
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
        bdvh.getViewerPanel().requestRepaint();
    }

    private void overrideStandardNavigation() {
        bdvh.getKeybindings().addInputMap("blocking-multipositioner", new InputMap(), "bdv", "navigation");
        InputTriggerMap itm = new InputTriggerMap();

        itm.put(InputTrigger.getFromString("button3"), "drag translate");
        itm.put(InputTrigger.getFromString("UP"), "zoom in");
        itm.put(InputTrigger.getFromString("shift UP"), "zoom in fast");
        itm.put(InputTrigger.getFromString("scroll"), "scroll zoom");

        itm.put(InputTrigger.getFromString("DOWN"), "zoom out");
        itm.put(InputTrigger.getFromString("shift DOWN"), "zoom out fast");

        selectionLayer = new SelectionLayer(this);
        selectionLayer.addSelectionBehaviours(common_behaviours);
        refreshBlockMap();

        bdvh.getTriggerbindings().addInputTriggerMap("default_navigation", itm, "transform");
    }

    // ----- MULTIPOSITIONER LISTENER METHODS

    // --- SLICES

    @Override
    public void sliceCreated(SliceSources slice) {
        debug.accept(slice.getName()+ " created");
        iniSlice(slice);
    }

    @Override
    public void sliceDeleted(SliceSources slice) {
        debug.accept(slice.getName()+ " deleted");
        guiState.deleted(slice);
    }

    // Error : sometimes this does not return
    @Override
    public void sliceZPositionChanged(SliceSources slice) { // should not be sync : slices lock is already locked
        debug.accept(slice.getName()+ " z position changed");
        guiState.runSlice(slice, guiState -> {
            guiState.slicePositionChanged();
            updateSliceDisplayedPosition(guiState); // fail!! TODO FIX
        });
        //bdvh.getViewerPanel().requestRepaint();
    }

    @Override
    public void sliceSelected(SliceSources slice) {
        debug.accept(slice.getName()+ " selected");
        bdvh.getViewerPanel().getDisplay().repaint();
    }

    @Override
    public void sliceDeselected(SliceSources slice) {
        debug.accept(slice.getName()+ " deselected");
        bdvh.getViewerPanel().getDisplay().repaint();
    }

    @Override
    public void sliceSourcesChanged(SliceSources slice) {
        debug.accept(slice.getName()+ " slices changed");
        guiState.runSlice(slice, SliceGuiState::sourcesChanged);
    }

    @Override
    public void slicePretransformChanged(SliceSources sliceSources) {
        bdvh.getViewerPanel().requestRepaint();
    }

    @Override
    public void sliceKeyOn(SliceSources slice) {
        bdvh.getViewerPanel().getDisplay().repaint();
    }

    @Override
    public void sliceKeyOff(SliceSources slice) {
        bdvh.getViewerPanel().getDisplay().repaint();
    }

    // --- ROI

    @Override
    public void roiChanged() {
        double[] currentRoi = msp.getROI();
        roiPX = currentRoi[0];
        roiPY = currentRoi[1];
        roiSX = currentRoi[2];
        roiSY = currentRoi[3];
        bdvh.getViewerPanel().requestRepaint();
    }

    // --- ACTIONS

    @Override
    public void actionEnqueue(SliceSources slice, CancelableAction action) {
        bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
    }

    @Override
    public void actionStarted(SliceSources slice, CancelableAction action) {
        bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
    }

    @Override
    public void actionFinished(SliceSources slice, CancelableAction action, boolean result) {
        bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
    }

    @Override
    public void actionCancelEnqueue(SliceSources slice, CancelableAction action) {
        bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
    }

    @Override
    public void actionCancelStarted(SliceSources slice, CancelableAction action) {
        bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
    }

    @Override
    public void actionCancelFinished(SliceSources slice, CancelableAction action, boolean result) {
        bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
    }

    // ----- MULTIPOSITIONER LISTENER METHODS - END

    // --------------------------------------------------------- SETTING MODES

    /**
     * Toggles between positioning and registration mode
     */
    public void nextMode() {
        setDisplayMode(1-mode);
    }

    public void setDisplayMode(int mode) {
        if (this.mode!=mode) {
            int oldMode = mode;
            this.mode = mode;
            modeChanged(mode, oldMode);
        }
    }

    private void modeChanged(int mode, int oldMode) {
        guiState.forEachSlice(sliceGuiState -> {
            sliceGuiState.sliceDisplayChanged();
            sliceGuiState.slicePositionChanged();
        });

        if (mode == POSITIONING_MODE_INT) {
            guiState.forEachSlice(SliceGuiState::enableGraphicalHandles);
            bdvh.getTriggerbindings().removeInputTriggerMap(REVIEW_BEHAVIOURS_KEY);
            bdvh.getTriggerbindings().removeBehaviourMap(REVIEW_BEHAVIOURS_KEY);
            positioning_behaviours.install(bdvh.getTriggerbindings(), POSITIONING_BEHAVIOURS_KEY);
            navigateCurrentSlice();
            refreshBlockMap();
        }

        if (mode == REVIEW_MODE_INT) {
            guiState.forEachSlice(SliceGuiState::disableGraphicalHandles);
            bdvh.getTriggerbindings().removeInputTriggerMap(POSITIONING_BEHAVIOURS_KEY);
            bdvh.getTriggerbindings().removeBehaviourMap(POSITIONING_BEHAVIOURS_KEY);
            review_behaviours.install(bdvh.getTriggerbindings(), REVIEW_BEHAVIOURS_KEY);
            navigateCurrentSlice();
            refreshBlockMap();
        }

        modeListeners.forEach(modeListener -> modeListener.modeChanged(this, oldMode, mode));
    }

    public void setSliceDisplayMode (int sliceDisplayMode) {
        if (this.sliceDisplayMode!=sliceDisplayMode) {
            this.sliceDisplayMode = sliceDisplayMode;
            guiState.forEachSlice(SliceGuiState::sliceDisplayChanged);
        }
    }

    public void toggleOverlap() {
        overlapMode+=1;
        if (overlapMode==3) overlapMode = 0;
        updateOverlapMode();
    }

    private void updateOverlapMode() {
        if (overlapMode==2) {
            updateSliceDisplayedPosition(null);
        } else {
            guiState.forEachSlice(this::updateSliceDisplayedPosition);
        }
    }

    // ---------------- Optional resources monitor (fails with PyImageJ, that's why it's optional)

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

    /**
     * @param slice queried slice
     * @return the position of the center of the slice when displayed in BigDataViewer
     */
    public RealPoint getDisplayedCenter(SliceSources slice) {
        if (mode==POSITIONING_MODE_INT) {
            double slicingAxisSnapped = (((int) ((slice.getSlicingAxisPosition()+guiState.getXShift(slice)) / msp.sizePixX)) * msp.sizePixX);
            double posX = ((slicingAxisSnapped) / msp.sizePixX * msp.sX / msp.getReslicedAtlas().getStep()) + (0.5) * (msp.sX);
            double posY = msp.sY * guiState.getYShift(slice);
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

    // ----------------- NAVIGATION

    /**
     * Center bdv on current slice (iCurrentSlice)
     */
    public void navigateCurrentSlice() {
        List<SliceSources> sortedSlices = msp.getSlices();
        if (iCurrentSlice >= sortedSlices.size()) {
            iCurrentSlice = 0;
        }
        if (sortedSlices.size() > 0) {
            SliceSources slice = sortedSlices.get(iCurrentSlice);
            if (slice!=null) {
                guiState.runSlice(slice, SliceGuiState::isCurrent);
                centerBdvViewOn(sortedSlices.get(iCurrentSlice));
            }
        }
    }

    /**
     * Center bdv on next slice (iCurrentSlice + 1)
     */
    public void navigateNextSlice() {
        List<SliceSources> sortedSlices = msp.getSlices();
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
            if ((previousSliceIndex>=0)&&(previousSliceIndex<sortedSlices.size())) { // Could have been deleted
                guiState.runSlice(sortedSlices.get(previousSliceIndex), SliceGuiState::isNotCurrent);
            }
            guiState.runSlice(sortedSlices.get(iCurrentSlice), SliceGuiState::isCurrent);
            if (overlapMode==2) updateSliceDisplayedPosition(null);
            centerBdvViewOn(sortedSlices.get(iCurrentSlice), true, previousSlice);
        }
    }

    /**
     * Center bdv on previous slice (iCurrentSlice - 1)
     */
    public void navigatePreviousSlice() {
        int previousSliceIndex = iCurrentSlice;
        iCurrentSlice--;
        List<SliceSources> sortedSlices = msp.getSlices();

        if (iCurrentSlice < 0) {
            iCurrentSlice = sortedSlices.size() - 1;
        }

        if (sortedSlices.size() > 0) {
            SliceSources previousSlice = null;
            if ((previousSliceIndex>=0)&&(previousSliceIndex<sortedSlices.size())) {
                previousSlice = sortedSlices.get(previousSliceIndex);
            }
            if ((previousSliceIndex>=0)&&(previousSliceIndex<sortedSlices.size())) { // Could have been deleted
                guiState.runSlice(sortedSlices.get(previousSliceIndex), SliceGuiState::isNotCurrent);
            }
            guiState.runSlice(sortedSlices.get(iCurrentSlice), SliceGuiState::isCurrent);
            if (overlapMode==2) updateSliceDisplayedPosition(null);
            centerBdvViewOn(sortedSlices.get(iCurrentSlice), true, previousSlice);
        }
    }

    public void centerBdvViewOn(SliceSources slice) {
        centerBdvViewOn(slice, false, null);
    }

    // Center bdv on a slice
    public void centerBdvViewOn(SliceSources current_slice, boolean maintainoffset, SliceSources previous_slice) {

        RealPoint offset = new RealPoint(3);

        if ((maintainoffset)&&(previous_slice!=null)) {

            RealPoint oldCenter = getDisplayedCenter(previous_slice);

            RealPoint centerScreen = getCurrentBdvCenter();
            offset.setPosition(-oldCenter.getDoublePosition(0) + centerScreen.getDoublePosition(0), 0);
            offset.setPosition(-oldCenter.getDoublePosition(1) + centerScreen.getDoublePosition(1), 1);

            if (Math.abs(offset.getDoublePosition(0))>msp.sX/2.0) {maintainoffset = false;}
            if (Math.abs(offset.getDoublePosition(1))>msp.sY/2.0) {maintainoffset = false;}

        } else {
            maintainoffset = false;
        }

        RealPoint centerSlice = getDisplayedCenter(current_slice);

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

    public Integer[] getSliceHandleCoords(SliceSources slice) {
        return guiState.getSliceHandleCoords(slice);
    }

    public int getCurrentSliceIndex() {
        return iCurrentSlice;
    }

    //------------------------------------------ DRAG BEHAVIOURS

    private final AtomicBoolean dragActionInProgress = new AtomicBoolean(false);

    boolean startDragAction() {
        boolean result = dragActionInProgress.compareAndSet(false, true);
        logger.debug("Attempt to do a drag action : successful = "+result);
        return result;
    }

    void stopDragAction() {
        logger.debug("Stopping a drag action");
        dragActionInProgress.set(false);
    }

    //------------------------------ Multipositioner Graphical handles

    @Override
    public void disabled(GraphicalHandle gh) {

    }

    @Override
    public void enabled(GraphicalHandle gh) {

    }

    @Override
    public void hover_in(GraphicalHandle gh) {
        gh_below_mouse.add(gh);
        if (gh_below_mouse.size() == 1) {
            block();
        }
    }

    @Override
    public void hover_out(GraphicalHandle gh) {
        gh_below_mouse.remove(gh);
        if (gh_below_mouse.size() == 0) {
            unblock();
        }
    }

    @Override
    public void created(GraphicalHandle gh) {

    }

    @Override
    public void removed(GraphicalHandle gh) {
        if (gh_below_mouse.contains(gh)) {
            gh_below_mouse.remove(gh);
            if (gh_below_mouse.size() == 0) unblock();
        }
        ghs.remove(gh);
    }

    public Object getCurrentSlice() {
        List<SliceSources> sortedSlices = msp.getSlices();

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

    private void drawSliceInfo(Graphics2D g, List<SliceSources> slicesCopy) {
        RealPoint mouseWindowPosition = new RealPoint(2);
        bdvh.getViewerPanel().getMouseCoordinates(mouseWindowPosition);
        // Slice info is displayed if the mouse is over the round slice handle
        Optional<SliceSources> optSlice = slicesCopy.stream()
                .filter(slice -> {
                    Integer[] coords = guiState.getSliceHandleCoords(slice);
                    if (coords==null) return false;
                    int radius = guiState.getBdvHandleRadius(slice);
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

    @Override
    public void mouseDragged(MouseEvent e) {
        this.ghs.forEach(gh -> gh.mouseDragged(e));
        guiState.forEachSlice(guiState -> guiState.ghs.forEach(gh -> gh.mouseDragged(e)));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        this.ghs.forEach(gh -> gh.mouseMoved(e));
        guiState.forEachSlice(guiState -> guiState.ghs.forEach(gh -> gh.mouseMoved(e)));
    }

    public Boolean getChannelVisibility(SliceSources slice, int iChannel) {
        if (guiState == null) return false;
        if (guiState.sliceGuiState == null) return false;
        SliceGuiState guiStateSlice = guiState.sliceGuiState.get(slice);
        if (guiStateSlice!=null) {
            return guiStateSlice.getChannelVisibility(iChannel);
        } else {
            return false;
        }
    }

    public Displaysettings getDisplaySettings(SliceSources slice, int iChannel) {
        if (guiState == null) return new Displaysettings(-1);
        if (guiState.sliceGuiState == null) return new Displaysettings(-1);
        SliceGuiState guiStateSlice = guiState.sliceGuiState.get(slice);
        if (guiStateSlice!=null) {
            return guiStateSlice.getDisplaySettings(iChannel);
        } else {
            return new Displaysettings(-1);
        }
    }

    public Boolean getSliceVisibility(SliceSources slice) {
        if (guiState == null) return false;
        if (guiState.sliceGuiState == null) return false;
        if (guiState.sliceGuiState.get(slice) == null) return false;
        SliceGuiState guiStateSlice = guiState.sliceGuiState.get(slice);
        if (guiStateSlice!=null) {
            return guiStateSlice.getSliceVisibility(); //
        } else {
            return false;
        }
    }

    @Override
    public void closing(MultiSlicePositioner msp) {
        if ((msp == this.msp)&&(!cleanAllOnExit)) {
            IJ.error("This ABBA session has been closed outside BigDataViewer, expect errors!");
        }
    }

    public void setSliceDisplayMinMax(SliceSources slice, int iChannel, double display_min, double display_max) {
        guiState.runSlice(slice,
                sliceGuiState -> {
                    if (iChannel<sliceGuiState.nChannels) {
                        Displaysettings ds = sliceGuiState.getDisplaySettings(iChannel);
                        ds.min = display_min;
                        ds.max = display_max;
                        sliceGuiState.setDisplaySettings(iChannel, ds);
                    }
                });
        tableView.sliceDisplaySettingsChanged(slice);
    }

    double overlapFactorX = 1.0;
    double overlapFactorY = 1.0;

    public void setOverlapFactorX(int value) {
        double newValue = 0.1 + 3.0 - ((value/100.0) * 3.0);
        if (newValue!=overlapFactorX) {
            overlapFactorX = newValue;
            if (overlapMode == 2) {
                updateOverlapMode();
            }
        }
    }

    public void setOverlapFactorY(int value) {
        double newValue = 0.1 + 3.0 - ((value/100.0) * 3.0);
        if (newValue!=overlapFactorY) {
            overlapFactorY = newValue;
            if (overlapMode == 2) {
                updateOverlapMode();
            }
        }
    }

    class InnerOverlay extends BdvOverlay {
        int drawCounter = 0;
        Color color = new Color(128,112,50,200);
        Stroke stroke = new BasicStroke(4);
        @Override
        protected void draw(Graphics2D g) {
            drawCounter++;
            drawCounter = drawCounter%21;
            // Gets a copy of the slices to avoid concurrent exception
            List<SliceSources> slicesCopy = msp.getSlices();

            // Gets current bdv view position
            AffineTransform3D bdvAt3D = new AffineTransform3D();
            bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);

            drawDragAndDropRectangle(g, bdvAt3D);

            guiState.forEachSlice(sliceGuiState -> {
                sliceGuiState.drawGraphicalHandles(g);
                List<CancelableAction> actions = msp.getActionsFromSlice(sliceGuiState.slice);
                Integer[] pos = sliceGuiState.getSliceHandleCoords();
                if (actions!=null) {
                    int iPos = 0;
                    int totalNumberOfRegistration = 0;
                    for (int idx = 0; idx<actions.size(); idx ++) {
                        CancelableAction action = actions.get(idx);
                        if (!action.isHidden()) {
                            if (action instanceof RegisterSliceAction) {
                                if (action.getSliceSources().getActionState(action).equals("(done)"))
                                totalNumberOfRegistration++;
                            }
                        }
                    }
                    int countNumberOfRegistration = 0;
                    for (int idx = 0; idx<actions.size(); idx ++) {
                        CancelableAction action = actions.get(idx);
                        if (!action.isHidden()) {
                            iPos++;
                            if (action instanceof RegisterSliceAction) {
                                // Is it viewed or not ? How to know this
                                countNumberOfRegistration++;
                                if (totalNumberOfRegistration-registrationStepBack<countNumberOfRegistration) {
                                    if (action.getSliceSources().getActionState(action).equals("(done)")) {
                                        action.drawAction(g, pos[0], pos[1] + iPos * 10, 0.85);
                                    } else {
                                        action.drawAction(g, pos[0], pos[1] + iPos * 10, 1.2);
                                    }
                                } else {
                                    action.drawAction(g, pos[0], pos[1] + iPos * 10, 1.2);
                                }
                            } else {
                                action.drawAction(g, pos[0], pos[1] + iPos * 10, 1.0);
                            }
                        }
                    }
                }
            });

            drawCurrentSliceOverlay(g, slicesCopy);

            if (mode == POSITIONING_MODE_INT) drawSetOfSliceControls(g, bdvAt3D, slicesCopy);

            if (selectionLayer != null) selectionLayer.draw(g);

            if (showAtlasPosition) drawAtlasPosition(g);

            if (showSliceInfo) drawSliceInfo(g, slicesCopy);

            int w = bdvh.getViewerPanel().getWidth();
            int h = bdvh.getViewerPanel().getHeight();

            g.setColor(color);
            g.setStroke(stroke);

            if (msp.getNumberOfTasks()>0) {
                g.drawString(""+msp.getNumberOfTasks(), w-30-4, h-18-4);
                if (drawCounter<=10) {
                    g.drawArc(w - 54, h - 54, 50, 50, 0, drawCounter * 36);
                } else {
                    g.drawArc(w - 54, h - 54, 50, 50, (drawCounter-10) * 36, 360-((drawCounter-10) * 36));
                }
            }

            if (msp.isModifiedSinceLastSave()) {
                g.drawString("Modified since last save!", 5, h-15);
            } else {
                g.drawString("No modification", 5, h-15);
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
                node = node.parent();
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

    private void drawSetOfSliceControls(Graphics2D g, AffineTransform3D bdvAt3D, List<SliceSources> slicesCopy) {


        if (slicesCopy.stream().anyMatch(SliceSources::isSelected)) {

            List<SliceSources> sortedSelected = msp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
            RealPoint precedentPoint = null;

            for (int i = 0; i < sortedSelected.size(); i++) {
                SliceSources slice = sortedSelected.get(i);
                //SliceGuiState sliceState = sliceGuiState.get(slice);
                Integer[] coords = guiState.getSliceHandleCoords(slice);//sliceState.getSliceHandleCoords();
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
                    RealPoint handleLeftPoint = this.getDisplayedCenter(slice);
                    handleLeftPoint.setPosition(+sY/2.0, 1);
                    bdvAt3D.apply(handleLeftPoint, handleLeftPoint);

                    leftPosition[0] = (int) handleLeftPoint.getDoublePosition(0);
                    leftPosition[1] = (int) handleLeftPoint.getDoublePosition(1);
                }

                if (i == sortedSelected.size() - 1) {
                    RealPoint handleRightPoint = this.getDisplayedCenter(slice);
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


        Color colorNotSelected = new Color(255, 255, 0, 64);
        Color colorSelected = new Color(0, 255, 0, 180);
        //Stroke stroke = new BasicStroke(4);


        // Set the stroke of the copy, not the original
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{9}, 0);
        g.setStroke(dashed);
        // draw dashed line from handle
        slicesCopy.forEach(slice -> {

            Integer[] coordSliceCenter = guiState.getSliceHandleCoords(slice);

            RealPoint handlePoint = this.getDisplayedCenter(slice);
            handlePoint.setPosition(+sY/2.0, 1);
            bdvAt3D.apply(handlePoint, handlePoint);
            if (slice.isSelected()) {
                g.setColor(colorSelected);
            } else {

                g.setColor(colorNotSelected);
            }

            g.drawLine(coordSliceCenter[0], coordSliceCenter[1],
                    (int) handlePoint.getDoublePosition(0), (int) handlePoint.getDoublePosition(1));

        });
    }

    private void drawCurrentSliceOverlay(Graphics2D g, List<SliceSources> slicesCopy) {

        if (iCurrentSlice != -1 && slicesCopy.size() > iCurrentSlice) {
            SliceSources slice = msp.getSlices().get(iCurrentSlice);
            //listeners.forEach(listener -> listener.isCurrentSlice(slice));
            g.setColor(new Color(255, 255, 255, 128));
            g.setStroke(new BasicStroke(5));
            Integer[] coords = guiState.getSliceHandleCoords(slice);//sliceGuiState.get(slice).getSliceHandleCoords();
            RealPoint sliceCenter = new RealPoint(coords[0], coords[1], 0);
            if (slice.isKeySlice()) {
                g.drawOval((int) sliceCenter.getDoublePosition(0) - 20, (int) sliceCenter.getDoublePosition(1) - 20, 39, 39);
            } else {
                g.drawOval((int) sliceCenter.getDoublePosition(0) - 15, (int) sliceCenter.getDoublePosition(1) - 15, 29, 29);
            }
            Integer[] c = {255,255,255,128};
            g.setColor(new Color(c[0], c[1], c[2], c[3]));
            g.setFont(new Font("TimesRoman", Font.BOLD, 18));
            g.drawString("\u25C4 \u25BA", (int) (sliceCenter.getDoublePosition(0) - 15), (int) (sliceCenter.getDoublePosition(1) - 20));

            String name = slice.getName();
            int yOffset = 20;
            if (mode==REVIEW_MODE_INT) yOffset = 130;
            if (slice.isKeySlice()) name += " [Key]";
            DecimalFormat df = new DecimalFormat("00.000");
            g.drawString("Z: "+df.format(slice.getSlicingAxisPosition())+" mm", 15, yOffset+20);
            g.drawString(name, 15, yOffset);
            List<CancelableAction> actions = new ArrayList<>(msp.getActionsFromSlice(slice)); // Copy useful ?
            actions = AlignerState.filterSerializedActions(actions); // To get rid of useless actions for the user
            g.setFont(new Font("TimesRoman", Font.PLAIN, 16));
            int index = 0;
            for (CancelableAction action : actions) {
                if ((!(action instanceof MoveSliceAction))&&(!(action instanceof CreateSliceAction))) {
                    g.drawString(action.toString(), 15, yOffset + 5 + (index + 2) * 15); // -30 because of Bdv message
                    index++;
                }
            }

        }
    }

    // ----- MODE CHANGE LISTENERS

    List<ModeListener> modeListeners = new ArrayList<>();

    public void addModeListener(ModeListener modeListener) {
        modeListeners.add(modeListener);
    }

    public void removeModeListener(ModeListener modeListener) {
        modeListeners.remove(modeListener);
    }

    public interface ModeListener {
        void modeChanged(BdvMultislicePositionerView mp, int oldmode, int newmode);
    }


    private void drawDragAndDropRectangle(Graphics2D g, AffineTransform3D bdvAt3D) {
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

    class SynchronizedSliceGuiState {
        private final Map<SliceSources, SliceGuiState> sliceGuiState = new ConcurrentHashMap<>();

        synchronized
        void created(final SliceSources slice) {
            sliceGuiState.put(slice, new SliceGuiState(BdvMultislicePositionerView.this, slice, bdvh));
            SliceGuiState.FilterDisplay fd = iChannel -> {
                if (BdvMultislicePositionerView.this.mode == REVIEW_MODE_INT) {
                    return slice.equals(BdvMultislicePositionerView.this.getCurrentSlice());
                }
                switch (BdvMultislicePositionerView.this.sliceDisplayMode) {
                    case NO_SLICE_DISPLAY_MODE:
                        return false;
                    case ALL_SLICES_DISPLAY_MODE:
                        return true;
                    case CURRENT_SLICE_DISPLAY_MODE:
                        return slice.equals(BdvMultislicePositionerView.this.getCurrentSlice());
                    default:
                        return false;
                }
            };
            sliceGuiState.get(slice).addDisplayFilters(fd);
            sliceGuiState.get(slice).created();
        }

        synchronized
        void deleted(SliceSources slice) {
            sliceGuiState.get(slice).deleted();
            sliceGuiState.remove(slice);
        }

        //synchronized
        int nSlices() {
            return sliceGuiState.values().size();
        }

        //synchronized
        void forEachSlice(Consumer<SliceGuiState> consumer) {
            sliceGuiState.values().forEach(consumer);
        }

        //synchronized
        void runSlice(SliceSources slice, Consumer<SliceGuiState> consumer) {
            SliceGuiState slice_gui = sliceGuiState.get(slice);
            if (slice_gui!=null) {
                consumer.accept(slice_gui);
            } else {
                logger.debug("Unavailable slice state, cannot perform operation "+consumer+" on slice "+slice);
            }
        }

        //synchronized
        double getYShift(SliceSources slice) {
            SliceGuiState slice_gui = sliceGuiState.get(slice);
            if (slice_gui!=null) {
                return slice_gui.getYShift();
            } else {
                logger.debug("Unavailable slice state, cannot perform operation getYShift on slice "+slice);
                return 0;
            }
        }

        //synchronized
        double getXShift(SliceSources slice) {
            SliceGuiState slice_gui = sliceGuiState.get(slice);
            if (slice_gui!=null) {
                return slice_gui.getXShift();
            } else {
                logger.debug("Unavailable slice state, cannot perform operation getXShift on slice "+slice);
                return 0;
            }
        }

        //synchronized
        Integer[] getSliceHandleCoords(SliceSources slice) {
            SliceGuiState slice_gui = sliceGuiState.get(slice);
            if (slice_gui!=null) {
                return slice_gui.getSliceHandleCoords();
            } else {
                logger.debug("Unavailable slice state, cannot perform operation getSliceHandleCoords on slice "+slice);
                return new Integer[]{0,0,0};
            }
        }

        //synchronized
        int getBdvHandleRadius(SliceSources slice) {
            SliceGuiState slice_gui = sliceGuiState.get(slice);
            if (slice_gui!=null) {
                return slice_gui.getBdvHandleRadius();
            } else {
                logger.debug("Unavailable slice state, cannot perform operation getBdvHandleRadius on slice "+slice);
                return 10;
            }
        }

        public void clear() {
            sliceGuiState.clear();
        }
    }

    public static class ViewState {
        List<SliceGuiState.State> slicesStates;
        double[] bdvView;
        int bdvViewMode;
        int bdvSliceViewMode;
        int overlapMode;
        boolean showInfo;
        int atlasSlicingStep;
        Integer iCurrentSlice;
        double overlapFactorX = 1.0;
        double overlapFactorY = 1.0;
    }

}
