package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.allen.AllenOntology;
import ch.epfl.biop.atlas.commands.ConstructROIsFromImgLabel;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.types.CompositeFloatPoly;
import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import ch.epfl.biop.java.utilities.roi.types.ImageJRoisFile;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.spimdata.qupath.QuPathEntryEntity;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.sourceandconverter.affine.AffineTransformedSourceWrapperRegistration;
import ch.epfl.biop.registration.sourceandconverter.affine.CenterZeroRegistration;
import ch.epfl.biop.scijava.command.ExportToImagePlusCommand;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterInspector;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static ch.epfl.biop.atlas.aligner.CancelableAction.errlog;

/**
 * Class which contains the current registered SourceAndConverter array
 *
 * Each element of the array is a channel
 *
 * This class should be UI independent (no show / bdvhandle, etc)
 *
 */

public class SliceSources {

    final private SliceSourcesGUIState guiState; // in here ? GOod idea ?

    // What are they ?
    final SourceAndConverter<?>[] original_sacs;

    // Used for registration : like 3D, but tilt and roll ignored because it is handled on the fixed source side
    private SourceAndConverter<?>[] registered_sacs;

    private List<RegistrationAndSources> registered_sacs_sequence = new ArrayList<>();

    // Where is the slice located along the slicing axis
    private double slicingAxisPosition;

    private boolean isSelected = false;

    //private double yShift_slicing_mode = 0;

    private final MultiSlicePositioner mp;

    private AffineTransformedSourceWrapperRegistration zPositioner;

    private AffineTransformedSourceWrapperRegistration preTransform;

    private CenterZeroRegistration centerPositioner;

    private ImagePlus impLabelImage;

    private AffineTransform3D at3DLastLabelImage;

    private boolean labelImageBeingComputed = false;

    private ConvertibleRois cvtRoisOrigin;

    private ConvertibleRois cvtRoisTransformed;

    private ConvertibleRois leftRightTranformed;

    private List<Registration<SourceAndConverter<?>[]>> registrations = new ArrayList<>();

    private final List<CompletableFuture<Boolean>> tasks = new ArrayList<>();

    private Map<CancelableAction, CompletableFuture<Boolean>> mapActionTask = new HashMap<>();

    private volatile CancelableAction actionInProgress = null;

    private ConvertibleRois leftRightOrigin = new ConvertibleRois();

    private int currentSliceIndex = -1;

    // For fast display : Icon TODO : see https://github.com/bigdataviewer/bigdataviewer-core/blob/17d2f55d46213d1e2369ad7ef4464e3efecbd70a/src/main/java/bdv/tools/RecordMovieDialog.java#L256-L318
    protected SliceSources(SourceAndConverter<?>[] sacs, double slicingAxisPosition, MultiSlicePositioner mp) {
        this.mp = mp;
        this.original_sacs = sacs;
        this.slicingAxisPosition = slicingAxisPosition;
        this.registered_sacs = this.original_sacs;

        centerPositioner = new CenterZeroRegistration();
        centerPositioner.setMovingImage(registered_sacs);

        zPositioner = new AffineTransformedSourceWrapperRegistration();
        preTransform = new AffineTransformedSourceWrapperRegistration();

        guiState = new SliceSourcesGUIState(this, mp);

        runRegistration(centerPositioner, Function.identity(), Function.identity());
        runRegistration(preTransform, Function.identity(), Function.identity());
        runRegistration(zPositioner, Function.identity(), Function.identity());
        waitForEndOfTasks();
        updateZPosition();
        guiState.positionChanged();
    }

    public SliceSourcesGUIState getGUIState() {
        return guiState;
    }

    public synchronized SourceAndConverter<?>[] getRegisteredSources() {
        return registered_sacs;
    }

    protected double getSlicingAxisPosition() {
        return slicingAxisPosition;
    }

    protected void setSlicingAxisPosition(double newSlicingAxisPosition) {
        slicingAxisPosition = newSlicingAxisPosition;
        updateZPosition();
        guiState.positionChanged();
    }

    public SourceAndConverter<?>[] getOriginalSources() {
        return original_sacs;
    }

    public synchronized void select() {
        this.isSelected = true;
        //guiState.select();
    }

    public synchronized void deSelect() {
        this.isSelected = false;
        //guiState.deselect();
    } // TODO : thread lock!

    public boolean isSelected() {
        return this.isSelected;
    }

    public int getIndex() {
        return currentSliceIndex;
    }

    private void updateZPosition() {
        AffineTransform3D zShiftAffineTransform = new AffineTransform3D();
        zShiftAffineTransform.translate(0, 0, slicingAxisPosition);
        zPositioner.setAffineTransform(zShiftAffineTransform); // Moves the registered slices to the correct position
    }

