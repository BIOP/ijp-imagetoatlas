package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.action.*;
import ch.epfl.biop.atlas.aligner.adapter.*;
import ch.epfl.biop.atlas.aligner.plugin.ExternalABBARegistrationPlugin;
import ch.epfl.biop.atlas.aligner.plugin.IABBARegistrationPlugin;
import ch.epfl.biop.atlas.aligner.plugin.RegistrationPluginHelper;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.sourceandconverter.processor.*;
import ch.epfl.biop.sourceandconverter.processor.adapter.*;
import com.google.gson.*;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.base.Entity;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.realtransform.AffineTransform3D;
import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.command.Command;
import org.scijava.convert.ConvertService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.PluginService;
import org.scijava.util.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.services.SourceAndConverterServiceLoader;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.persist.RuntimeTypeAdapterFactory;
import sc.fiji.persist.ScijavaGsonHelper;

import javax.swing.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static sc.fiji.bdvpg.scijava.services.SourceAndConverterService.SPIM_DATA_INFO;

/**
 * All specific methods and fields dedicated to the multislice positioner
 *
 * There is:
 *
 * - a positioning mode
 *      This is mostly useful at the beginning of the registration
 *      Slices can be moved along the axis / stretched and shrunk
 *      Only certain sections of the atlas are shown to improve global overview, based on the user need
 *
 * - a review mode
 *      This is mostly useful for reviewing the quality of registration
 *      Only one slice is visible at a time
 *      The atlas is fully displayed
 */

public class MultiSlicePositioner implements Closeable {

    protected static Logger logger = LoggerFactory.getLogger(MultiSlicePositioner.class);

    /**
     * Object which serves as a lock in order to allow
     * for executing one task at a time if the user feedback is going
     * to be necessary
     */
    static public final Object manualActionLock = new Object();

    // Behaviour (and linked overlay) that handles user rectangular selection of sources
    // private SourceSelectorBehaviour ssb;

    /**
     * Slicing Model Properties
     * nPixD : number of pixels of the atlas
     * sD : physical dimension of the atlas
     * sizePixD : physical pixel size
     */
    final public int nPixX, nPixY, nPixZ;
    final public double sX, sY, sZ;
    final public double sizePixX, sizePixY, sizePixZ;

    // List of slices contained in multislicepositioner - publicly accessible through getSlices() method
    private List<SliceSources> slices = new ArrayList<>();//Collections.synchronizedList(new ArrayList<>());

    // Stack of actions that have been performed by the user - used for undo
    protected List<CancelableAction> userActions = new ArrayList<>();

    // Stack of actions that have been cancelled by the user - used for redo
    protected List<CancelableAction> redoableUserActions = new ArrayList<>();

    // scijava context
    Context scijavaCtx;

    // Multislice observer observes and display events happening to slices
    protected SliceActionObserver mso;

    // Resliced atlas
    ReslicedAtlas reslicedAtlas;

    // Original biop atlas
    private Atlas biopAtlas;

    // Rectangle user defined regions that crops the region of interest for registrations
    double roiPX, roiPY, roiSX, roiSY;

    // Loggers

    /**
     * Non blocking log message for users
     */
    public Consumer<String> log = (message) -> logger.info("Multipositioner : "+message);

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

    final Object slicesLock = new Object();

    /**
     * @return a copy of the array of current slices in this ABBA instance
     */
    public List<SliceSources> getSlices() { // synchronized ??
        ArrayList<SliceSources> out;
        synchronized (slicesLock) {
            out = new ArrayList<>(slices);
        }
        return out;
    }

    /**
     * Starts ABBA in a bigdataviewer window
     * @param biopAtlas an atlas
     * @param reslicedAtlas a resliced atlas
     * @param ctx a scijava context
     */
    public MultiSlicePositioner(Atlas biopAtlas, ReslicedAtlas reslicedAtlas, Context ctx) {

        logger.info("Creating MultiSlicePositioner instance");
        this.reslicedAtlas = reslicedAtlas;
        this.biopAtlas = biopAtlas;
        this.scijavaCtx = ctx;

        reslicedAtlas.setStep(50);
        reslicedAtlas.setRotateX(0);
        reslicedAtlas.setRotateY(0);

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

        mso = new SliceActionObserver(this);
        addSliceListener(mso);

        // Default registration region = full atlas size
        roiPX = -sX / 2.0;
        roiPY = -sY / 2.0;
        roiSX = sX;
        roiSY = sY;

        logger.info("MultiSlicePositioner instance created");
    }

    public ReslicedAtlas getReslicedAtlas() {
        return reslicedAtlas;
    }

