package ch.epfl.biop.atlastoimg2d.multislice;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.allen.AllenOntology;
import ch.epfl.biop.atlas.allen.AllenOntologyJson;
import ch.epfl.biop.atlas.commands.ConstructROIsFromImgLabel;
import ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices.PutAtlasStructureToImageNoRoiManager;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.types.CompositeFloatPoly;
import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import ch.epfl.biop.java.utilities.roi.types.ImageJRoisFile;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.qupathfiji.MinimalQuPathProject;
import ch.epfl.biop.qupathfiji.QuPathEntryEntity;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.sourceandconverter.AffineTransformedSourceWrapperRegistration;
import ch.epfl.biop.registration.sourceandconverter.CenterZeroRegistration;
import ch.epfl.biop.scijava.command.ExportToImagePlusCommand;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.RealPoint;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterInspector;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static ch.epfl.biop.scijava.command.QuPathProjectToBDVDatasetCommand.QUPATH_LINKED_PROJECT;
import static net.imglib2.cache.img.DiskCachedCellImgOptions.options;

public class SliceSources {

    // What are they ?
    SourceAndConverter[] original_sacs;

    // Visible to the user in slicing mode
    SourceAndConverter<?>[] relocated_sacs_positioning_mode;

    // Used for registration : like 3D, but tilt and roll ignored because it is handled on the fixed source side
    SourceAndConverter<?>[] registered_sacs;

    Map<Registration, SourceAndConverter[]> registered_sacs_sequence = new HashMap<>();

    // Where are they ?
    volatile double slicingAxisPosition;

    private boolean isSelected = false;

    double yShift_slicing_mode = 0;

    final MultiSlicePositioner mp;

    List<GraphicalHandle> ghs = new ArrayList<>();

    Behaviours behavioursHandleSlice;

    volatile AffineTransformedSourceWrapperRegistration zPositioner;

    volatile AffineTransformedSourceWrapperRegistration slicingModePositioner;

    CenterZeroRegistration centerPositioner;

    volatile ImagePlus impLabelImage = null;

    volatile AffineTransform3D at3DLastLabelImage = null;

    volatile boolean labelImageBeingComputed = false;

    volatile ConvertibleRois cvtRoisOrigin = null;

    volatile ConvertibleRois cvtRoisTransformed = null;

    List<Registration> registrations = new ArrayList<>();

    List<CompletableFuture<Boolean>> tasks = new ArrayList<>();

    Map<CancelableAction, CompletableFuture> mapActionTask = new HashMap<>();

    volatile CancelableAction actionInProgress = null;

    // For fast display : Icon TODO : see https://github.com/bigdataviewer/bigdataviewer-core/blob/17d2f55d46213d1e2369ad7ef4464e3efecbd70a/src/main/java/bdv/tools/RecordMovieDialog.java#L256-L318
    protected SliceSources(SourceAndConverter[] sacs, double slicingAxisPosition, MultiSlicePositioner mp) {
        this.mp = mp;
        this.original_sacs = sacs;
        this.slicingAxisPosition = slicingAxisPosition;
        this.registered_sacs = this.original_sacs;

        centerPositioner = new CenterZeroRegistration();
        centerPositioner.setMovingImage(registered_sacs);

        zPositioner = new AffineTransformedSourceWrapperRegistration();

        behavioursHandleSlice = new Behaviours(new InputTriggerConfig());
        behavioursHandleSlice.behaviour(mp.getSelectedSourceDragBehaviour(this), "dragSelectedSources" + this.toString(), "button1");
        behavioursHandleSlice.behaviour((ClickBehaviour) (x, y) -> {
            deSelect();
            mp.bdvh.getViewerPanel().requestRepaint();
        }, "deselectedSources" + this.toString(), "button3", "ctrl button1");

        GraphicalHandle gh = new CircleGraphicalHandle(mp,
                behavioursHandleSlice,
                mp.bdvh.getTriggerbindings(),
                this.toString(), // pray for unicity ? TODO : do better than thoughts and prayers
                this::getBdvHandleCoords,
                this::getBdvHandleRadius,
                this::getBdvHandleColor
        );
        ghs.add(gh);
        iniPosition();
    }

    protected double getSlicingAxisPosition() {
        return slicingAxisPosition;
    }

