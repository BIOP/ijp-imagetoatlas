package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvHandleFrame;
import bdv.util.BdvOptions;
import bdv.util.BdvOverlay;
import bdv.util.BdvStackSource;
import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.util.source.alpha.AlphaConverter;
import bdv.util.source.alpha.IAlphaSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerPanel;
import bdv.viewer.animate.MessageOverlayAnimator;
import ch.epfl.biop.ResourcesMonitor;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.CreateSliceAction;
import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import ch.epfl.biop.atlas.aligner.DeleteSliceAction;
import ch.epfl.biop.atlas.aligner.MoveSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.adapter.AlignerState;
import ch.epfl.biop.atlas.aligner.command.ABBACheckForUpdateCommand;
import ch.epfl.biop.atlas.aligner.command.ABBACiteInfoCommand;
import ch.epfl.biop.atlas.aligner.command.ABBADocumentationCommand;
import ch.epfl.biop.atlas.aligner.command.ABBAForumHelpCommand;
import ch.epfl.biop.atlas.aligner.command.ABBAImportDemoSlicesCommand;
import ch.epfl.biop.atlas.aligner.command.ABBAStartLogCommand;
import ch.epfl.biop.atlas.aligner.command.ABBAStateLoadCommand;
import ch.epfl.biop.atlas.aligner.command.ABBAStateSaveCommand;
import ch.epfl.biop.atlas.aligner.command.ABBAUserFeedbackCommand;
import ch.epfl.biop.atlas.aligner.command.ABBAGenerateMethodsPrompt;
import ch.epfl.biop.atlas.aligner.command.AtlasSlicingAdjusterCommand;
import ch.epfl.biop.atlas.aligner.command.DeepSliceDocumentationCommand;
import ch.epfl.biop.atlas.aligner.command.ExportAtlasToImageJCommand;
import ch.epfl.biop.atlas.aligner.command.ExportDeformationFieldToImageJCommand;
import ch.epfl.biop.atlas.aligner.command.ExportRegionsToRoiManagerCommand;
import ch.epfl.biop.atlas.aligner.command.ExportRegionsToRoisetFileCommand;
import ch.epfl.biop.atlas.aligner.command.ExportRegistrationToQuPathCommand;
import ch.epfl.biop.atlas.aligner.command.ExportResampledSlicesToBDVSourceCommand;
import ch.epfl.biop.atlas.aligner.command.ExportSlicesOriginalDataToImageJCommand;
import ch.epfl.biop.atlas.aligner.command.ExportSlicesToBDVCommand;
import ch.epfl.biop.atlas.aligner.command.ExportSlicesToBDVJsonDatasetCommand;
import ch.epfl.biop.atlas.aligner.command.ExportSlicesToImageJCommand;
import ch.epfl.biop.atlas.aligner.command.ExportSlicesToQuickNIIDatasetCommand;
import ch.epfl.biop.atlas.aligner.command.ImportSliceFromImagePlusCommand;
import ch.epfl.biop.atlas.aligner.command.ImportSliceFromSourcesCommand;
import ch.epfl.biop.atlas.aligner.command.ImportSlicesFromFilesCommand;
import ch.epfl.biop.atlas.aligner.command.ImportSlicesFromQuPathCommand;
import ch.epfl.biop.atlas.aligner.command.MirrorDoCommand;
import ch.epfl.biop.atlas.aligner.command.MirrorUndoCommand;
import ch.epfl.biop.atlas.aligner.command.RasterSlicesCommand;
import ch.epfl.biop.atlas.aligner.command.RasterSlicesDeformationCommand;
import ch.epfl.biop.atlas.aligner.command.RegisterSlicesBigWarpCommand;
import ch.epfl.biop.atlas.aligner.command.RegisterSlicesCopyAndApplyCommand;
import ch.epfl.biop.atlas.aligner.command.RegisterSlicesDeepSliceWebCommand;
import ch.epfl.biop.atlas.aligner.command.RegisterSlicesDeepSliceLocalCommand;
import ch.epfl.biop.atlas.aligner.command.RegisterSlicesEditLastCommand;
import ch.epfl.biop.atlas.aligner.command.RegisterSlicesElastixAffineCommand;
import ch.epfl.biop.atlas.aligner.command.RegisterSlicesElastixSplineCommand;
import ch.epfl.biop.atlas.aligner.command.RegisterSlicesRemoveLastCommand;
import ch.epfl.biop.atlas.aligner.command.ReindexSlicesCommand;
import ch.epfl.biop.atlas.aligner.command.RotateSlicesCommand;
import ch.epfl.biop.atlas.aligner.command.SetSlicesDeselectedCommand;
import ch.epfl.biop.atlas.aligner.command.SetSlicesDisplayRangeCommand;
import ch.epfl.biop.atlas.aligner.command.SetSlicesSelectedCommand;
import ch.epfl.biop.atlas.aligner.command.SetSlicesThicknessCommand;
import ch.epfl.biop.atlas.aligner.command.SetSlicesThicknessMatchNeighborsCommand;
import ch.epfl.biop.atlas.aligner.command.SliceAffineTransformCommand;
import ch.epfl.biop.atlas.aligner.gui.MultiSliceContextMenuClickBehaviour;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.AtlasAdjustDisplayCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.AtlasInfoPanel;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.EditPanel;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.NavigationPanel;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.SliceDefineROICommand;
import ch.epfl.biop.atlas.aligner.plugin.ABBACommand;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.bdv.gui.graphicalhandle.GraphicalHandle;
import ch.epfl.biop.bdv.gui.graphicalhandle.GraphicalHandleListener;
import ch.epfl.biop.wrappers.deepslice.ij2commands.DeepSlicePrefsSet;
import ch.epfl.biop.wrappers.ij2command.BiopWrappersSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ij.IJ;
import ij.plugin.frame.Recorder;
import mpicbg.spim.data.sequence.Tile;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.real.FloatType;
import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.scijava.MenuPath;
import org.scijava.cache.CacheService;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.object.ObjectService;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.PluginService;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import org.scijava.ui.swing.widget.SwingInputHarvester;
import org.scijava.ui.swing.widget.SwingInputPanel;
import org.scijava.widget.InputPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.scijava.BdvScijavaHelper;
import sc.fiji.bdvpg.scijava.ScijavaSwingUI;
import sc.fiji.bdvpg.scijava.services.ui.swingdnd.BdvTransferHandler;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import spimdata.util.Displaysettings;

import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static bdv.ui.BdvDefaultCards.DEFAULT_SOURCEGROUPS_CARD;
import static bdv.ui.BdvDefaultCards.DEFAULT_SOURCES_CARD;
import static bdv.ui.BdvDefaultCards.DEFAULT_VIEWERMODES_CARD;
import static bdv.util.source.alpha.AlphaSourceHelper.ALPHA_SOURCE_KEY;

public class BdvMultislicePositionerView implements MultiSlicePositioner.SliceChangeListener, GraphicalHandleListener, MouseMotionListener, MultiSlicePositioner.MultiSlicePositionerListener {

    final public static String NAME_CARD_ATLAS_INFORMATION = "Atlas Information";
    final public static String NAME_CARD_ATLAS_DISPLAY = "Atlas Display";
    final public static String NAME_CARD_DISPLAY_NAVIGATION = "Display & Navigation"; //"Atlas Display";
    final public static String NAME_CARD_EDIT_SLICES = "Edit Selected Slices";
    final public static String NAME_CARD_ATLAS_SLICING = "Atlas Slicing";
    final public static String NAME_CARD_DEFINE_ROI = "Define region of interest for registration";
    final public static String NAME_CARD_CURRENT_SLICE_INFO = "Current slice info";
    final public static String NAME_CARD_SLICES_DISPLAY = "Slices Display";

    public MultiSlicePositioner msp; // TODO : make accessor
    final BdvHandle bdvh;

    TableView tableView;

    protected static final Logger logger = LoggerFactory.getLogger(MultiSlicePositioner.class);

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

    private final Runnable atlasSlicingListener;

    protected final SynchronizedSliceGuiState guiState = new SynchronizedSliceGuiState();

    private MultiSliceContextMenuClickBehaviour mscClick;

    private boolean showSliceInfo; // use accessors

    // Flag for overlaying the position of the mouse on the atlas + the region name
    private boolean showAtlasPosition = true; // use accessors

    private final double sX, sY;

    private double roiPX, roiPY, roiSX, roiSY;

    private TransferHandler transferHandler;

    private ViewerPanel vp;

    private int previouszStep;

    final List<Runnable> extraCleanUp = new ArrayList<>();