    protected void setIndex(int idx) {
        currentSliceIndex = idx;
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

    protected boolean exactMatch(List<SourceAndConverter<?>> testSacs) {
        Set<SourceAndConverter<?>> originalSacsSet = new HashSet<>(Arrays.asList(original_sacs));
        return (originalSacsSet.containsAll(testSacs) && testSacs.containsAll(originalSacsSet));
    }

    protected boolean isContainingAny(Collection<SourceAndConverter<?>> sacs) {
        Set originalSacsSet = new HashSet();
        for (SourceAndConverter sac : original_sacs) {
            originalSacsSet.add(sac);
        }
        return (sacs.stream().distinct().anyMatch(originalSacsSet::contains));
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

    public void transformSourceOrigin(AffineTransform3D at3D) {
        preTransform.setAffineTransform(at3D);
    }

    public AffineTransform3D getTransformSourceOrigin() {
        return preTransform.getAffineTransform();
    }

    public void rotateSourceOrigin(int axis, double angle) {
        AffineTransform3D at3d = preTransform.getAffineTransform();
        at3d.rotate(axis, angle);
        preTransform.setAffineTransform(at3d);
    }

    public void appendRegistration(Registration<SourceAndConverter<?>[]> reg) {

        registered_sacs = reg.getTransformedImageMovingToFixed(registered_sacs);

        registered_sacs_sequence.add(new RegistrationAndSources(reg, registered_sacs));

        registrations.add(reg);

        mp.mso.updateInfoPanel(this);

        guiState.sourcesChanged();

    }

    // public : enqueueRegistration
    private boolean performRegistration(Registration<SourceAndConverter<?>[]> reg,
                                       Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed,
                                       Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving) {

        reg.setFixedImage(preprocessFixed.apply(mp.reslicedAtlas.nonExtendedSlicedSources));
        reg.setMovingImage(preprocessMoving.apply(registered_sacs));
        boolean out = reg.register();
        if (!out) {
            errlog.accept("Issue during registration of class "+reg.getClass().getSimpleName());
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
    protected void runRegistration(Registration<SourceAndConverter<?>[]> reg,
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

                registered_sacs_sequence.remove(registered_sacs_sequence.get(registered_sacs_sequence.size()-1));

                registered_sacs = registered_sacs_sequence.get(registered_sacs_sequence.size()-1).sacs;

                guiState.sourcesChanged();

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
                if (out) {
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
                        if (out) {
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
                mp.errlog.accept("Unregistered action");
            }
        }
    }

    void computeLabelImage(AffineTransform3D at3D, String naming) {
        labelImageBeingComputed = true;

        // 0 - slicing model : empty source but properly defined in space and resolution
        SourceAndConverter singleSliceModel = new EmptySourceAndConverterCreator("SlicingModel", at3D,
                mp.nPixX,
                mp.nPixY,
                1
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

        ConstructROIsFromImgLabel labelToROIs = new ConstructROIsFromImgLabel();
        labelToROIs.atlas = mp.biopAtlas;
        labelToROIs.labelImg = impLabelImage;
        labelToROIs.smoothen = false;
        labelToROIs.run();
        cvtRoisOrigin = labelToROIs.cr_out;

        at3DLastLabelImage = at3D;
        labelImageBeingComputed = false;

        // Now Left Right:
        sac = mp.reslicedAtlas.nonExtendedSlicedSources[mp.reslicedAtlas.nonExtendedSlicedSources.length-2]; // Don't know why this is working

        sac = resampler.apply(sac);
        sac = SourceTransformHelper.createNewTransformedSourceAndConverter(translateZ, new SourceAndConverterAndTimeRange(sac, 0));

        export = new ExportToImagePlusCommand();

        export.level=0;
        export.timepointBegin=0;
        export.timepointEnd=0;
        export.sacs = new SourceAndConverter[1];
        export.sacs[0] = sac;
        export.run();

        ImagePlus leftRightImage = export.imp_out;

        leftRightOrigin.set(leftRightImage);

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
            roi.color = mp.biopAtlas.ontology.getColor(atlasId);
        }

        IJShapeRoiArray roiArray = (IJShapeRoiArray) leftRightTranformed.to(IJShapeRoiArray.class);

        Roi left = roiArray.rois.get(0).getRoi();
        left.setStrokeColor(new Color(0,255,0));
        left.setName("Left");
        roiList.rois.add(new CompositeFloatPoly(left));

        Roi right = roiArray.rois.get(1).getRoi();
        right.setStrokeColor(new Color(255,0,255));
        right.setName("Right");
        roiList.rois.add(new CompositeFloatPoly(right));

    }

    protected synchronized void exportRegionsToROIManager(String namingChoice) {
        prepareExport(namingChoice);
        cvtRoisTransformed.to(RoiManager.class);
    }

    protected synchronized void exportToQuPathProject(boolean erasePreviousFile) {
        prepareExport("id");

        ImageJRoisFile ijroisfile = (ImageJRoisFile) cvtRoisTransformed.to(ImageJRoisFile.class);

        storeInQuPathProjectIfExists(ijroisfile, erasePreviousFile);
    }

    protected synchronized void exportRegionsToFile(String namingChoice, File dirOutput, boolean erasePreviousFile) {

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

    private void storeInQuPathProjectIfExists(ImageJRoisFile ijroisfile, boolean erasePreviousFile) {
        //-------------------- Experimental : finding where to save the ROIS
        SourceAndConverter rootSac = SourceAndConverterInspector.getRootSourceAndConverter(original_sacs[0]);

        if (SourceAndConverterServices.getSourceAndConverterService()
                .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)==null) {

            System.out.println("Slice not associated to any dataset");

        } else {
            AbstractSpimData asd =
                    ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                            .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).asd;

            mp.log.accept("BDV Datasets found associated with the slice!");


            int viewSetupId = ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                    .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).setupId;

            BasicViewSetup bvs = (BasicViewSetup) asd.getSequenceDescription().getViewSetups().get(viewSetupId);

            if (bvs.getAttribute(QuPathEntryEntity.class)!=null) {
                QuPathEntryEntity qpent = bvs.getAttribute(QuPathEntryEntity.class);

                String filePath = qpent.getQuPathProjectionLocation();

                // under filePath, there should be a folder data/#entryID

                File dataEntryFolder = new File(filePath, "data"+File.separator+qpent.getId());

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
                    mp.errlog.accept("QuPath Entry data folder ["+dataEntryFolder.toString() +"] not found! : ");
                }

            } else {
                mp.errlog.accept("QuPathEntryEntity not found");
            }
        }
    }

    // TODO
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

        leftRightTranformed = new ConvertibleRois();

        IJShapeRoiArray arrayIniRegions = (IJShapeRoiArray) cvtRoisOrigin.to(IJShapeRoiArray.class);
        cvtRoisTransformed.set(arrayIniRegions);
        RealPointList listRegions = ((RealPointList) cvtRoisTransformed.to(RealPointList.class));

        IJShapeRoiArray arrayIniLeftRight = (IJShapeRoiArray) leftRightOrigin.to(IJShapeRoiArray.class);
        leftRightTranformed.set(arrayIniLeftRight);
        RealPointList listLeftRight = ((RealPointList) leftRightOrigin.to(RealPointList.class));

        // Perform reverse transformation, in the reverse order:
        //  - From atlas coordinates -> image coordinates

        AffineTransform3D at3D = new AffineTransform3D();
        at3D.translate(-mp.nPixX / 2.0, -mp.nPixY / 2.0, 0);
        at3D.scale(mp.sizePixX, mp.sizePixY, mp.sizePixZ);
        at3D.translate(0, 0, slicingAxisPosition);
        listRegions = getTransformedPtsFixedToMoving(listRegions, at3D.inverse());

        listLeftRight = getTransformedPtsFixedToMoving(listLeftRight, at3D.inverse());

        Collections.reverse(this.registrations);

        for (Registration reg : this.registrations) {
            listRegions = reg.getTransformedPtsFixedToMoving(listRegions);
            listLeftRight = reg.getTransformedPtsFixedToMoving(listLeftRight);
        }
        
        Collections.reverse(this.registrations);

        this.original_sacs[0].getSpimSource().getSourceTransform(0,0,at3D);
        listRegions = getTransformedPtsFixedToMoving(listRegions, at3D);

        listLeftRight = getTransformedPtsFixedToMoving(listLeftRight, at3D);

        cvtRoisTransformed.clear();
        listRegions.shapeRoiList = new IJShapeRoiArray(arrayIniRegions);

        leftRightTranformed.clear();
        listLeftRight.shapeRoiList = new IJShapeRoiArray(arrayIniLeftRight);

        cvtRoisTransformed.set(listRegions);


        leftRightTranformed.set(listLeftRight);
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

    protected void editLastRegistration(
        Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed,
        Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving) {
        Registration reg = this.registrations.get(registrations.size() - 1);
        if (reg.isEditable()) {
            mp.log.accept("Edition will begin when the manual lock is acquired");
            synchronized (MultiSlicePositioner.manualActionLock) {
                this.removeRegistration(reg);
                // preprocessFixed has an issue...
                reg.setFixedImage(preprocessFixed.apply(mp.reslicedAtlas.nonExtendedSlicedSources)); // No filtering -> all channels
                reg.setMovingImage(preprocessMoving.apply(registered_sacs)); // NO filtering -> all channels
                reg.edit();
                this.appendRegistration(reg);
            }
        } else {
            mp.log.accept("The last registration of class "+reg.getClass().getSimpleName()+" is not editable.");
        }
    }

    // TODO!!!!!!!!
    public int getAdaptedMipMapLevel(double pxSizeInMm) {
        return registered_sacs[0].getSpimSource().getNumMipmapLevels() - 1;
    }

    public static class RegistrationAndSources {

        final Registration reg;
        final SourceAndConverter[] sacs;

        public RegistrationAndSources(Registration reg, SourceAndConverter[] sacs) {
            this.reg = reg;
            this.sacs = sacs;
        }
    }
}