    protected void setSlicingAxisPosition(double newSlicingAxisPosition) {
        slicingAxisPosition = newSlicingAxisPosition;
    }

    void iniPosition() {
        runRegistration(centerPositioner, Function.identity(), Function.identity());
        runRegistration(zPositioner, Function.identity(), Function.identity());
        waitForEndOfTasks();
        updatePosition();
    }

    public synchronized void select() {
        this.isSelected = true;
    }

    public synchronized void deSelect() {
        this.isSelected = false;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    private int currentSliceIndex = -1;

    public int getIndex() {
        return currentSliceIndex;
    }

    protected void setIndex(int idx) {
        currentSliceIndex = idx;
    }

    protected Integer[] getBdvHandleCoords() {
        AffineTransform3D bdvAt3D = new AffineTransform3D();
        mp.bdvh.getViewerPanel().state().getViewerTransform(bdvAt3D);
        RealPoint sliceCenter = new RealPoint(3);
        if (mp.currentMode == MultiSlicePositioner.POSITIONING_MODE_INT) {
            sliceCenter = getCenterPositionPMode();
            bdvAt3D.apply(sliceCenter, sliceCenter);
            return new Integer[]{(int) sliceCenter.getDoublePosition(0), (int) sliceCenter.getDoublePosition(1), (int) sliceCenter.getDoublePosition(2)};
        } else if (mp.currentMode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
            RealPoint zero = new RealPoint(3);
            zero.setPosition(0, 0);
            bdvAt3D.apply(zero, zero);
            return new Integer[]{35 * (currentSliceIndex - mp.slices.size() / 2) + (int) zero.getDoublePosition(0), 20, 0};
        } else {
            return new Integer[]{0, 0, 0};
        }
    }

    protected String getActionState(CancelableAction action) {
        if ((action!=null)&&(action == actionInProgress)) {
            return "(pending)";
        }
        if (mapActionTask.containsKey(action)) {
            if (tasks.contains(mapActionTask.get(action))) {
                CompletableFuture future = tasks.get(tasks.indexOf(mapActionTask.get(action)));
                if (future.isDone()) {
                    return "(done)";
                } else if (future.isCancelled()) {
                    return "(cancelled)";
                } else {
                    return "(locked)";
                }
            } else {
                return "future not found";
            }
        } else {
            return "unknown action";
        }
    }

    public Integer[] getBdvHandleColor() {
        if (isSelected) {
            return new Integer[]{0, 255, 0, 200};

        } else {
            return new Integer[]{255, 255, 0, 64};
        }
    }

    public Integer getBdvHandleRadius() {
        return 12;
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

    public void waitForEndOfTasks() {
        if (tasks.size()>0) {
            try {
                CompletableFuture lastTask = tasks.get(tasks.size()-1);
                lastTask.get();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Some registration were cancelled");
            }
        }
    }

    protected void updatePosition() {
        AffineTransform3D zShiftAffineTransform = new AffineTransform3D();
        zShiftAffineTransform.translate(0, 0, slicingAxisPosition);
        zPositioner.setAffineTransform(zShiftAffineTransform); // Moves the registered slices to the correct position
        AffineTransform3D slicingModePositionAffineTransform = new AffineTransform3D();
        RealPoint center = getCenterPositionPMode();
        slicingModePositionAffineTransform.translate(center.getDoublePosition(0), center.getDoublePosition(1), -slicingAxisPosition);
        slicingModePositioner.setAffineTransform(slicingModePositionAffineTransform);
    }

    public RealPoint getCenterPositionPMode() {
        double slicingAxisSnapped = (((int) ((slicingAxisPosition) / mp.sizePixX)) * mp.sizePixX);
        double posX = (slicingAxisSnapped / mp.sizePixX * mp.sX / mp.reslicedAtlas.getStep()) + 0.5 * (mp.sX);
        double posY = mp.sY * yShift_slicing_mode;
        return new RealPoint(posX, posY, 0);
    }

    public RealPoint getCenterPositionRMode() {
        return new RealPoint(0, 0, slicingAxisPosition);
    }

    public void appendRegistration(Registration<SourceAndConverter[]> reg) {
        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .remove(mp.bdvh, registered_sacs);

        registered_sacs = reg.getTransformedImageMovingToFixed(registered_sacs);

        slicingModePositioner = new AffineTransformedSourceWrapperRegistration();

        slicingModePositioner.setMovingImage(registered_sacs);
        SourceAndConverterServices.getSourceAndConverterService().remove(relocated_sacs_positioning_mode);

        relocated_sacs_positioning_mode = slicingModePositioner.getTransformedImageMovingToFixed(registered_sacs);
        updatePosition();

        registered_sacs_sequence.put(reg, registered_sacs);

        if (mp.currentMode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
            SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .show(mp.bdvh, registered_sacs);
        }

        if (mp.currentMode == MultiSlicePositioner.POSITIONING_MODE_INT) {
            SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .show(mp.bdvh, relocated_sacs_positioning_mode);
            enableGraphicalHandles();
        }

        registrations.add(reg);

        mp.mso.updateInfoPanel(this);

    }

    // public : enqueueRegistration
    private boolean performRegistration(Registration<SourceAndConverter[]> reg,
                                       Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed,
                                       Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving) {

        reg.setFixedImage(preprocessFixed.apply(mp.reslicedAtlas.nonExtendedSlicedSources));
        reg.setMovingImage(preprocessMoving.apply(registered_sacs));
        boolean out = reg.register();
        if (!out) {

        } else {
            appendRegistration(reg);
        }
        return out;
    }

    /**
     * Asynchronous handling of registrations + combining with manual sequential registration if necessary
     *
     * @param reg
     */

    protected void runRegistration(Registration<SourceAndConverter[]> reg,
                                Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed,
                                Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving) {

        if (reg.isManual()) {
            //Waiting for manual lock release...
            synchronized (MultiSlicePositioner.manualActionLock) {
                //Manual lock released
                performRegistration(reg,preprocessFixed, preprocessMoving);
            }
        } else {
            performRegistration(reg,preprocessFixed, preprocessMoving);
        }

    }

    protected synchronized boolean removeRegistration(Registration reg) {
        if (registrations.contains(reg)) {
            int idx = registrations.indexOf(reg);
            if (idx == registrations.size() - 1) {

                registrations.remove(reg);
                registered_sacs_sequence.remove(reg);

                Registration last = registrations.get(registrations.size() - 1);

                SourceAndConverterServices.getSourceAndConverterService()
                        .remove(registered_sacs);
                SourceAndConverterServices.getSourceAndConverterService()
                        .remove(relocated_sacs_positioning_mode);

                registered_sacs = registered_sacs_sequence.get(last);

                slicingModePositioner = new AffineTransformedSourceWrapperRegistration();

                slicingModePositioner.setMovingImage(registered_sacs);
                SourceAndConverterServices.getSourceAndConverterService().remove(relocated_sacs_positioning_mode);

                relocated_sacs_positioning_mode = slicingModePositioner.getTransformedImageMovingToFixed(registered_sacs);
                updatePosition();

                if (mp.currentMode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
                    SourceAndConverterServices.getSourceAndConverterDisplayService()
                            .show(mp.bdvh, registered_sacs);
                }

                if (mp.currentMode == MultiSlicePositioner.POSITIONING_MODE_INT) {
                    SourceAndConverterServices.getSourceAndConverterDisplayService()
                            .show(mp.bdvh, relocated_sacs_positioning_mode);
                    enableGraphicalHandles();
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected void enqueueRunAction(CancelableAction action, Runnable postRun) {
        synchronized(tasks) {
            CompletableFuture<Boolean> startingPoint;
            if (tasks.size() == 0) {
                startingPoint = CompletableFuture.supplyAsync(() -> true);
            } else {
                startingPoint = tasks.get(tasks.size() - 1);
            }
            tasks.add(startingPoint.thenApplyAsync((out) -> {
                if (out == true) {
                    actionInProgress = action;
                    boolean result = action.run();
                    actionInProgress = null;
                    postRun.run();
                    return result;
                } else {
                    return false;
                }
            }));
            tasks.add(tasks.get(tasks.size() - 1).thenApplyAsync(
                (out) -> {
                    mp.mso.updateInfoPanel(this);
                    return out;}
                ));
            mapActionTask.put(action, tasks.get(tasks.size() - 1));
        }
    }

    protected synchronized void enqueueCancelAction(CancelableAction action, Runnable postRun) {
        synchronized(tasks) {
            // Has the action started ?
            if (mapActionTask.containsKey(action)) {
                if (mapActionTask.get(action).isDone() || ((action!=null)&&(action == this.actionInProgress))) {
                    CompletableFuture<Boolean> startingPoint;
                    if (tasks.size() == 0) {
                        startingPoint = CompletableFuture.supplyAsync(() -> true);
                    } else {
                        startingPoint = tasks.get(tasks.size() - 1);
                    }
                    tasks.add(startingPoint.thenApplyAsync((out) -> {
                        if (out == true) {
                            boolean result = action.cancel();
                            tasks.remove(mapActionTask.get(action));
                            mapActionTask.remove(action);
                            postRun.run();
                            return result;
                        } else {
                            return false;
                        }
                    }));
                } else {
                    // Not done yet! - let's remove right now from the task list
                    mapActionTask.get(action).cancel(true);
                    tasks.remove(mapActionTask.get(action));
                    mapActionTask.remove(action);
                    postRun.run();
                }
            } else if (action instanceof CreateSlice) {
                waitForEndOfTasks();
                action.cancel();
            } else {
                System.err.println("Unregistered action");
            }
        }
    }

    void computeLabelImage(AffineTransform3D at3D, String naming) {
        labelImageBeingComputed = true;

        System.out.println("Compute Label Image");

        final int[] cellDimensions = new int[]{32, 32, 1};

        // Cached Image Factory Options
        final DiskCachedCellImgOptions factoryOptions = options()
                .cellDimensions(cellDimensions)
                .cacheType(DiskCachedCellImgOptions.CacheType.BOUNDED)
                .maxCacheSize(1);

        // Creates cached image factory of Type UnsignedShort
        final DiskCachedCellImgFactory<UnsignedShortType> factory = new DiskCachedCellImgFactory<>(new UnsignedShortType(), factoryOptions);

        System.out.println("0");
        // 0 - slicing model : empty source but properly defined in space and resolution
        SourceAndConverter singleSliceModel = new EmptySourceAndConverterCreator("SlicingModel", at3D,
                mp.nPixX,
                mp.nPixY,
                1,
                factory
        ).get();

        SourceResampler resampler = new SourceResampler(null,
                singleSliceModel, false, false, false
        );

        AffineTransform3D translateZ = new AffineTransform3D();
        translateZ.translate(0, 0, -slicingAxisPosition);


        SourceAndConverter sac =
                mp.reslicedAtlas.nonExtendedSlicedSources[mp.reslicedAtlas.nonExtendedSlicedSources.length-1]; // By convention the label image is the last one

        sac = resampler.apply(sac);
        sac = SourceTransformHelper.createNewTransformedSourceAndConverter(translateZ, new SourceAndConverterAndTimeRange(sac, 0));

        ExportToImagePlusCommand export = new ExportToImagePlusCommand();

        export.level=0;
        export.timepointBegin=0;
        export.timepointEnd=0;
        export.sacs = new SourceAndConverter[1];
        export.sacs[0] = sac;
        export.run();

        impLabelImage = export.imp_out;
        //impLabelImage.show();

        ConstructROIsFromImgLabel labelToROIs = new ConstructROIsFromImgLabel();
        labelToROIs.atlas = mp.biopAtlas;
        labelToROIs.labelImg = impLabelImage;
        labelToROIs.smoothen = false;
        labelToROIs.run();
        cvtRoisOrigin = labelToROIs.cr_out;

        at3DLastLabelImage = at3D;
        labelImageBeingComputed = false;
    }

    void prepareExport(String namingChoice) {
        // Need to raster the label image
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.translate(-mp.nPixX / 2.0, -mp.nPixY / 2.0, 0);
        at3D.scale(mp.sizePixX, mp.sizePixY, mp.sizePixZ);
        at3D.translate(0, 0, slicingAxisPosition);

        boolean computeLabelImageNecessary = true;

        if (!labelImageBeingComputed) {
            if (at3DLastLabelImage != null) {
                if (Arrays.equals(at3D.getRowPackedCopy(), at3DLastLabelImage.getRowPackedCopy())) {
                    computeLabelImageNecessary = false;
                }
            }
        }

        if (computeLabelImageNecessary) {
            computeLabelImage(at3D, namingChoice);
        } else {
            while (labelImageBeingComputed) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {

                }
            }
        }

        computeTransformedRois();

        // Renaming
        IJShapeRoiArray roiList = (IJShapeRoiArray) cvtRoisTransformed.to(IJShapeRoiArray.class);
        for (int i=0;i<roiList.rois.size();i++) {
            CompositeFloatPoly roi = roiList.rois.get(i);
            int atlasId = Integer.valueOf(roi.name );
            // Issue with modulo 65000!!
            if (mp.biopAtlas.ontology instanceof AllenOntology) {
                atlasId = atlasId % 65000;
            }
            String name = mp.biopAtlas.ontology.getProperties(atlasId).get(namingChoice);
            roi.name = name;
        }

    }

    public synchronized void exportRegionsToROIManager(String namingChoice) {
        prepareExport(namingChoice);
        cvtRoisTransformed.to(RoiManager.class);
    }

    public synchronized void exportToQuPathProject(boolean erasePreviousFile) {
        prepareExport("id");

        ImageJRoisFile ijroisfile = (ImageJRoisFile) cvtRoisTransformed.to(ImageJRoisFile.class);

        storeInQuPathProjectIfExists(ijroisfile, erasePreviousFile);
    }

    public synchronized void exportRegionsToFile(String namingChoice, File dirOutput, boolean erasePreviousFile) {

        prepareExport(namingChoice);

        ImageJRoisFile ijroisfile = (ImageJRoisFile) cvtRoisTransformed.to(ImageJRoisFile.class);

        //--------------------

        File f = new File(dirOutput, toString()+".zip");
        try {

            if (f.exists()) {
                if (erasePreviousFile) {
                    Files.delete(Paths.get(f.getAbsolutePath()));

                    // Save in user specified folder
                    Files.copy(Paths.get(ijroisfile.f.getAbsolutePath()),Paths.get(f.getAbsolutePath()));
                } else {
                    System.err.println("ROI File already exists!");
                }
            } else {
                // Save in user specified folder
                Files.copy(Paths.get(ijroisfile.f.getAbsolutePath()),Paths.get(f.getAbsolutePath()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    void storeInQuPathProjectIfExists(ImageJRoisFile ijroisfile, boolean erasePreviousFile) {
        //-------------------- Experimental : finding where to save the ROIS
        SourceAndConverter rootSac = SourceAndConverterInspector.getRootSourceAndConverter(original_sacs[0]);

        if (SourceAndConverterServices.getSourceAndConverterService()
                .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)==null) {

            System.out.println("Slice not associated to any dataset");

        } else {
            AbstractSpimData asd =
                    ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                            .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).asd;

            System.out.println("BDV Datasets found associated with the slice!");

            if (SourceAndConverterServices
                    .getSourceAndConverterService()
                    .containsMetadata(asd, QUPATH_LINKED_PROJECT)) {
                System.out.println("Linked QuPath Project found!");

                int viewSetupId = ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                        .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).setupId;

                BasicViewSetup bvs = (BasicViewSetup) asd.getSequenceDescription().getViewSetups().get(viewSetupId);

                if (bvs.getAttribute(QuPathEntryEntity.class)!=null) {
                    QuPathEntryEntity qpent = bvs.getAttribute(QuPathEntryEntity.class);

                    MinimalQuPathProject qpProj = (MinimalQuPathProject) SourceAndConverterServices
                            .getSourceAndConverterService()
                            .getMetadata(asd, QUPATH_LINKED_PROJECT);

                    String filePath = Paths.get(qpProj.uri).getParent().toString();

                    // under filePath, there should be a folder data/#entryID

                    File dataEntryFolder = new File(filePath, "data"+File.separator+Integer.toString(qpent.getId()) );

                    // TODO : check if the file already exists...
                    if (dataEntryFolder.exists()) {

                        File f = new File(dataEntryFolder, "ABBA-RoiSet.zip");
                        System.out.println("attempt save QuPath" + f.getAbsolutePath());
                        try {

                            if (f.exists()) {
                                if (erasePreviousFile) {
                                    Files.delete(Paths.get(f.getAbsolutePath()));
                                    // Save in user specified folder
                                    Files.copy(Paths.get(ijroisfile.f.getAbsolutePath()),Paths.get(f.getAbsolutePath()));
                                    writeOntotogyIfNotPresent(filePath);
                                } else {
                                    System.err.println("Error : QuPath ROI file already exists");
                                }
                            } else {
                                // Save in user specified folder
                                Files.copy(Paths.get(ijroisfile.f.getAbsolutePath()),Paths.get(f.getAbsolutePath()));
                                writeOntotogyIfNotPresent(filePath);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    } else {
                        System.err.println("QuPath Entry data folder ["+dataEntryFolder.toString() +"] not found! : ");
                    }

                } else {
                    System.err.println("QuPathEntryEntity not found");
                }


            } else {
                System.err.println("No QuPath project found");
            }
        }
    }

    static public synchronized void writeOntotogyIfNotPresent(String quPathFilePath) {
        File ontology = new File(quPathFilePath, "AllenMouseBrainOntology.json");
       /* if (!ontology.exists()) {
            // Write ontology once
            Gson gson = new Gson();
            AllenOntologyJson onto = AllenOntologyJson.getOntologyFromFile(new File("src/main/resources/AllenBrainData/1.json"));
            try {
                gson.toJson(onto, new FileWriter(quPathFilePath));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }*/
    }

    public String toString() {
        int index = mp.getSortedSlices().indexOf(this);
        return "Slice_"+index;
    }

    private void computeTransformedRois() {
        cvtRoisTransformed = new ConvertibleRois();

        IJShapeRoiArray arrayIni = (IJShapeRoiArray) cvtRoisOrigin.to(IJShapeRoiArray.class);
        cvtRoisTransformed.set(arrayIni);
        RealPointList list = ((RealPointList) cvtRoisTransformed.to(RealPointList.class));
        // Perform reverse transformation, in the reverse order:
        //  - From atlas coordinates -> image coordinates

        AffineTransform3D at3D = new AffineTransform3D();
        at3D.translate(-mp.nPixX / 2.0, -mp.nPixY / 2.0, 0);
        at3D.scale(mp.sizePixX, mp.sizePixY, mp.sizePixZ);
        at3D.translate(0, 0, slicingAxisPosition);
        list = getTransformedPtsFixedToMoving(list, at3D.inverse());

        Collections.reverse(this.registrations);

        for (Registration reg : this.registrations) {
            System.out.println("Registration class "+reg.getClass().getSimpleName());
            list = reg.getTransformedPtsFixedToMoving(list);
        }
        Collections.reverse(this.registrations);

        this.original_sacs[0].getSpimSource().getSourceTransform(0,0,at3D);
        list = getTransformedPtsFixedToMoving(list, at3D);

        cvtRoisTransformed.clear();
        list.shapeRoiList = new IJShapeRoiArray(arrayIni);

        /*for (int i = 0;i<arrayIni.rois.size();i++) {
            list.shapeRoiList.rois.get(i).getRoi().setProperty("hierarchy", arrayIni.rois.get(i).getRoi().getProperty("hierarchy"));

            System.out.println("------------");
            System.out.println(arrayIni.rois.get(i).getRoi().getProperty("hierarchy"));

        }*/

        cvtRoisTransformed.set(list);
    }

    public RealPointList getTransformedPtsFixedToMoving(RealPointList pts, AffineTransform3D at3d) {
        ArrayList<RealPoint> cvtList = new ArrayList<>();
        for (RealPoint p : pts.ptList) {
            RealPoint pt3d = new RealPoint(3);
            pt3d.setPosition(new double[]{p.getDoublePosition(0), p.getDoublePosition(1),0});
            at3d.inverse().apply(pt3d, pt3d);
            RealPoint cpt = new RealPoint(pt3d.getDoublePosition(0), pt3d.getDoublePosition(1));
            cvtList.add(cpt);
        }
        return new RealPointList(cvtList);
    }

    public void hide() {
        this.deSelect();
        if (mp.currentMode == MultiSlicePositioner.REGISTRATION_MODE_INT) {
            mp.getBdvh().getViewerPanel().state()
                    .setSourcesActive(Arrays.asList(registered_sacs), false);
        }

        if (mp.currentMode == MultiSlicePositioner.POSITIONING_MODE_INT) {
            mp.getBdvh().getViewerPanel().state()
                    .setSourcesActive(Arrays.asList(relocated_sacs_positioning_mode), false);
        }
    }
}