    private int iCurrentSlice = 0;

    private long lastErrorMessageTimestampMs = -1;
    private long delayBetweenMessagesMs = 5000;

    private void blockingErrorMessageForUsers(String title, String message) {
        if ((System.currentTimeMillis()-lastErrorMessageTimestampMs)>delayBetweenMessagesMs) {
            JOptionPane.showMessageDialog(new JFrame(), message, title, JOptionPane.ERROR_MESSAGE);
        } else {
            infoMessageForUser(title, message); // Non blocking
        }
        lastErrorMessageTimestampMs = System.currentTimeMillis();
    }

    private void warningMessageForUser(String title, String message) {
        JOptionPane.showMessageDialog(new JFrame(), message, title, JOptionPane.WARNING_MESSAGE);
    }

    final MessageOverlayAnimator moa;

    private void infoMessageForUser(String title, String message) {
        // Multi-line not supported
        message = message.replaceAll("\n", " | ");
        if (title.isEmpty()) {
            moa.add(message);
        } else {
            moa.add(title+" | "+message);
        }
        bdvh.getViewerPanel().getDisplay().repaint();
    }

    // Maximum right position of the selected slices
    final Integer[] rightPosition = new Integer[]{0, 0, 0};

    // Maximum left position of the selected slices
    final Integer[] leftPosition = new Integer[]{0, 0, 0};

    private int sliceDisplayMode = 0;

    final static public int NO_SLICE_DISPLAY_MODE = 2;
    final static public int ALL_SLICES_DISPLAY_MODE = 0;
    final static public int CURRENT_SLICE_DISPLAY_MODE = 1;

    // Current coordinate where Sources are dragged
    private int iSliceNoStep;

    protected int overlapMode = OVERLAP_BELOW_ATLAS;

    final private static int OVERLAP_ON_ATLAS = 0;
    final private static int OVERLAP_BELOW_ATLAS = 1;
    final private static int OVERLAP_BELOW_ATLAS_STAIRS = 2;


    //------------------------------ Multipositioner Graphical handles

    Set<GraphicalHandle> ghs = new HashSet<>();