    public List<SliceSources> getSelectedSlices() {
        return getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
    }

    public void close() {
        mpListeners.forEach(l -> l.closing(this));
        mpListeners.clear();
        scijavaCtx.getService(ObjectService.class).removeObject(this);
        logger.info("Closing multipositioner bdv window, releasing some resources.");
        if (mso!=null) this.mso.clear();
        if (userActions!=null) this.userActions.clear();
        if (slices!=null) this.slices.clear();
        this.redoableUserActions.clear();
        this.biopAtlas = null;
        this.slices = null;
        this.userActions = null;
        this.mso = null;
        this.reslicedAtlas = null;
        currentSerializedSlice = null;
    }

    // -------------------------------------------------------- NAVIGATION ( BOTH MODES )

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
        listeners.forEach(SliceChangeListener::roiChanged);
    }

    public double[] getROI() {
        return new double[]{roiPX, roiPY, roiSX, roiSY};
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

    public String getUndoMessage() {
        if (userActions.size()==0) {
            return "(None)";
        } else {
            CancelableAction lastAction = userActions.get(userActions.size()-1);
            if (lastAction instanceof MarkActionSequenceBatchAction) {
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
            if (lastAction instanceof MarkActionSequenceBatchAction) {
                CancelableAction lastlastAction = redoableUserActions.get(redoableUserActions.size()-2);
                return "("+lastlastAction.actionClassString()+" [batch])";
            } else {
                return "("+lastAction.actionClassString()+")";
            }
        }
    }

    public void rotateSlices(int axis, double angle_rad) {
        List<SliceSources> sortedSelected = getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        for (SliceSources slice : sortedSelected) {
            slice.rotateSourceOrigin(axis, angle_rad);
        }
    }

    protected void removeSlice(SliceSources sliceSource) {
        logger.info("Removing slice "+sliceSource+"...");
        synchronized (slicesLock) {
            slices.remove(sliceSource);
            sortSlices();
        }
        listeners.forEach(listener -> {
            logger.debug("Removing slice "+sliceSource+" - calling "+listener);
            listener.sliceDeleted(sliceSource);
        });
        logger.info("Slice "+sliceSource+" removed!");
    }

    protected void createSlice(SliceSources slice) {
        logger.info("Creating slice "+slice+"...");
        synchronized (slicesLock) {
            slices.add(slice);
            sortSlices();
        }
        listeners.forEach(listener -> {
            logger.debug("Creating slice "+slice+" - calling "+listener);
            listener.sliceCreated(slice);
        });
        logger.info("Slice "+slice+" created!");
        sortSlices(); // makes sense, no ?
    }

    private void sortSlices() {
        slices.sort(Comparator.comparingDouble(SliceSources::getSlicingAxisPosition));
        // Sending index info to slices each time this function is called
        for (int i = 0; i < slices.size(); i++) {
            slices.get(i).setIndex(i);
        }
        logger.debug("Slices sorted recomputed");
    }

    public void positionZChanged(SliceSources slice) {
        synchronized (slicesLock) {
            sortSlices();
        }
        listeners.forEach(listener -> listener.sliceZPositionChanged(slice));
    }

    public void sliceSelected(SliceSources slice) {
        listeners.forEach(listener -> listener.sliceSelected(slice));
    }

    public void sliceDeselected(SliceSources slice) {
        listeners.forEach(listener -> listener.sliceDeselected(slice));
    }

    public Atlas getAtlas() {
        return biopAtlas;
    }

    public int getNumberOfAtlasChannels() {
        return reslicedAtlas.nonExtendedSlicedSources.length;
    }

    public int getChannelBoundForSelectedSlices() {
        List<SliceSources> slices = getSelectedSlices();
        if (slices.size()==0) {
            return 0;
        } else {
            return slices.stream()
                    .mapToInt(slice -> slice.nChannels)
                    .min().getAsInt();
        }
    }

    public AffineTransform3D getAffineTransformFormAlignerToAtlas() {
        return reslicedAtlas.getSlicingTransformToAtlas();
    }

    public List<CancelableAction> getActionsFromSlice(SliceSources slice) {
        return mso.getActionsFromSlice(slice);
    }

    public Context getContext() {
        return scijavaCtx;
    }

    public int userActionsSize() {
        return userActions.size();
    }

    public int redoableUserActionsSize() {
        return redoableUserActions.size();
    }

    protected void runRequest(CancelableAction action) {
        if ((action.getSliceSources()!=null)) {
            logger.debug("Action "+action+" on slice "+action.getSliceSources()+" requested (async).");
            listeners.forEach(sliceChangeListener -> sliceChangeListener.actionEnqueue(action.getSliceSources(), action));
            action.getSliceSources().enqueueRunAction(action, () -> {} );
        } else {
            // Not asynchronous
            logger.debug("Action "+action+" on slice "+action.getSliceSources()+" run (non async).");
            listeners.forEach(sliceChangeListener -> sliceChangeListener.actionEnqueue(action.getSliceSources(), action));
            listeners.forEach(sliceChangeListener -> sliceChangeListener.actionStarted(action.getSliceSources(), action));
            addTask();
            boolean result = action.run();
            removeTask();
            listeners.forEach(sliceChangeListener -> sliceChangeListener.actionFinished(action.getSliceSources(), action, result));
            logger.debug("Action "+action+" on slice "+action.getSliceSources()+" done.");
        }
        if (action.isValid()) {
            logger.debug("Action "+action+" on slice "+action.getSliceSources()+" is valid.");
            userActions.add(action);
            logger.debug("Action "+action+" on slice "+action.getSliceSources()+" added to userActions.");
            if (redoableUserActions.size() > 0) {
                if (redoableUserActions.get(redoableUserActions.size() - 1).equals(action)) {
                    redoableUserActions.remove(redoableUserActions.size() - 1);
                } else {
                    logger.debug("DELETED REDOABLE ACTIONS");
                    // different branch : clear redoable actions
                    redoableUserActions.clear();
                }
            }
            logger.debug("Action "+action+" on slice "+action.getSliceSources()+" info sending to MultiSliceObserver.");
            logger.debug("Action "+action+" on slice "+action.getSliceSources()+" info sent to MultiSliceObserver!");
        } else {
            logger.error("Invalid action "+action+" on slice "+action.getSliceSources());
        }
    }

    protected void cancelRequest(CancelableAction action) {
        listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelEnqueue(action.getSliceSources(), action));
        if (action.isValid()) {
            if ((action.getSliceSources() == null)) {
                // Not asynchronous
                logger.debug("Non Async cancel call : " + action + " on slice "+action.getSliceSources());
                listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelStarted(action.getSliceSources(), action));
                boolean result = action.cancel();
                listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelFinished(action.getSliceSources(), action, result));
            } else {
                logger.debug("Async cancel call : " + action + " on slice "+action.getSliceSources());
                action.getSliceSources().enqueueCancelAction(action, () -> { });
            }
            if (userActions.get(userActions.size() - 1).equals(action)) {
                logger.debug(action+" cancelled on slice "+action.getSliceSources()+", updating useractions and redoable actions");
                userActions.remove(userActions.size() - 1);
                redoableUserActions.add(action);
            } else {
                logger.error("Error : cancel not called on the last action");
            }
        }
    }

    protected boolean runCreateSlice(CreateSliceAction createSliceAction) {
        synchronized (CreateSliceAction.class) { // only one slice addition at a time
            boolean sacAlreadyPresent = false;
            for (SourceAndConverter sac : createSliceAction.getSacs()) {
                for (SliceSources slice : getSlices()) {
                    for (SourceAndConverter test : slice.getOriginalSources()) {
                        if (test.equals(sac)) {
                            sacAlreadyPresent = true;
                            break;
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
                    if (ss.exactMatch(createSliceAction.getSacs())) {
                        exactMatch = true;
                        zeSlice = ss;
                    }
                }

                if (!exactMatch) {
                    log.accept("A source is already used in the positioner : slice not created.");
                    return false;
                } else {
                    // Move action:
                    new MoveSliceAction(this, zeSlice, createSliceAction.slicingAxisPosition).runRequest();
                    return false;
                }
            }

            if (createSliceAction.getSlice() == null) {// for proper redo function
                createSliceAction.setSlice(new SliceSources(createSliceAction.getSacs().toArray(new SourceAndConverter[0]),
                        createSliceAction.slicingAxisPosition, this, createSliceAction.zSliceThicknessCorrection, createSliceAction.zSliceShiftCorrection));
            }

            createSlice(createSliceAction.getSlice());

            log.accept("Slice added");
        }
        return true;
    }

    protected boolean cancelCreateSlice(CreateSliceAction action) {
        removeSlice(action.getSlice());
        log.accept("Slice "+action.getSlice()+" removed ");
        return true;
    }

    //-----------------------------------------

    public SliceSources createSlice(SourceAndConverter[] sacsArray, double slicingAxisPosition) {
        CreateSliceAction cs = new CreateSliceAction(this, Arrays.asList(sacsArray), slicingAxisPosition,1,0);
        cs.runRequest();
        return cs.getSlice();
    }

    public <T extends Entity> List<SliceSources> createSlice(SourceAndConverter[] sacsArray, double slicingAxisPosition, double axisIncrement, final Class<T> attributeClass, T defaultEntity) {
        List<SliceSources> out = new ArrayList<>();
        List<SourceAndConverter<?>> sacs = Arrays.asList(sacsArray);
        if ((sacs.size() > 1) && (attributeClass != null)) {

            // Check whether the source can be splitted
            // Split based on attribute argument (usually Tile.class)

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

            new MarkActionSequenceBatchAction(this).runRequest();
            for (int i = 0; i < sortedTiles.size(); i++) {
                T group = sortedTiles.get(i);
                if (group.getId()!=-1) {
                    CreateSliceAction cs = new CreateSliceAction(this, sacsGroups.get(group), slicingAxisPosition + i * axisIncrement,1,0);
                    cs.runRequest();
                    if (cs.getSlice() != null) {
                        out.add(cs.getSlice());
                    }
                }
            }
            new MarkActionSequenceBatchAction(this).runRequest();

        } else {
            CreateSliceAction cs = new CreateSliceAction(this, sacs, slicingAxisPosition,1,0);
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
        new MoveSliceAction(this, slice, axisPosition).runRequest();
    }

    /**
     * Equal spacing between selected slices, respecting the location of key slices
     * (key slices are not moved)
     * Also the first and last selected slices are not moved
     */
    public void equalSpacingSelectedSlices() {
        List<SliceSources> sortedSelected = getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (sortedSelected.size() > 2) {
            int indexPreviousKey = 0;
            int indexNextKey = 1;
            new MarkActionSequenceBatchAction(this).runRequest();
            while (indexNextKey<sortedSelected.size()) {
                if ((sortedSelected.get(indexNextKey).isKeySlice())||(indexNextKey==sortedSelected.size()-1)) {
                    double totalSpacing = sortedSelected.get(indexNextKey).getSlicingAxisPosition()-sortedSelected.get(indexPreviousKey).getSlicingAxisPosition();
                    double delta = totalSpacing / (double) (indexNextKey-indexPreviousKey);
                    for (int i = indexPreviousKey + 1; i<indexNextKey; i++) {
                        moveSlice(sortedSelected.get(i), sortedSelected.get(indexPreviousKey).getSlicingAxisPosition() + ((double) i-(indexPreviousKey)) * delta);
                    }
                    indexPreviousKey = indexNextKey;
                }
                indexNextKey++;
            }
            new MarkActionSequenceBatchAction(this).runRequest();
        }
    }

    public void editLastRegistrationSelectedSlices(boolean reuseOriginalChannels, SourcesProcessor preprocessSlice, SourcesProcessor preprocessAtlas) {
        if (getSelectedSlices().size()==0) {
            warningMessageForUser.accept("No selected slice", "Please select the slice you want to edit");
            log.accept("Edit registration ignored : no slice selected");
        } else {
            for (SliceSources slice : getSelectedSlices()) {
                new EditLastRegistrationAction(this, slice, reuseOriginalChannels, preprocessSlice, preprocessAtlas ).runRequest();
            }
        }
    }

    public static Map<String,String> convertToString(Context ctx, Map<String, Object> params) {
        Map<String,String> convertedParams = new HashMap<>();

        ConvertService cs = ctx.getService(ConvertService.class);

        params.keySet().forEach(k -> convertedParams.put(k, cs.convert(params.get(k), String.class)));

        return convertedParams;
    }

    public void registerSelectedSlices(Command command,
                                       SourcesProcessor preprocessFixed,
                                       SourcesProcessor preprocessMoving) {
        registerSelectedSlices(command,
                preprocessFixed,
                preprocessMoving,
                new HashMap<>()
        );
    }

    /**
     * Main function which triggers registration of the selected slices
     * @param command the ui command
     * @param preprocessFixed how fixed sources need to be preprocessed before being registered
     * @param preprocessMoving how moving sources need to be preprocessed before being registered
     * @param parameters parameters used for the registration - all objects will be converted
     *                   to String using the scijava {@link ConvertService}. They need to be strings
     *                   to be serialized
     */
    public void registerSelectedSlices(Command command,
                                       SourcesProcessor preprocessFixed,
                                       SourcesProcessor preprocessMoving,
                                       Map<String,Object> parameters) {
        registerSelectedSlices(RegistrationPluginHelper.registrationFromUI(scijavaCtx,command.getClass()),
                preprocessFixed,
                preprocessMoving,
                parameters
        );
    }

    /**
     * Main function which triggers registration of the selected slices
     * @param registrationClass the kind of registration which should be started
     * @param preprocessFixed how fixed sources need to be preprocessed before being registered
     * @param preprocessMoving how moving sources need to be preprocessed before being registered
     * @param parameters parameters used for the registration - all objects will be converted
     *                   to String using the scijava {@link ConvertService}. They need to be strings
     *                   to be serialized
     */
    public void registerSelectedSlices(Class<? extends IABBARegistrationPlugin> registrationClass,
                                       SourcesProcessor preprocessFixed,
                                       SourcesProcessor preprocessMoving,
                                       Map<String,Object> parameters) {

        PluginService ps = scijavaCtx.getService(PluginService.class);
        Supplier<? extends IABBARegistrationPlugin> pluginSupplier =
                () -> {
                    try {
                        return (IABBARegistrationPlugin) ps.getPlugin(registrationClass).createInstance();
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                        return null;
                    }
                };

        registerSelectedSlices(pluginSupplier, preprocessFixed, preprocessMoving, parameters);
    }

    /**
     * Main function which triggers registration of the selected slices
     * @param registrationPluginName name of the registration plugin - external
     * @param preprocessFixed how fixed sources need to be preprocessed before being registered
     * @param preprocessMoving how moving sources need to be preprocessed before being registered
     * @param parameters parameters used for the registration - all objects will be converted
     *                   to String using the scijava {@link ConvertService}. They need to be strings
     *                   to be serialized
     */
    public void registerSelectedSlices(String registrationPluginName,
                                       SourcesProcessor preprocessFixed,
                                       SourcesProcessor preprocessMoving,
                                       Map<String,Object> parameters) {
        if (externalRegistrationPlugins.containsKey(registrationPluginName)) {
            registerSelectedSlices(externalRegistrationPlugins.get(registrationPluginName),
                    preprocessFixed, preprocessMoving, parameters);
        } else {
            this.errlog.accept("Registration type:"+registrationPluginName+" not found!");
        }
    }


    /**
     * Main function which triggers registration of the selected slices
     * @param registrationPluginSupplier a supplier of registration plugins
     * @param preprocessFixed how fixed sources need to be preprocessed before being registered
     * @param preprocessMoving how moving sources need to be preprocessed before being registered
     * @param parameters parameters used for the registration - all objects will be converted
     *                   to String using the scijava {@link ConvertService}. They need to be strings
     *                   to be serialized
     */
    public void registerSelectedSlices(Supplier<? extends IABBARegistrationPlugin> registrationPluginSupplier,
                                       SourcesProcessor preprocessFixed,
                                       SourcesProcessor preprocessMoving,
                                       Map<String,Object> parameters) {
        if (getSelectedSlices().size()==0) {
            warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
            log.accept("Registration ignored : no slice selected");
        } else {

            // Putting user defined ROIs
            parameters.put("px", roiPX);
            parameters.put("py", roiPY);
            parameters.put("sx", roiSX);
            parameters.put("sy", roiSY);

            for (SliceSources slice : getSelectedSlices()) {
                IABBARegistrationPlugin registration = registrationPluginSupplier.get();
                if (registration!=null) {
                    registration.setScijavaContext(scijavaCtx);

                    registration.setSliceInfo(new SliceInfo(this, slice));

                    // Sends parameters to the registration
                    registration.setRegistrationParameters(convertToString(scijavaCtx, parameters));

                    // Always set slice at zero position for registration
                    parameters.put("pz", 0);
                    AffineTransform3D at3d = new AffineTransform3D();
                    at3d.translate(0, 0, -slice.getSlicingAxisPosition());
                    SourcesAffineTransformer z_zero = new SourcesAffineTransformer(at3d);

                    new RegisterSliceAction(this, slice, registration, SourcesProcessorHelper.compose(z_zero, preprocessFixed), SourcesProcessorHelper.compose(z_zero, preprocessMoving)).runRequest();
                }
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
            if (action instanceof MarkActionSequenceBatchAction) {
                action.cancelRequest();
                action = userActions.get(userActions.size() - 1);
                while (!(action instanceof MarkActionSequenceBatchAction)) {
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
            if (action instanceof MarkActionSequenceBatchAction) {
                action.runRequest();
                action = redoableUserActions.get(redoableUserActions.size() - 1);
                while (!(action instanceof MarkActionSequenceBatchAction)) {
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

    // ------------------------------------------------ Serialization / Deserialization

    Gson getGsonStateSerializer(List<SourceAndConverter> serialized_sources) {
        GsonBuilder gsonbuilder = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(SourceAndConverter.class, new IndexedSourceAndConverterAdapter(serialized_sources))
                .registerTypeAdapter(SourceAndConverter[].class, new IndexedSourceAndConverterArrayAdapter(serialized_sources));

        // Now gets all custom serializers for RealTransform.class, using Scijava extensibility plugin
        // Most of the adapters comes from Bdv-Playground

        ScijavaGsonHelper.getGsonBuilder(scijavaCtx, gsonbuilder, true);

        gsonbuilder.registerTypeHierarchyAdapter(AlignerState.SliceSourcesState.class, new SliceSourcesStateDeserializer((slice) -> currentSerializedSlice = slice));


        // For actions serialization
        RuntimeTypeAdapterFactory<CancelableAction> factoryActions = RuntimeTypeAdapterFactory.of(CancelableAction.class);

        factoryActions.registerSubtype(CreateSliceAction.class);
        factoryActions.registerSubtype(MoveSliceAction.class);
        factoryActions.registerSubtype(RegisterSliceAction.class);
        factoryActions.registerSubtype(KeySliceOnAction.class);
        factoryActions.registerSubtype(KeySliceOffAction.class);

        gsonbuilder.registerTypeAdapterFactory(factoryActions);
        gsonbuilder.registerTypeHierarchyAdapter(CreateSliceAction.class, new CreateSliceAdapter(this));
        gsonbuilder.registerTypeHierarchyAdapter(MoveSliceAction.class, new MoveSliceAdapter(this, this::currentSliceGetter));
        gsonbuilder.registerTypeHierarchyAdapter(RegisterSliceAction.class, new RegisterSliceAdapter(this, this::currentSliceGetter));
        gsonbuilder.registerTypeHierarchyAdapter(KeySliceOnAction.class, new KeySliceOnAdapter(this, this::currentSliceGetter));
        gsonbuilder.registerTypeHierarchyAdapter(KeySliceOffAction.class, new KeySliceOffAdapter(this, this::currentSliceGetter));

        // For registration registration
        RuntimeTypeAdapterFactory<Registration> factoryRegistrations = RuntimeTypeAdapterFactory.of(Registration.class);
        gsonbuilder.registerTypeAdapterFactory(factoryRegistrations);

        PluginService pluginService = scijavaCtx.getService(PluginService.class);

        // Creates adapter for all registration plugins
        RegistrationAdapter registrationAdapter = new RegistrationAdapter(scijavaCtx, this);
        pluginService.getPluginsOfType(IABBARegistrationPlugin.class).forEach(registrationPluginClass -> {
            IABBARegistrationPlugin plugin = pluginService.createInstance(registrationPluginClass);
            factoryRegistrations.registerSubtype(plugin.getClass());
            gsonbuilder.registerTypeHierarchyAdapter(plugin.getClass(), registrationAdapter);
        });

        factoryRegistrations.registerSubtype(ExternalABBARegistrationPlugin.class);
        gsonbuilder.registerTypeHierarchyAdapter(ExternalABBARegistrationPlugin.class, registrationAdapter);

        // For sources processor

        RuntimeTypeAdapterFactory<SourcesProcessor> factorySourcesProcessor = RuntimeTypeAdapterFactory.of(SourcesProcessor.class);

        factorySourcesProcessor.registerSubtype(SourcesAffineTransformer.class);
        factorySourcesProcessor.registerSubtype(SourcesChannelsSelect.class);
        factorySourcesProcessor.registerSubtype(SourcesProcessComposer.class);
        factorySourcesProcessor.registerSubtype(SourcesResampler.class);
        factorySourcesProcessor.registerSubtype(SourcesIdentity.class);

        gsonbuilder.registerTypeAdapterFactory(factorySourcesProcessor);
        gsonbuilder.registerTypeHierarchyAdapter(SourcesChannelsSelect.class, new SourcesChannelSelectAdapter());
        gsonbuilder.registerTypeHierarchyAdapter(SourcesAffineTransformer.class, new SourcesAffineTransformerAdapter());
        gsonbuilder.registerTypeHierarchyAdapter(SourcesResampler.class, new SourcesResamplerAdapter());
        gsonbuilder.registerTypeHierarchyAdapter(SourcesProcessComposer.class, new SourcesComposerAdapter());
        gsonbuilder.registerTypeHierarchyAdapter(SourcesIdentity.class, new SourcesIdentityAdapter());

        return gsonbuilder.create();
    }

    private boolean stateChangedSinceLastSave = false;

    public void stateHasBeenChanged() {
        stateChangedSinceLastSave = true;
    }

    public boolean isModifiedSinceLastSave() {
        return stateChangedSinceLastSave;
    }

    public void saveState(File stateFile, boolean overwrite) {
        addTask();
        if (slices.size() == 0) {
            errorMessageForUser.accept("No Slices To Save", "No slices are present. Nothing saved");
            return;
        }

        slices.get(0).waitForEndOfTasks();

        // Wait patiently for all tasks to be performed
        log.accept("Waiting for all tasks to be finished ... ");
        getSlices().forEach(SliceSources::waitForEndOfTasks);
        log.accept("All tasks have been performed!");

        // First save all sources required in the state
        List<SourceAndConverter> allSacs = new ArrayList<>();

        getSlices().forEach(sliceSource -> allSacs.addAll(Arrays.asList(sliceSource.getOriginalSources())));

        String fileNoExt = FilenameUtils.removeExtension(stateFile.getAbsolutePath());
        File sacsFile = new File(fileNoExt+"_sources.json");

        if (sacsFile.exists()&&(!overwrite)) {
            logger.error("File "+sacsFile.getAbsolutePath()+" already exists. Abort command");
            return;
        }

        SourceAndConverterServiceSaver sacss = new SourceAndConverterServiceSaver(sacsFile,this.scijavaCtx,allSacs);
        sacss.run();
        List<SourceAndConverter> serialized_sources = new ArrayList<>();

        sacss.getSacToId().values().stream().sorted().forEach(i -> serialized_sources.add(sacss.getIdToSac().get(i)));

        try {
            FileWriter writer = new FileWriter(stateFile.getAbsolutePath());
            AlignerState alignerState = new AlignerState(this);
            alignerState.version = VersionUtils.getVersion(AlignerState.class);
            getGsonStateSerializer(serialized_sources).toJson(alignerState, writer);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stateChangedSinceLastSave = false;
        removeTask();
    }

    public void loadState(File stateFile) {
        addTask();
        boolean emptyState = this.slices.size()==0;
        // TODO : add a clock as an overlay
        getSlices().forEach(SliceSources::waitForEndOfTasks);

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

                JsonObject element = gson.fromJson(fileReader, JsonObject.class);
                // Didn't have the foresight to add a version number from the start...
                String version = element.has("version") ? element.get("version").getAsString() : null;
                if (version == null) {
                    log.accept("Old state version, conversion required.");
                    element = (JsonObject) AlignerState.convertOldJson(element);
                    log.accept("Conversion done.");
                    // throw new IllegalArgumentException("Older-style project is not compatible with this current FIJI QuPath bridge ");
                    //				return LegacyProject.readFromFile(fileProject, cls);
                }

                AlignerState state = gson.fromJson(element, AlignerState.class); // actions are executed during deserialization
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

                state.slices_state_list.forEach(sliceState -> {
                    sliceState.slice.waitForEndOfTasks();
                    sliceState.slice.transformSourceOrigin((AffineTransform3D) (sliceState.preTransform));
                });

                if (emptyState) stateChangedSinceLastSave = false; // loaded state has not been changed, and it was the only one loaded

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            errlog.accept("Error : file "+stateFile.getAbsolutePath()+" not found!");
        }
        removeTask();
    }

    volatile private SliceSources currentSerializedSlice = null;

    //-------------------- Event listeners

    SliceSources currentSliceGetter() {
        return currentSerializedSlice;
    }

    List<SliceChangeListener> listeners = new ArrayList<>();

    public void addSliceListener(SliceChangeListener listener) {
        logger.debug("Adding slice change listener :"+listener+" of class"+listener.getClass().getSimpleName());
        listeners.add(listener);
    }

    public void removeSliceListener(SliceChangeListener listener) {
        listeners.remove(listener);
    }

    public void notifySourcesChanged(SliceSources sliceSources) {
        listeners.forEach(sliceChangeListener -> sliceChangeListener.sliceSourcesChanged(sliceSources));
    }

    public void slicePreTransformChanged(SliceSources sliceSources) {
        stateHasBeenChanged();
        listeners.forEach(sliceChangeListener -> sliceChangeListener.slicePretransformChanged(sliceSources));
    }

    protected AtomicInteger numberOfTasks = new AtomicInteger();

    /**
     * Simply indicates that a task is current being performed.
     * This can be useful to indicate to the user that something is currently waited for or going on.
     * You need to indicate when it has ended with {@link MultiSlicePositioner#removeTask()}
     * See {@link MultiSlicePositioner#getNumberOfTasks()}
     */
    public void addTask() {
        numberOfTasks.incrementAndGet();
        logger.debug("Task added. Current number of tasks: "+numberOfTasks.get());
    }

    /**
     * Indicates that a task has been finished.
     * See {@link MultiSlicePositioner#addTask()}
     * See {@link MultiSlicePositioner#getNumberOfTasks()}
     */
    public void removeTask(){
        numberOfTasks.decrementAndGet();
        logger.debug("Task removed. Current number of tasks: "+numberOfTasks.get());
    }

    /**
     *
     * @return the number of tasks currently being performed
     */
    public int getNumberOfTasks() {
        return numberOfTasks.get();
    }

    public interface SliceChangeListener {
        void sliceDeleted(SliceSources slice);
        void sliceCreated(SliceSources slice);
        void sliceZPositionChanged(SliceSources slice);
        void sliceSelected(SliceSources slice);
        void sliceDeselected(SliceSources slice);
        void sliceSourcesChanged(SliceSources slice);
        void slicePretransformChanged(SliceSources slice);
        void sliceKeyOn(SliceSources slice);
        void sliceKeyOff(SliceSources slice);

        void roiChanged();

        void actionEnqueue(SliceSources slice, CancelableAction action);
        void actionStarted(SliceSources slice, CancelableAction action);
        void actionFinished(SliceSources slice, CancelableAction action, boolean result);
        void actionCancelEnqueue(SliceSources slice, CancelableAction action);
        void actionCancelStarted(SliceSources slice, CancelableAction action);
        void actionCancelFinished(SliceSources slice, CancelableAction action, boolean result);
    }

    public interface MultiSlicePositionerListener {
        void closing(MultiSlicePositioner msp);
    }

    List<MultiSlicePositionerListener> mpListeners = new ArrayList<>();

    public void addMultiSlicePositionerListener(MultiSlicePositionerListener listener) {
        mpListeners.add(listener);
    }

    public void removeMultiSlicePositionerListener(MultiSlicePositionerListener listener) {
        mpListeners.remove(listener);
    }

    /**
     * Informations sent to the registration server (provided the user agrees)
     */
    public static class SliceInfo {

        public SliceInfo(MultiSlicePositioner mp, SliceSources slice) {
            sliceAxisPosition = slice.getSlicingAxisPosition();
            matrixAtlas = mp.getReslicedAtlas().getSlicingTransform().getRowPackedCopy();
            matrixAlignerToAtlas = mp.getAffineTransformFormAlignerToAtlas().getRowPackedCopy();
            rotX = mp.getReslicedAtlas().getRotateX();
            rotY = mp.getReslicedAtlas().getRotateY();
            sliceHashCode = slice.hashCode();
            sessionHashcode = mp.hashCode();
            abbaInfoVersion = VersionUtils.getVersion(MultiSlicePositioner.class);
            atlas = mp.getAtlas().getName();
        }

        String type = "ABBA Registration"; // Tag to know where from which software this job comes from

        String abbaInfoVersion; // To handle future versions

        String atlas; // Could be made modular later - for now ABBA only uses this

        int sliceHashCode; // Provide a way to know if the same slice was registered several times

        int sessionHashcode; // Anonymously identifies a full ABBA session

        double[] matrixAtlas; // How the atlas is sliced (rotation correction NOT taken into acount )

        double[] matrixAlignerToAtlas; // How the atlas is sliced (rotation correction taken into account)

        double sliceAxisPosition; // Position of the slice along the atlas

        double rotX, rotY; // Rotation x and y slicing correction

    }

    //---------------------- For PyImageJ extensions

    static Map<String, Supplier<? extends IABBARegistrationPlugin>> externalRegistrationPlugins = new HashMap<>();

    /**
     * Register an external registration plugin, for instance performed by a python function
     * through PyImageJ
     * @param name of the registration plugin
     * @param pluginSupplier the thing that makes a new plugin of this kind
     */
    public static void registerRegistrationPlugin(String name, Supplier<? extends IABBARegistrationPlugin> pluginSupplier) {
        externalRegistrationPlugins.put(name, pluginSupplier);
    }

    public static boolean isExternalRegistrationPlugin(String name) {
        return externalRegistrationPlugins.containsKey(name);
    }

    public static Supplier<? extends IABBARegistrationPlugin> getExternalRegistrationPluginSupplier(String name) {
        return externalRegistrationPlugins.get(name);
    }

    static Map<String, List<String>> externalRegistrationPluginsUI = new HashMap<>();

    public Map<String, List<String>> getExternalRegistrationPluginsUI() {
        return externalRegistrationPluginsUI;
    }

    public static void registerRegistrationPluginUI(String registrationTypeName, String registrationUICommandName) {
        if (!externalRegistrationPluginsUI.containsKey(registrationTypeName)) {
            externalRegistrationPluginsUI.put(registrationTypeName, new ArrayList<>());
        }
        externalRegistrationPluginsUI.get(registrationTypeName).add(registrationUICommandName);
    }

}