    final Set<GraphicalHandle> gh_below_mouse = new HashSet<>();

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
        BdvHandleHelper.removeCard(bdvh, DEFAULT_SOURCEGROUPS_CARD);
        BdvHandleHelper.removeCard(bdvh, DEFAULT_VIEWERMODES_CARD);
        if (SwingUtilities.isEventDispatchThread()) {
            bdvh.getCardPanel().setCardExpanded(DEFAULT_SOURCES_CARD, false);
            BdvScijavaHelper.clearBdvHandleMenuBar(bdvh);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    bdvh.getCardPanel().setCardExpanded(DEFAULT_SOURCES_CARD, false);
                    BdvScijavaHelper.clearBdvHandleMenuBar(bdvh);
                });
            } catch (Exception e) {
                blockingErrorMessageForUsers("Error when clearing Bdv Defaults", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void installBdvMenu(int hierarchyLevelsSkipped) {

        // Load and Save state
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"File>Save State (+View)",0, () -> new Thread(this::saveState).start(), "/graphics/SaveState.png", "Save ABBA state file");
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"File>Load State (+View)",0, () -> new Thread(this::loadState).start(), "/graphics/LoadState.png", "Load ABBA state file");
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "File>Import Demo Sections", ABBAImportDemoSlicesCommand.class, "mp", msp );

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Undo [Ctrl+Z]",0, msp::cancelLastAction, "/graphics/ABBAUndo.png", "Undo Last Action");
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Redo [Ctrl+Shift+Z]",0, msp::redoAction, "/graphics/ABBARedo.png", "Redo Last Action");
        BdvScijavaHelper.addSeparator(bdvh,"Edit");

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Configuration>Mouse Options>Show Atlas Position",0, this::showAtlasPosition);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Configuration>Mouse Options>Hide Atlas Position",0, this::hideAtlasPosition);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Configuration>Mouse Options>Show Slice Info",0, this::showSliceInfo);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Edit>Configuration>Mouse Options>Hide Slice Info",0, this::hideSliceInfo);

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Edit>Configuration>Set Elastix & Transformix path", BiopWrappersSet.class);

        // Slice importer
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Import>Import QuPath Project", ImportSlicesFromQuPathCommand.class, "mp", msp );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Import>Import Current ImageJ Window", ImportSliceFromImagePlusCommand.class, "mp", msp );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Import>Import With Bio-Formats", ImportSlicesFromFilesCommand.class, "mp", msp );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Import>Import Sources", ImportSliceFromSourcesCommand.class, "mp", msp );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Import>Import Demo Sections", ABBAImportDemoSlicesCommand.class, "mp", msp );


        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"View>Display Mode>Positioning Mode",0, () -> setDisplayMode(POSITIONING_MODE_INT));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"View>Display Mode>Review Mode",0, () -> setDisplayMode(REVIEW_MODE_INT));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"View>Display Mode>Positioning - Change Overlap Mode [O]",0, this::toggleOverlap);

        // Cards commands
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"View>Cards>Expand Card Panel",0, () -> bdvh.getSplitPanel().setCollapsed(false));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"View>Cards>Collapse Card Panel",0, () -> bdvh.getSplitPanel().setCollapsed(true));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"View>Cards>Add Resources Monitor",0, this::addResourcesMonitor);

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"View>Navigate>Next Slice [Right]",0, this::navigateNextSlice);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"View>Navigate>Previous Slice [Left]",0, this::navigatePreviousSlice);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"View>Navigate>Center On Current Slice [C]",0, this::navigateCurrentSlice);
        BdvScijavaHelper.addSeparator(bdvh, "View");
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "View>Log", ABBAStartLogCommand.class, "mp", msp );


        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Slices>Select All Slices [Ctrl+A]",0,() -> msp.getSlices().forEach(SliceSources::select));
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Slices>Deselect All Slices [Ctrl+Shift+A]",0,() -> msp.getSlices().forEach(SliceSources::deSelect));
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(),"Slices>Select Slices", SetSlicesSelectedCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(),"Slices>Deselect Slices", SetSlicesDeselectedCommand.class, "mp", msp);
        BdvScijavaHelper.addSeparator(bdvh,"Slices");
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Slices>Remove Slices",0,() ->
                msp.getSlices()
                        .stream()
                        .filter(SliceSources::isSelected)
                        .forEach(slice -> new DeleteSliceAction(msp, slice).runRequest())
        );
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(),"Slices>Re-index Slices channels", ReindexSlicesCommand.class, "mp", msp);
        BdvScijavaHelper.addSeparator(bdvh,"Slices");
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Slices>Set Slices Display Range", SetSlicesDisplayRangeCommand.class, "mp", msp );
        BdvScijavaHelper.addSeparator(bdvh,"Slices");

        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh,"Slices>Distribute Slices [D]",0,() -> {
            if (this.mode == POSITIONING_MODE_INT) msp.equalSpacingSelectedSlices();
        });

        DeepSliceHelper.addJavaAtlases();

        if (DeepSliceHelper.isDeepSliceMouseCompatible(msp.getReslicedAtlas().ba.getName())) {

            logger.debug("Installing DeepSlice Command for Mouse");
            BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Edit>Configuration>Set DeepSlice Env path", DeepSlicePrefsSet.class);

            BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>DeepSlice>DeepSlice Registration (Web)", RegisterSlicesDeepSliceWebCommand.class,  "mp", msp, "model", "mouse");
            BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>DeepSlice>DeepSlice Registration (Local)", RegisterSlicesDeepSliceLocalCommand.class,  "mp", msp, "model", "mouse");

        }

        if (DeepSliceHelper.isDeepSliceRatCompatible(msp.getReslicedAtlas().ba.getName())) {

            logger.debug("Installing DeepSlice Command for Rat");
            BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Edit>Configuration>DeepSlice setup...", DeepSlicePrefsSet.class, 0);

            BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>DeepSlice>DeepSlice Registration (Web)", RegisterSlicesDeepSliceWebCommand.class, "mp", msp, "model", "rat");
            BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>DeepSlice>DeepSlice Registration (Local)", RegisterSlicesDeepSliceLocalCommand.class, "mp", msp, "model", "rat");

        }

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>Affine>Rotate", RotateSlicesCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>Affine>Interactive Transform", SliceAffineTransformCommand.class, "mp", msp);

        logger.debug("Installing java registration plugins ui");

        // Adds registration plugin commands : discovered via scijava plugin autodiscovery mechanism
        installRegistrationPluginUI(hierarchyLevelsSkipped);

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Edit>(Experimental)>Raster and cache deformation field", RasterSlicesDeformationCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Edit>(Experimental)>Raster slice", RasterSlicesCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Edit>(Experimental)>Copy and Apply Registration", RegisterSlicesCopyAndApplyCommand.class, "mp", msp );

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Export>Set Slices Thickness", SetSlicesThicknessCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Export>Set Slices Thickness (fill gaps)", SetSlicesThicknessMatchNeighborsCommand.class, "mp", msp);
        BdvScijavaHelper.addSeparator(bdvh,"Export");

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Export> ImageJ > Export Regions To Roi Manager", ExportRegionsToRoiManagerCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Export> ImageJ > Export Regions To File", ExportRegionsToRoisetFileCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Export> QuPath > Export Registrations To QuPath Project", ExportRegistrationToQuPathCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Edit>(Experimental)> Export > Bdv > Export Registered Slices to BDV Json Dataset", ExportSlicesToBDVJsonDatasetCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Edit>(Experimental)> Export > Bdv > Export Resampled Slices as BDV Source", ExportResampledSlicesToBDVSourceCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Export> BigDataViewer > Export Registered Slices to BDV", ExportSlicesToBDVCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Export> ImageJ > Export Registered Slices to ImageJ", ExportSlicesToImageJCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Export> ImageJ > Export Original Slices to ImageJ", ExportSlicesOriginalDataToImageJCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Export> ImageJ > Export Atlas Coordinates of Original Slices to ImageJ", ExportDeformationFieldToImageJCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Export> ImageJ > Export Atlas to ImageJ", ExportAtlasToImageJCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Edit>(Experimental)>Export Registered Slices to Quick NII Dataset", ExportSlicesToQuickNIIDatasetCommand.class, "mp", msp);

        BdvScijavaHelper.addSeparator(bdvh,"Register");
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>Edit Last Registration", RegisterSlicesEditLastCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>Remove Last Registration", RegisterSlicesRemoveLastCommand.class, "mp", msp );

        final BiConsumer<String,String> guiErrorLogger = this::blockingErrorMessageForUsers;

        msp.subscribeToErrorMessages(guiErrorLogger);
        addToCleanUpHook(() -> msp.unSubscribeFromErrorMessages(guiErrorLogger));

        final BiConsumer<String,String> warnLogger = this::warningMessageForUser;

        msp.subscribeToWarningMessages(warnLogger);
        addToCleanUpHook(() -> msp.unSubscribeFromWarningMessages(warnLogger));

        final BiConsumer<String,String> infoLogger = this::infoMessageForUser;

        msp.subscribeToInfoMessages(infoLogger);
        addToCleanUpHook(() -> msp.unSubscribeFromInfoMessages(infoLogger));

        // Update check
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Help>Check for updates", ABBACheckForUpdateCommand.class);

        // Help commands
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Help>Ask for help in the forum (web)", ABBAForumHelpCommand.class);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Help>Go to documentation (web)", ABBADocumentationCommand.class);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Help>Give your feedback (web)", ABBAUserFeedbackCommand.class);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Help>About DeepSlice (web)", DeepSliceDocumentationCommand.class);

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Cite>How to cite ABBA (web)", ABBACiteInfoCommand.class);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Cite>Generate methods prompt", ABBAGenerateMethodsPrompt.class, "mp", msp);
        BdvScijavaHelper.addSeparator(bdvh,"Cite");
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Cite>About DeepSlice (web)", DeepSliceDocumentationCommand.class);
        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh, "Cite>About current atlas (web)", 0, () ->
        {
            try {
                (msp.getContext().getService(PlatformService.class)).open(new URL(msp.getAtlas().getURL()));
            } catch (IOException e) {
                msp.errorMessageForUser.accept("Could not open atlas URL", "msp.getAtlas().getURL() | Error: "+e.getMessage());
            }
        }
        );

    }

    private void installRegistrationPluginUI(int hierarchyLevelsSkipped) {

        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>Affine>Elastix Registration (Affine)", RegisterSlicesElastixAffineCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>Spline>BigWarp Registration", RegisterSlicesBigWarpCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Register>Spline>Elastix Registration (Spline)", RegisterSlicesElastixSplineCommand.class, "mp", msp);
        BdvScijavaHelper.addSeparator(bdvh,"Slices");
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Slices>Mirror Slices", MirrorDoCommand.class, "mp", msp);
        BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), "Slices>Un-Mirror Slices", MirrorUndoCommand.class, "mp", msp);

        if (!msp.getExternalRegistrationPluginsUI().isEmpty()) {
            BdvScijavaHelper.addSeparator(bdvh,"Register");
        }

        logger.debug("Installing external registration plugins ui");
        msp.getExternalRegistrationPluginsUI().keySet().forEach(externalRegistrationType ->
            msp.getExternalRegistrationPluginsUI().get(externalRegistrationType).forEach( ui -> {
                        logger.info("External registration plugin "+ui+" added in bdv user interface");
                        BdvScijavaHelper.addActionToBdvHandleMenu(bdvh, "Register>"+ui, 0, () ->
                            (msp.getContext().getService(CommandService.class)).run(ui, true, "mp", msp)
                        );
                    }
            )
        );


        //logger.debug("Adding interactive transform");
        //BdvScijavaHelper.addActionToBdvHandleMenu(bdvh, "Align>ABBA - Interactive Transform", 0, () ->
        //    (msp.getContext().getService(CommandService.class)).run(SliceAffineTransformCommand.class, true, "mp", msp)
        //);

    }

    private void installBigDataViewerCards() {
        BdvHandleHelper.addCard(bdvh, NAME_CARD_ATLAS_INFORMATION, new AtlasInfoPanel(msp).getPanel(), true);

        //TODO, FIX NULL
        BdvHandleHelper.addCard(bdvh, NAME_CARD_ATLAS_DISPLAY, ScijavaSwingUI.getPanel(msp.getContext(), AtlasAdjustDisplayCommand.class, "view", this), true);

        logger.debug("Adding table view");
        addTableView();

        BdvHandleHelper.addCard(bdvh, NAME_CARD_DISPLAY_NAVIGATION, new NavigationPanel(this).getPanel(), true);

        BdvHandleHelper.addCard(bdvh, NAME_CARD_EDIT_SLICES, new EditPanel(msp).getPanel(), true);

        try {
            Module module = ScijavaSwingUI.createModule(msp.getContext(), AtlasSlicingAdjusterCommand.class, "reslicedAtlas", msp.getReslicedAtlas());
            SwingInputHarvester swingInputHarvester = new SwingInputHarvester();
            msp.getContext().inject(swingInputHarvester);
            InputPanel<JPanel, JPanel> inputPanel = new SwingInputPanel();
            swingInputHarvester.buildPanel(inputPanel, module);
            BdvHandleHelper.addCard(bdvh, NAME_CARD_ATLAS_SLICING, inputPanel.getComponent(), true);
            msp.getReslicedAtlas().addListener(() -> {
                inputPanel.getWidget("rotateX").refreshWidget();
                inputPanel.getWidget("rotateY").refreshWidget();
            });
        } catch (Exception e) {
            msp.errorMessageForUser.accept("GUI Initialisation error", e.getMessage());
        }

        BdvHandleHelper.addCard(bdvh, NAME_CARD_DEFINE_ROI,
                ScijavaSwingUI.getPanel(msp.getContext(), SliceDefineROICommand.class, "mp", msp, "view", this),
                false);

        addToCleanUpHook(() ->
            SwingUtilities.invokeLater(() -> {
            if (bdvh.getCardPanel()!=null) {
                    bdvh.getCardPanel().removeCard(NAME_CARD_ATLAS_INFORMATION);
                    bdvh.getCardPanel().removeCard(NAME_CARD_ATLAS_DISPLAY);
                    bdvh.getCardPanel().removeCard(NAME_CARD_DISPLAY_NAVIGATION);
                    bdvh.getCardPanel().removeCard(NAME_CARD_EDIT_SLICES);
                    bdvh.getCardPanel().removeCard(NAME_CARD_ATLAS_SLICING);
                    bdvh.getCardPanel().removeCard(NAME_CARD_DEFINE_ROI);
                    bdvh.getCardPanel().removeCard(NAME_CARD_CURRENT_SLICE_INFO);

            }
            })
        );
    }

    private void displayAtlas() {
        List<SourceAndConverter<?>> sacsToAppend = new ArrayList<>();
        for (int i = 0; i < msp.getAtlas().getMap().getStructuralImages().size(); i++) {
            sacsToAppend.add(msp.getReslicedAtlas().extendedSlicedSources[i]);
            sacsToAppend.add(msp.getReslicedAtlas().nonExtendedSlicedSources[i]);
        }
        SourceAndConverterServices.getBdvDisplayService()
                .show(bdvh, sacsToAppend.toArray(new SourceAndConverter[0]));
        bdvh.getViewerPanel().state().addSourcesToGroup(sacsToAppend, bdvh.getViewerPanel().state().getGroups().get(0));

    }

    public void addToCleanUpHook(Runnable runnable) {
        extraCleanUp.add(runnable);
    }

    private void addRoiOverlaySource() {

        logger.debug("Adding user ROI source");

        final float valTrue = 0.0f;
        final float valFalse = 1.0f;

        BiConsumer<RealLocalizable, FloatType> fun = (loc, val) -> {
            double px = loc.getFloatPosition(0);
            double py = loc.getFloatPosition(1);

            if (py<-sY/1.9) {val.set(valTrue); return;}
            if (py>sY/1.9) {val.set(valTrue); return;}

            if (mode == POSITIONING_MODE_INT) {
                final double v = Math.IEEEremainder(px + sX * 0.5, sX);
                if (v < roiPX) {val.set(valFalse); return;}
                if (v > roiPX+roiSX) {val.set(valFalse); return;}
                if (py<roiPY) {val.set(valFalse); return;}
                if (py>roiPY+roiSY) {val.set(valFalse); return;}
                val.set(valTrue);
            }
            if (mode == REVIEW_MODE_INT) {
                if (loc.getFloatPosition(0) < roiPX) {val.set(valFalse); return;}
                if (loc.getFloatPosition(0) > roiPX+roiSX) {val.set(valFalse); return;}
                if (loc.getFloatPosition(1) < roiPY) {val.set(valFalse); return;}
                if (loc.getFloatPosition(1) > roiPY+roiSY) {val.set(valFalse); return;}
                val.set(valTrue);
            }
        };

        FunctionRealRandomAccessible<FloatType> roiOverlay = new FunctionRealRandomAccessible<>(3, fun, FloatType::new);

        long boxSizeUm = 1_000_000; // 1 meter
        RealRandomAccessibleIntervalSource<FloatType> roiSource = new RealRandomAccessibleIntervalSource<>(roiOverlay,
                new FinalInterval(new long[]{-boxSizeUm, -boxSizeUm, -boxSizeUm}, new long[]{boxSizeUm, boxSizeUm, boxSizeUm}),
                new FloatType(), new AffineTransform3D(), "ROI");

        SourceAndConverter<FloatType> roiSAC = SourceAndConverterHelper.createSourceAndConverter(roiSource);
        SourceAndConverterServices.getSourceAndConverterService().register(roiSAC);

        IAlphaSource alpha = new WrappedIAlphaSource(roiSource);

        SourceAndConverter<FloatType> alpha_sac = new SourceAndConverter<>(alpha, new AlphaConverter());

        SourceAndConverterServices.getSourceAndConverterService().setMetadata(roiSAC, ALPHA_SOURCE_KEY, alpha_sac);

        BdvStackSource<?> bss = BdvFunctions.show(roiSAC, BdvOptions.options().addTo(bdvh));

        bdvh.getViewerPanel().state().addSourceToGroup(bss.getSources().get(0), bdvh.getViewerPanel().state().getGroups().get(2));

        bss.setDisplayRangeBounds(0,4);
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
        frame.setIconImage((new ImageIcon(MultiSlicePositioner.class.getResource("/graphics/ABBAStart.png"))).getImage());
    }

    private void addTableView() {
        tableView = new TableView(this);
        msp.addSliceListener(tableView);
        BdvHandleHelper.addCard(bdvh, NAME_CARD_SLICES_DISPLAY, tableView.getPanel(), true);
        addToCleanUpHook(() -> {
            tableView.cleanup();
            if (msp!=null) { // Because cleanup is called 2 times. TODO fix double call
                msp.removeSliceListener(tableView);
            }
            // TODO: remove card
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
                    //Atlas atlas = msp.getAtlas();
                    msp.close();
                    ctx.getService(ObjectService.class).removeObject(msp);
                    //ctx.getService(ObjectService.class).removeObject(atlas);
                    /*
                    * This line is buggy with pyimagej:
                    *
                        Exception in thread "AWT-EventQueue-0" java.lang.ClassCastException: com.sun.proxy.$Proxy16 cannot be cast to org.scijava.Prioritized
                            at org.scijava.Prioritized.compareTo(Prioritized.java:39)
                            at org.jpype.proxy.JPypeProxy.hostInvoke(Native Method)
                            at org.jpype.proxy.JPypeProxy.invoke(Unknown Source)
                            at com.sun.proxy.$Proxy16.equals(Unknown Source)
                            at java.util.ArrayList.remove(ArrayList.java:534)
                            at org.scijava.object.ObjectIndex.removeFromList(ObjectIndex.java:337)
                            at org.scijava.object.ObjectIndex.remove(ObjectIndex.java:323)
                            at org.scijava.object.ObjectIndex.remove(ObjectIndex.java:282)
                            at org.scijava.object.ObjectIndex.remove(ObjectIndex.java:196)
                            at org.scijava.object.ObjectService.removeObject(ObjectService.java:97)
                            at ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView.lambda$addCleanAllHook$47(BdvMultislicePositionerView.java:653)

                    * */

                    // Remove all sources - TODO : make this more specific!
                    SourceAndConverterServices
                            .getSourceAndConverterService()
                            .remove(
                                    SourceAndConverterServices
                                            .getSourceAndConverterService().getSourceAndConverters().toArray(new SourceAndConverter[0])
                            );

                    //System.gc();
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

        boolean tmpRecord = Recorder.record;
        // Avoid meesing up with the recorder during initialisation / a bit sketchy
        try {

            Recorder.record = false;

            // Final variable initialization
            this.bdvh = bdvh;
            this.vp = bdvh.getViewerPanel();
            this.msp = msp;
            this.sX = msp.sX;
            this.sY = msp.sY;
            roiChanged(); // initialize roi

            moa = new MessageOverlayAnimator(8000, 0.0, 0.1);
            SwingUtilities.invokeLater(() -> bdvh.getViewerPanel().addOverlayAnimator(moa));

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

            overlapMode = OVERLAP_BELOW_ATLAS_STAIRS;
            updateOverlapMode();

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
            bdvh.getSplitPanel().setDividerSize(10);
            bdvh.getSplitPanel().setDividerLocation(bdvh.getSplitPanel().getWidth()-600);

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
        } finally {
            Recorder.record = tmpRecord; // Restore recorder state even if things are failing
        }
    }

    /**
     * Saves the current view on top of the state file
     */
    public void loadState(Object... kv) {
        try {
            msp.addTask();
            bdvh.getViewerPanel().requestRepaint();
            if ((msp.getSlices()!=null)&&(!msp.getSlices().isEmpty())) {
                msp.errorMessageForUser.accept("Slices are already present!", "You can't open a state file if slices are already present in ABBA instance.");
                 return;
            }
            CommandModule cm;
            try {
                blockBdvRepaint();
                if ((kv!=null)&&(kv.length==2)) {
                    cm = msp.getContext().getService(CommandService.class)
                            .run(ABBAStateLoadCommand.class, true, "mp", msp, kv[0], kv[1]).get();
                } else {
                    cm = msp.getContext().getService(CommandService.class)
                            .run(ABBAStateLoadCommand.class, true, "mp", msp).get();
                }
                if ((cm == null) || (cm.getOutput("success") == null)) return;
                boolean success = (boolean) cm.getOutput("success");
                if (!success) return;
            } finally {
                resumeBdvRepaint();
            }
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
                    this.infoMessageForUser("Load state", "Cannot restore display slices!"); // TODO : could be improved
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
        } finally {
            msp.removeTask();
        }
    }

    private boolean updateBdv = true;

    private void blockBdvRepaint() {
        updateBdv = false;
    }

    private void resumeBdvRepaint() {
        updateBdv = true;
        bdvh.getViewerPanel().getDisplay().repaint();
        bdvh.getViewerPanel().requestRepaint();
    }

    protected boolean bdvRepaintEnabled() {
        return updateBdv;
    }

    public void saveState() {
        saveState(null);
    }
    public void saveState(File stateFileIn) {

        //BdvScijavaHelper.addCommandToBdvHandleMenu(bdvh, msp.getContext(), ABBAStateSaveCommand.class, hierarchyLevelsSkipped,"mp", msp );
        try {
            if (stateFileIn==null) {
                CommandModule cm = msp.getContext().getService(CommandService.class)
                        .run(ABBAStateSaveCommand.class, true, "mp", msp).get();

                msp.waitForTasks();

                boolean success = (Boolean) (cm.getOutput("success"));

                if (!success) {
                    blockingErrorMessageForUsers("State not saved!", "Something went wrong");
                    return;
                }

                stateFileIn = (File) cm.getInput("state_file");
            } else {
                String extension = FilenameUtils.getExtension(stateFileIn.getAbsolutePath());
                if (extension.trim().isEmpty()) {
                    logger.debug("Adding abba extension to state file");
                    stateFileIn = new File(stateFileIn.getAbsolutePath()+".abba");
                }

                if (stateFileIn.exists()) {
                    blockingErrorMessageForUsers("Can't save the state","Error, the file "+stateFileIn+" already exists.");
                    return;
                } else {
                    boolean success = msp.saveState(stateFileIn, true);
                    if (!success) {
                        blockingErrorMessageForUsers("State not saved!", "Something went wrong");
                        return;
                    }
                }
            }

            String fileNoExt = FilenameUtils.removeExtension(stateFileIn.getAbsolutePath());
            File viewFile = new File(fileNoExt+"_bdv_view.json");

            // Ok, let's save the view File

            List<SliceGuiState.State> states = new ArrayList<>(Collections.nCopies(msp.getSlices().size(), null));

            guiState.forEachSlice(sliceState ->
                    states.set(sliceState.slice.getIndex(), new SliceGuiState.State(sliceState))
            );
            ViewState vs = new ViewState();
            vs.slicesStates = states;
            vs.showInfo = showSliceInfo;
            vs.bdvViewMode = mode;
            vs.bdvSliceViewMode = sliceDisplayMode;
            vs.overlapMode = overlapMode;
            vs.bdvView = bdvh.getViewerPanel().state().getViewerTransform().getRowPackedCopy();
            vs.atlasSlicingStep = (int) msp.getReslicedAtlas().getStep();
            vs.iCurrentSlice = iCurrentSlice;
            notifyCurrentSliceListeners();

            FileWriter writer = new FileWriter(viewFile.getAbsolutePath());
            new GsonBuilder().setPrettyPrinting().create().toJson(vs, writer);
            writer.flush();
            writer.close();

            if ((stateFileIn.exists())&& (Files.size(Paths.get(stateFileIn.getAbsolutePath()))>0)) {
                infoMessageForUser("State saved", "Path:" + stateFileIn.getAbsolutePath());
            } else {
                blockingErrorMessageForUsers("State not saved!", "Something went wrong");
            }

        } catch (Exception e) {
            blockingErrorMessageForUsers("State not saved!", e.getMessage());
            e.printStackTrace();
        }
    }

    Thread modificationMonitorThread;
    boolean stopMonitoring = false;

    private void modificationMonitor() {
        boolean previousStateModification = msp.isModifiedSinceLastSave();
        boolean previousTimeReady = msp.getNumberOfTasks()==0;
        while ((!stopMonitoring)&&(bdvh!=null)) {
            if (bdvRepaintEnabled()) {
                MultiSlicePositioner current_msp = this.msp;
                if (current_msp != null) {
                    if (previousStateModification != current_msp.isModifiedSinceLastSave()) {
                        previousStateModification = current_msp.isModifiedSinceLastSave();
                        BdvHandleHelper.setWindowTitle(bdvh, getViewName());
                    }
                    if (current_msp.getNumberOfTasks() > 0) {
                        if (bdvh != null) {
                            bdvh.getViewerPanel().getDisplay().repaint(); // OK
                            previousTimeReady = false;
                        }
                    } else {
                        if (!previousTimeReady) {
                            if (bdvh != null) {
                                bdvh.getViewerPanel().getDisplay().repaint(); // OK
                                previousTimeReady = true;
                            }
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
            return ABBABdvViewPrefs.title_prefix+name+"* (modified)"+ABBABdvViewPrefs.title_suffix;
        } else {
            return ABBABdvViewPrefs.title_prefix+name+ABBABdvViewPrefs.title_suffix;
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
        logger.debug("Initializing "+slice.getName());
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
        if ((overlapMode == OVERLAP_ON_ATLAS)&&(sliceGuiState!=null)) {
            sliceGuiState.setYShift(0);
        } else if ((overlapMode == OVERLAP_BELOW_ATLAS)&&(sliceGuiState!=null)) {
            sliceGuiState.setYShift(1);
        } else if (overlapMode == OVERLAP_BELOW_ATLAS_STAIRS) {
            // N^2 algo! Take care TODO improve
            double lastPositionAlongX = -Double.MAX_VALUE;
            double stairIndex = 0;
            List<SliceSources> slices = msp.getSlices();
            double globalOffsY = 0.35;
            synchronized (this) { // synchronize after getting the slices to avoid thread deadlock
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
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(globalOffsY+overlapFactorY));
                                } else {
                                    stairIndex += overlapFactorY;
                                    final double finalStairIndex = stairIndex;
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(globalOffsY+overlapFactorY + finalStairIndex));
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
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(globalOffsY+overlapFactorY));
                                } else {
                                    stairIndex += overlapFactorY;
                                    final double finalStairIndex = stairIndex;
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(globalOffsY+overlapFactorY + finalStairIndex));
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
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(globalOffsY+overlapFactorY));
                                } else {
                                    stairIndex += overlapFactorY;
                                    final double finalStairIndex = stairIndex;
                                    guiState.runSlice(slice, guiState -> guiState.setYShift(globalOffsY+overlapFactorY + finalStairIndex));
                                }
                            }
                        }
                    }
                }
            }
        }
        if (bdvRepaintEnabled()) {bdvh.getViewerPanel().requestRepaint();} // OK
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
        logger.debug(slice.getName()+ " created");
        iniSlice(slice);
    }

    @Override
    public void sliceDeleted(SliceSources slice) {
        logger.debug(slice.getName()+ " deleted");
        guiState.deleted(slice);
    }

    // Error : sometimes this does not return
    @Override
    public void sliceZPositionChanged(SliceSources slice) { // should not be sync : slices lock is already locked
        logger.debug(slice.getName() + " z position changed");
        guiState.runSlice(slice, guiState -> {
            guiState.slicePositionChanged();
            updateSliceDisplayedPosition(guiState); // fail!! TODO FIX
        });
        //bdvh.getViewerPanel().requestRepaint();
    }

    @Override
    public void sliceSelected(SliceSources slice) {
        if (bdvRepaintEnabled()) {
            logger.debug(slice.getName() + " selected");
            bdvh.getViewerPanel().getDisplay().repaint();
        }
    }

    @Override
    public void sliceDeselected(SliceSources slice) {
        if (bdvRepaintEnabled()) {
            logger.debug(slice.getName() + " deselected");
            bdvh.getViewerPanel().getDisplay().repaint();
        }
    }

    @Override
    public void sliceSourcesChanged(SliceSources slice) {
        logger.debug(slice.getName() + " slices changed");
        guiState.runSlice(slice, SliceGuiState::sourcesChanged);
    }

    @Override
    public void slicePretransformChanged(SliceSources sliceSources) {
        if (bdvRepaintEnabled()) {
            bdvh.getViewerPanel().requestRepaint();
        }
    }

    @Override
    public void sliceKeyOn(SliceSources slice) {
        if (bdvRepaintEnabled()) {
            bdvh.getViewerPanel().getDisplay().repaint();
        }
    }

    @Override
    public void sliceKeyOff(SliceSources slice) {
        if (bdvRepaintEnabled()) {
            bdvh.getViewerPanel().getDisplay().repaint();
        }
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
        if (bdvRepaintEnabled()) {
            bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
        }
    }

    @Override
    public void actionStarted(SliceSources slice, CancelableAction action) {
        if (bdvRepaintEnabled()) {
            bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
        }
    }

    @Override
    public void actionFinished(SliceSources slice, CancelableAction action, boolean result) {
        if (bdvRepaintEnabled()) {
            bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
        }
    }

    @Override
    public void actionCancelEnqueue(SliceSources slice, CancelableAction action) {
        if (bdvRepaintEnabled()) {
            bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
        }
    }

    @Override
    public void actionCancelStarted(SliceSources slice, CancelableAction action) {
        if (bdvRepaintEnabled()) {
            bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update
        }
    }

    @Override
    public void actionCancelFinished(SliceSources slice, CancelableAction action, boolean result) {
        if (bdvRepaintEnabled()) {
            bdvh.getViewerPanel().getDisplay().repaint(); // Overlay Update // OK
        }
    }

    @Override
    public void converterChanged(SliceSources slice) {
        guiState.forEachSlice(SliceGuiState::updateDisplaySettings);
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
        if (overlapMode==OVERLAP_BELOW_ATLAS_STAIRS) {
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
                BdvHandleHelper.addCard(bdvh, "Resources Monitor", rm, false);
                //bdvh.getCardPanel().adCard("Resources Monitor", rm, false);
            } catch (Exception e) {
                rm = null;
                logger.debug("Could not start Resources Monitor");
            }
        } else {
            if (bdvh == null) {
                blockingErrorMessageForUsers("Issue in GUI generation", "No Graphical User Interface.");
            }
            if (rm!=null) {
                warningMessageForUser("Warning", "Resource Monitor is already present");
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
        notifyCurrentSliceListeners();
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
            if (overlapMode==OVERLAP_BELOW_ATLAS_STAIRS) updateSliceDisplayedPosition(null);
            centerBdvViewOn(sortedSlices.get(iCurrentSlice), true, previousSlice);
        }
        notifyCurrentSliceListeners();
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
            if (overlapMode==OVERLAP_BELOW_ATLAS_STAIRS) updateSliceDisplayedPosition(null);
            centerBdvViewOn(sortedSlices.get(iCurrentSlice), true, previousSlice);
        }
        notifyCurrentSliceListeners();
    }

    public void navigateSlice(SliceSources slice) {
        int previousSliceIndex = iCurrentSlice;
        List<SliceSources> sortedSlices = msp.getSlices();
        iCurrentSlice = sortedSlices.indexOf(slice);

        if (iCurrentSlice < 0) {
            return;
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
            if (overlapMode==OVERLAP_BELOW_ATLAS_STAIRS) updateSliceDisplayedPosition(null);
            centerBdvViewOn(sortedSlices.get(iCurrentSlice), true, previousSlice);
        }
        notifyCurrentSliceListeners();
    }

    public void centerBdvViewOn(SliceSources slice) {
        centerBdvViewOn(slice, false, null);
    }

    // Center bdv on a slice
    public void centerBdvViewOn(SliceSources current_slice, boolean maintainoffset, SliceSources previous_slice) {

        RealPoint offset = new RealPoint(3);

        RealPoint centerScreen = getCurrentBdvCenter();

        if ((maintainoffset)&&(previous_slice!=null)) {

            RealPoint oldCenter = getDisplayedCenter(previous_slice);

            offset.setPosition(- oldCenter.getDoublePosition(0) + centerScreen.getDoublePosition(0), 0);

            offset.setPosition((- oldCenter.getDoublePosition(1) + centerScreen.getDoublePosition(1)), 1);

            offset.setPosition(0, 2);

            if (mode == REVIEW_MODE_INT) {
                } else {
                offset.setPosition(0, 1);
            }

            if (Math.abs(offset.getDoublePosition(0))>msp.sX*1.5) {maintainoffset = false;}
            if (Math.abs(offset.getDoublePosition(1))>msp.sY*1.5) {maintainoffset = false;}

        } else {
            maintainoffset = false;
        }

        RealPoint centerSlice = getDisplayedCenter(current_slice);

        if(maintainoffset) {

            centerSlice.move(offset);

            if (mode == REVIEW_MODE_INT) {
            } else {
                centerSlice.setPosition(centerScreen.getDoublePosition(1), 1);
            }
        }

        AffineTransform3D at3d = BdvHandleHelper.getViewerTransformWithNewCenter(bdvh, centerSlice.positionAsDoubleArray());

        bdvh.getViewerPanel().state().setViewerTransform(at3d);

        if (bdvRepaintEnabled()) {
            bdvh.getViewerPanel().requestRepaint();
        }

    }

    RealPoint getCurrentBdvCenter() {
        RealPoint centerBdv = new RealPoint(3);

        double px = bdvh.getViewerPanel().getWidth() / 2.0;
        double py = bdvh.getViewerPanel().getHeight() / 2.0;
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
                notifyCurrentSliceListeners();
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
            g.setFont(ABBABdvViewPrefs.slice_info_font);
            g.setColor(ABBABdvViewPrefs.slice_info_color);
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

    public void setSliceChannelVisibility(SliceSources slice, int iChannel, boolean visible) {
        guiState.runSlice(slice, sliceGuiState -> sliceGuiState.setChannelVisibility(iChannel, visible));
        tableView.sliceDisplaySettingsChanged(slice);
    }

    public void setSelectedSlicesVisibility(int iChannel, boolean visible) {
        guiState.forEachSelectedSlice(sliceGuiState -> sliceGuiState.setChannelVisibility(iChannel, visible));
        msp.getSelectedSlices().forEach(slice -> tableView.sliceDisplaySettingsChanged(slice));
    }

    public void setSelectedSlicesVisibility(Boolean visible) {
        guiState.forEachSelectedSlice(sliceGuiState -> sliceGuiState.setSliceVisibility(visible));
        msp.getSelectedSlices().forEach(slice -> tableView.sliceDisplaySettingsChanged(slice));
    }

    double overlapFactorX = 1.0;
    double overlapFactorY = 1.0;

    public void setOverlapFactorX(int value) {
        double newValue = 0.1 + 3.0 - ((value/100.0) * 3.0);
        if (newValue!=overlapFactorX) {
            overlapFactorX = newValue;
            if (overlapMode == OVERLAP_BELOW_ATLAS_STAIRS) {
                updateOverlapMode();
            }
        }
    }

    public void setOverlapFactorY(int value) {
        double newValue = 0.1 + 3.0 - ((value/100.0) * 3.0);
        if (newValue!=overlapFactorY) {
            overlapFactorY = newValue;
            if (overlapMode == OVERLAP_BELOW_ATLAS_STAIRS) {
                updateOverlapMode();
            }
        }
    }

    class InnerOverlay extends BdvOverlay {
        int drawCounter = 0;
        @Override
        protected void draw(Graphics2D g) {

            // Enable antialiasing for this overlay
            enableAntialiasing(g);

            drawCounter++;
            drawCounter = drawCounter%21;
            // Gets a copy of the slices to avoid concurrent exception
            List<SliceSources> slicesCopy = msp.getSlices();

            // Gets current bdv view position
            AffineTransform3D bdvAt3D = new AffineTransform3D();
            bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);

            drawDragAndDropRectangle(g, bdvAt3D); // honestly it's not used!

            guiState.forEachSlice(sliceGuiState -> {
                sliceGuiState.drawGraphicalHandles(g);
                List<CancelableAction> actionsRaw = msp.getActionsFromSlice(sliceGuiState.slice);
                if (actionsRaw==null) {
                    System.err.println("Error : actions null for slice "+sliceGuiState.slice);
                } else {
                    List<CancelableAction> actions = new ArrayList<>(actionsRaw);
                    Integer[] pos = sliceGuiState.getSliceHandleCoords();
                    if (actions != null) {
                        int iPos = 0;
                        int totalNumberOfRegistration = 0;
                        for (CancelableAction action : actions) {
                            if (!action.isHidden()) {
                                if (action instanceof RegisterSliceAction) {
                                    if (action.getSliceSources().getActionState(action).equals("(done)"))
                                        totalNumberOfRegistration++;
                                }
                            }
                        }
                        int countNumberOfRegistration = 0;
                        for (CancelableAction action : actions) {
                            if (!action.isHidden()) {
                                iPos++;
                                if (action instanceof RegisterSliceAction) {
                                    // Is it viewed or not ? How to know this
                                    countNumberOfRegistration++;
                                    if (totalNumberOfRegistration - registrationStepBack < countNumberOfRegistration) {
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
                }
            });

            drawCurrentSliceOverlay(g, slicesCopy);

            if (mode == POSITIONING_MODE_INT) drawSetOfSliceControls(g, bdvAt3D, slicesCopy);

            if (selectionLayer != null) selectionLayer.draw(g);

            if (showAtlasPosition) drawAtlasPosition(g);

            if (showSliceInfo) drawSliceInfo(g, slicesCopy);

            int w = bdvh.getViewerPanel().getWidth();
            int h = bdvh.getViewerPanel().getHeight();

            g.setColor(ABBABdvViewPrefs.task_counter_color);
            g.setStroke(ABBABdvViewPrefs.task_counter_stroke);

            if (msp.getNumberOfTasks() > 0) {
                String text = "" + msp.getNumberOfTasks();
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent(); // Use ascent for baseline alignment

                // Center of the arc
                int centerX = w - 54 + 25;
                int centerY = h - 54 + 25;

                // Calculate x and y for centered text
                int x = centerX - textWidth / 2;
                int y = centerY + textHeight / 2;

                g.drawString(text, x, y);

                if (drawCounter <= 10) {
                    g.drawArc(w - 54, h - 54, 50, 50, 0, drawCounter * 36);
                } else {
                    g.drawArc(w - 54, h - 54, 50, 50, (drawCounter - 10) * 36, 360 - ((drawCounter - 10) * 36));
                }
            }

            if (msp.isModifiedSinceLastSave()) {
                g.drawString("Modified since last save!", 5, h-15);
            } else {
                g.drawString("No modification", 5, h-15);
            }

        }
    }

    private void enableAntialiasing(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB); // Better for LCD screens
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    Object getSourceValueAt(SourceAndConverter<?> sac, RealPoint pt) {
        RealRandomAccessible<?> rra_ible = sac.getSpimSource().getInterpolatedSource(0, 0, Interpolation.NEARESTNEIGHBOR);
        if (rra_ible != null) {
            AffineTransform3D sourceTransform = new AffineTransform3D();
            sac.getSpimSource().getSourceTransform(0, 0, sourceTransform);
            RealRandomAccess<?> rra = rra_ible.realRandomAccess();
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

            SourceAndConverter<?> label = reslicedAtlas.extendedSlicedSources[reslicedAtlas.getLabelSourceIndex()]; // By convention the label image is the last one (OK)
            labelValue = ((IntegerType<?>) getSourceValueAt(label, globalMouseCoordinates)).getInteger();
            SourceAndConverter<?> lrSource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.getLeftRightSourceIndex()]; // By convention the left right indicator image is the next to last one
            leftRight = ((IntegerType<?>) getSourceValueAt(lrSource, globalMouseCoordinates)).getInteger();

            SourceAndConverter<FloatType> xSource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.getCoordinateSourceIndex(0)]; // 0 = X
            SourceAndConverter<FloatType> ySource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.getCoordinateSourceIndex(1)]; // By convention the left right indicator image is the next to last one
            SourceAndConverter<FloatType> zSource = reslicedAtlas.extendedSlicedSources[reslicedAtlas.getCoordinateSourceIndex(2)]; // By convention the left right indicator image is the next to last one

            coords[0] = ((FloatType) getSourceValueAt(xSource, globalMouseCoordinates)).get();
            coords[1] = ((FloatType) getSourceValueAt(ySource, globalMouseCoordinates)).get();
            coords[2] = ((FloatType) getSourceValueAt(zSource, globalMouseCoordinates)).get();
        } else {
            assert mode == REVIEW_MODE_INT;
            SourceAndConverter<?> label = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.getLabelSourceIndex()]; // By convention the label image is the last one
            labelValue = ((IntegerType<?>) getSourceValueAt(label, globalMouseCoordinates)).getInteger();
            SourceAndConverter<?> lrSource = reslicedAtlas.nonExtendedSlicedSources[reslicedAtlas.getLeftRightSourceIndex()]; // By convention the left right indicator image is the next to last one
            leftRight = ((IntegerType<?>) getSourceValueAt(lrSource, globalMouseCoordinates)).getInteger();

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
                    ontologyLocation.append("<").append(node);
                }
            }
        }

        g.setFont(ABBABdvViewPrefs.mouse_atlas_coordinates_font);
        g.setColor(ABBABdvViewPrefs.mouse_atlas_coordinates_color);
        try {
            Point mouseLocation = bdvh.getViewerPanel().getMousePosition();
            if ((ontologyLocation!=null)&&(mouseLocation!=null)) {
                g.drawString(ontologyLocation.toString(),mouseLocation.x,mouseLocation.y);
            }
            if ((mouseLocation!=null)&&(!coordinates.startsWith("[0.00;0.00;0.00]"))) {
                g.drawString(coordinates, mouseLocation.x, mouseLocation.y - 20);
            }
        } catch (NullPointerException npe) {
            System.out.println("NPE CAUGHT!!!!");
        }
    }

    private void drawSetOfSliceControls(Graphics2D g, AffineTransform3D bdvAt3D, List<SliceSources> slicesCopy) {

        if (slicesCopy.stream().anyMatch(SliceSources::isSelected)) {

            List<SliceSources> sortedSelected = msp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
            RealPoint precedentPoint = null;

            g.setStroke(ABBABdvViewPrefs.line_between_selected_slices_stroke);
            for (int i = 0; i < sortedSelected.size(); i++) {
                SliceSources slice = sortedSelected.get(i);
                Integer[] coords = guiState.getSliceHandleCoords(slice);
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
                    handleLeftPoint.setPosition(sY/2.0, 1);
                    bdvAt3D.apply(handleLeftPoint, handleLeftPoint);

                    leftPosition[0] = (int) handleLeftPoint.getDoublePosition(0);
                    leftPosition[1] = (int) handleLeftPoint.getDoublePosition(1);
                }

                if (i == sortedSelected.size() - 1) {
                    RealPoint handleRightPoint = this.getDisplayedCenter(slice);
                    handleRightPoint.setPosition(sY/2.0, 1);
                    bdvAt3D.apply(handleRightPoint, handleRightPoint);

                    rightPosition[0] = (int) handleRightPoint.getDoublePosition(0);
                    rightPosition[1] = (int) handleRightPoint.getDoublePosition(1);
                }

            }

            if (sortedSelected.size() > 1) {
                ghs.forEach(GraphicalHandle::enable);
                g.setColor(ABBABdvViewPrefs.line_between_selected_slices_color);
                g.drawLine(leftPosition[0], leftPosition[1], rightPosition[0], rightPosition[1]);
            } else if (sortedSelected.size() == 1) {
                g.setColor(ABBABdvViewPrefs.line_between_selected_slices_color);
                g.drawLine(leftPosition[0], leftPosition[1], rightPosition[0], rightPosition[1]);
            } else {
                ghs.forEach(GraphicalHandle::disable);
            }
            ghs.forEach(gh -> gh.draw(g));
        }

        // Set the stroke of the copy, not the original
        g.setStroke(ABBABdvViewPrefs.dashed_stroke_slice_handle_to_atlas);

        // Needs manual clipping because otherwise the time it takes to draw lines is abysmal
        int w = bdvh.getViewerPanel().getWidth();
        int h = bdvh.getViewerPanel().getHeight();

        // draw dashed line from handle
        slicesCopy.forEach(slice -> {

            Integer[] coordSliceCenter = guiState.getSliceHandleCoords(slice);

            RealPoint handlePoint = this.getDisplayedCenter(slice);
            handlePoint.setPosition(sY/2.0, 1);
            bdvAt3D.apply(handlePoint, handlePoint);
            if (slice.isSelected()) {
                g.setColor(ABBABdvViewPrefs.color_slice_handle_selected);
            } else {
                g.setColor(ABBABdvViewPrefs.color_slice_handle_not_selected);
            }

            if ((coordSliceCenter[0]>0) && (coordSliceCenter[0]<w)) {
                if (coordSliceCenter[1]>0) {
                    if ((int) handlePoint.getDoublePosition(1)<h) {
                        g.drawLine(coordSliceCenter[0], Math.min(h,coordSliceCenter[1]),
                                (int) handlePoint.getDoublePosition(0), Math.max(0,(int) handlePoint.getDoublePosition(1)));
                    }
                }
            }
        });
    }

    private void drawCurrentSliceOverlay(Graphics2D g, List<SliceSources> slicesCopy) {

        if (iCurrentSlice != -1 && slicesCopy.size() > iCurrentSlice) {
            SliceSources slice = msp.getSlices().get(iCurrentSlice);
            //listeners.forEach(listener -> listener.isCurrentSlice(slice));
            g.setColor(ABBABdvViewPrefs.current_slice_circle_color);
            g.setStroke(ABBABdvViewPrefs.current_slice_circle_stroke);
            Integer[] coords = guiState.getSliceHandleCoords(slice);//sliceGuiState.get(slice).getSliceHandleCoords();
            RealPoint sliceCenter = new RealPoint(coords[0], coords[1], 0);
            if (slice.isKeySlice()) {
                g.drawOval((int) sliceCenter.getDoublePosition(0) - 20, (int) sliceCenter.getDoublePosition(1) - 20, 39, 39);
            } else {
                g.drawOval((int) sliceCenter.getDoublePosition(0) - 15, (int) sliceCenter.getDoublePosition(1) - 15, 29, 29);
            }
            g.setColor(ABBABdvViewPrefs.current_slice_handle_color);
            g.setFont(ABBABdvViewPrefs.arrow_on_current_slice_font);
            g.drawString("\u25C4 \u25BA", (int) (sliceCenter.getDoublePosition(0) - 20), (int) (sliceCenter.getDoublePosition(1) - 20));

            String name = slice.getName();
            int yOffset = 20;
            if (mode==REVIEW_MODE_INT) yOffset = 160;
            if (slice.isKeySlice()) name += " [Key]";
            DecimalFormat df = new DecimalFormat("00.000");
            DecimalFormat df2 = new DecimalFormat(".0");
            g.drawString("Z: "+df.format(slice.getSlicingAxisPosition()-msp.getReslicedAtlas().getZOffset())+" mm (Thickness: "+df2.format(slice.getThicknessInMm()*1000.0)+" um)", 15, yOffset+20);
            g.drawString(name, 15, yOffset);
            List<CancelableAction> actionsArray = msp.getActionsFromSlice(slice);
            if (actionsArray!=null) {
                List<CancelableAction> actions = new ArrayList<>(actionsArray); // Copy useful ?
                actions = AlignerState.filterSerializedActions(actions); // To get rid of useless actions for the user
                g.setColor(ABBABdvViewPrefs.action_summary_current_slice_color);
                g.setFont(ABBABdvViewPrefs.action_summary_current_slice_font);
                int index = 0;
                for (CancelableAction action : actions) {
                    if ((!(action instanceof MoveSliceAction)) && (!(action instanceof CreateSliceAction))) {
                        g.drawString(action.toString(), 15, yOffset + 5 + (index + 2) * 15); // -30 because of Bdv message
                        index++;
                    }
                }
            }
        }
    }

    // ----- MODE CHANGE LISTENERS

    final List<ModeListener> modeListeners = new ArrayList<>();

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

        g.setColor(ABBABdvViewPrefs.rectangle_dnd_color);

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

    }

    /**
     * TransferHandler class :
     * Controls drag and drop actions in the multislice positioner
     */
    class TransferHandler extends BdvTransferHandler {

        @Override
        public boolean canImport(TransferSupport support) {
            // First, check if the superclass can handle the import
            boolean canSuperImport = super.canImport(support);
            if (canSuperImport) return true;

            // Check if the component is a JComponent
            if (support.getComponent() instanceof JComponent) {
                // Check if the data flavor is a file list flavor
                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    DropLocation dl = support.getDropLocation();
                    updateDropLocation(support, dl);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean importData(TransferSupport support) {
            // Check if the component is a JComponent
            if (support.getComponent() instanceof JComponent) {
                // Check if the data flavor is a file list flavor
                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    try {
                        // Get the list of files being dragged
                        List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        // Check if there is exactly one file and its extension is .abba or .qpproj
                        if (files.size() == 1) {
                            File file = files.get(0);
                            String fileName = file.getName().toLowerCase();
                            if (fileName.endsWith(".abba")) {
                                new Thread(() -> BdvMultislicePositionerView.this.loadState("state_file", file)).start();
                                return true; // Indicate success
                            } else if (fileName.endsWith(".qpproj")) {
                                new Thread(() -> {
                                    MultiSlicePositioner msp = BdvMultislicePositionerView.this.msp;
                                    CommandService cs = BdvMultislicePositionerView.this.msp.getContext().getService(CommandService.class);
                                    cs.run(ImportSlicesFromQuPathCommand.class, true, "mp", msp, "qupath_project", file);
                                }).start();
                                // Handle the file import here
                                return true; // Indicate success
                            } else {
                                // Show an error message
                                JOptionPane.showMessageDialog((Component) support.getComponent(),
                                        "Please drop a file with .abba or .qpproj extension.",
                                        "Invalid Number of Files",
                                        JOptionPane.ERROR_MESSAGE);
                                return false; // Indicate failure
                            }
                        } else {
                            JOptionPane.showMessageDialog((Component) support.getComponent(),
                                    "Please drop a single file (with .abba or .qpproj extension).",
                                    "Invalid File Type",
                                    JOptionPane.ERROR_MESSAGE);
                            return false; // Indicate failure
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false; // Indicate failure
                    }
                }
            }
            // Delegate to the superclass for other cases
            return super.importData(support);
        }

        @Override
        public void updateDropLocation(TransferSupport support, DropLocation dl) {

            // Gets the point in real coordinates
            RealPoint pt3d = new RealPoint(3);
            bdvh.getViewerPanel().displayToGlobalCoordinates(dl.getDropPoint().x, dl.getDropPoint().y, pt3d);

            // Computes which slice it corresponds to (useful for overlay redraw)
            iSliceNoStep = (int) (pt3d.getDoublePosition(0) / sX);

            //Repaint the overlay only
            if (bdvRepaintEnabled()) {
                bdvh.getViewerPanel().getDisplay().repaint();
            }
        }

        /**
         * When the user drops the data -> import the slices
         *
         * @param support weird stuff for swing drag and drop TODO : link proper documentation
         * @param sacs list of source and converter to import
         */
        @Override
        public void importSourcesAndConverters(TransferSupport support, List<SourceAndConverter<?>> sacs) {
            double slicingAxisPosition = iSliceNoStep * msp.sizePixX * (int) msp.getReslicedAtlas().getStep();
            msp.createSlice(sacs.toArray(new SourceAndConverter[0]), slicingAxisPosition, msp.getAtlas().getMap().getAtlasPrecisionInMillimeter(), Tile.class, new Tile(-1));
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
            return sliceGuiState.size();
        }

        //synchronized
        void forEachSlice(Consumer<SliceGuiState> consumer) {
            sliceGuiState.values().forEach(consumer);
        }

        void forEachSelectedSlice(Consumer<SliceGuiState> consumer) {
            sliceGuiState.values().stream().filter(sliceGuiState -> sliceGuiState.slice.isSelected()).forEach(consumer);
        }

        //synchronized
        void runSlice(SliceSources slice, Consumer<SliceGuiState> consumer) {
            SliceGuiState slice_gui = sliceGuiState.get(slice);
            if (slice_gui != null) {
                consumer.accept(slice_gui);
            } else {
                logger.debug("Unavailable slice state, cannot perform operation " + consumer + " on slice " + slice);
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

    @SuppressWarnings("CanBeFinal")
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

    //-------------------- Current slice listener
    final List<CurrentSliceListener> currentSliceListeners = new ArrayList<>();

    void notifyCurrentSliceListeners() {
        synchronized (currentSliceListeners) {
            Object currentSlice = this.getCurrentSlice();
            if (currentSlice instanceof SliceSources) {
                SliceSources slice = (SliceSources) currentSlice;
                currentSliceListeners.forEach(listener -> listener.currentSliceChanged(slice));
            }
        }
    }

    public interface CurrentSliceListener {
        void currentSliceChanged(SliceSources slice);
    }

    public void addCurrentSliceListener(CurrentSliceListener listener) {
        synchronized (currentSliceListeners) {
            currentSliceListeners.add(listener);
        }
    }

    public void removeCurrentSliceListener(CurrentSliceListener listener) {
        synchronized (currentSliceListeners) {
            currentSliceListeners.remove(listener);
        }
    }

    static class WrappedIAlphaSource implements IAlphaSource {

        final Source<FloatType> alpha;
        private WrappedIAlphaSource(Source<FloatType> alpha) {
            this.alpha = alpha;
        }

        @Override
        public boolean intersectBox(AffineTransform3D affineTransform, Interval cell, int timepoint) {
            return true;
        }

        @Override
        public boolean isPresent(int t) {
            return alpha.isPresent(t);
        }

        @Override
        public RandomAccessibleInterval<FloatType> getSource(int t, int level) {
            return alpha.getSource(t, level);
        }

        @Override
        public RealRandomAccessible<FloatType> getInterpolatedSource(int t, int level, Interpolation method) {
            return alpha.getInterpolatedSource(t, level, method);
        }

        @Override
        public void getSourceTransform(int t, int level, AffineTransform3D transform) {
            alpha.getSourceTransform(t, level, transform);
        }

        @Override
        public FloatType getType() {
            return alpha.getType();
        }

        @Override
        public String getName() {
            return alpha.getName();
        }

        @Override
        public VoxelDimensions getVoxelDimensions() {
            return alpha.getVoxelDimensions();
        }

        @Override
        public int getNumMipmapLevels() {
            return alpha.getNumMipmapLevels();
        }
    }

}
