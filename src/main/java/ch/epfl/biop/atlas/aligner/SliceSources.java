package ch.epfl.biop.atlas.aligner;

import bdv.util.BoundedRealTransform;
import bdv.util.DefaultInterpolators;
import bdv.util.QuPathBdvHelper;
import bdv.util.source.alpha.AlphaSourceHelper;
import bdv.util.source.alpha.AlphaSourceRAI;
import bdv.util.source.alpha.IAlphaSource;
import bdv.viewer.Interpolation;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.plugin.RegistrationPluginHelper;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.command.AllenBrainAdultMouseAtlasCCF2017Command;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import ch.epfl.biop.bdv.img.entity.ImageName;
import ch.epfl.biop.bdv.img.imageplus.ImagePlusHelper;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.SelectToROIKeepLines;
import ch.epfl.biop.java.utilities.roi.types.CompositeFloatPoly;
import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import ch.epfl.biop.java.utilities.roi.types.ImageJRoisFile;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.sourceandconverter.affine.AffineTransformedSourceWrapperRegistration;
import ch.epfl.biop.registration.sourceandconverter.affine.CenterZeroRegistration;
import ch.epfl.biop.registration.sourceandconverter.spline.RealTransformSourceAndConverterRegistration;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesIdentity;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessComposer;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import com.google.gson.Gson;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.FinalVoxelDimensions;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.InvertibleRealTransformSequence;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSequence;
import net.imglib2.realtransform.Wrapped2DTransformAs3D;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterInspector;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.importer.EmptySourceAndConverterCreator;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;
import sc.fiji.persist.ScijavaGsonHelper;
import spimdata.util.Displaysettings;
//import spimdata.imageplus.ImagePlusHelper;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


/**
 * Class which contains the current registered SourceAndConverter array
 *
 * Each element of the array is a channel
 *
 * This class should be UI independent (no show / bdvhandle, etc)
 *
 */

public class SliceSources {

    protected static Logger logger = LoggerFactory.getLogger(SliceSources.class);

    //final private SliceSourcesGUIState guiState; // in here ? GOod idea ?

    // What are they ?
    final SourceAndConverter<?>[] original_sacs;

    public final int nChannels;

    // Used for registration : like 3D, but tilt and roll ignored because it is handled on the fixed source side
    private SourceAndConverter<?>[] registered_sacs;

    private final List<RegistrationAndSources> registered_sacs_sequence = new ArrayList<>();

    // Where is the slice located along the slicing axis
    private double slicingAxisPosition;

    private boolean isSelected = false;

    private final MultiSlicePositioner mp;

    private final AffineTransformedSourceWrapperRegistration zPositioner;

    private final AffineTransformedSourceWrapperRegistration preTransform;

    private final CenterZeroRegistration centerPositioner;

    private ImagePlus impLabelImage;

    private AffineTransform3D at3DLastLabelImage;

    private boolean labelImageBeingComputed = false;

    private ConvertibleRois cvtRoisOrigin;

    private ConvertibleRois cvtRoisTransformed;

    private ConvertibleRois leftRightTranformed;

    private final List<Registration<SourceAndConverter<?>[]>> registrations = new ArrayList<>();

    private final List<CompletableFuture<Boolean>> tasks = new ArrayList<>();

    private final Map<CancelableAction, CompletableFuture<Boolean>> mapActionTask = new ConcurrentHashMap<>();

    private volatile CancelableAction actionInProgress = null;

    private final ConvertibleRois leftRightOrigin = new ConvertibleRois();

    private int currentSliceIndex = -1;

    protected String name = "";

    final IAlphaSource alphaSource;

    protected final DefaultInterpolators< FloatType > interpolators = new DefaultInterpolators<>();

    // For fast display : Icon TODO : see https://github.com/bigdataviewer/bigdataviewer-core/blob/17d2f55d46213d1e2369ad7ef4464e3efecbd70a/src/main/java/bdv/tools/RecordMovieDialog.java#L256-L318
    protected SliceSources(SourceAndConverter<?>[] sacs, double slicingAxisPosition, MultiSlicePositioner mp, double thicknessCorrection, double zShiftCorrection) {

        this.zThicknessCorrection = thicknessCorrection;
        this.zShiftCorrection = zShiftCorrection;

        nChannels = sacs.length;

        this.mp = mp;
        this.original_sacs = sacs;
        this.slicingAxisPosition = slicingAxisPosition;
        this.registered_sacs = this.original_sacs;

        centerPositioner = new CenterZeroRegistration();
        centerPositioner.setMovingImage(registered_sacs);

        zPositioner = new AffineTransformedSourceWrapperRegistration();
        preTransform = new AffineTransformedSourceWrapperRegistration();

        String unit = original_sacs[0].getSpimSource().getVoxelDimensions().unit();
        double voxX = original_sacs[0].getSpimSource().getVoxelDimensions().dimension(0);
        double voxY = original_sacs[0].getSpimSource().getVoxelDimensions().dimension(0);
        double voxZ = original_sacs[0].getSpimSource().getVoxelDimensions().dimension(0);
        FinalVoxelDimensions voxD = new FinalVoxelDimensions(unit, voxX, voxY, voxZ);
        FinalInterval interval = new FinalInterval(mp.nPixX,mp.nPixY,1);

        alphaSource = new IAlphaSource() {
            @Override
            public boolean doBoundingBoxCulling()
            {
                return false;
            }

            @Override
            public boolean intersectBox(AffineTransform3D affineTransform, Interval cell, int timepoint) {
                // Let's try a simplebox computation and see if there are intersections.
                AlphaSourceRAI.Box3D box_cell = new AlphaSourceRAI.Box3D(affineTransform, cell);
                AffineTransform3D affineTransform3D = new AffineTransform3D();
                getSourceTransform(timepoint,0,affineTransform3D);
                AlphaSourceRAI.Box3D box_this = new AlphaSourceRAI.Box3D(affineTransform3D, this.getSource(timepoint,0));
                return box_this.intersects(box_cell);
            }

            @Override
            public boolean isPresent(int t) {
                return t==0;
            }

            @Override
            public RandomAccessibleInterval<FloatType> getSource(int t, int level) {
                final RandomAccessible< FloatType > randomAccessible =
                        new FunctionRandomAccessible<>( 3, () -> (loc, out) -> out.setReal( 1f ), FloatType::new );
                return Views.interval(randomAccessible, interval);
            }

            @Override
            public RealRandomAccessible<FloatType> getInterpolatedSource(int t, int level, Interpolation interpolation) {
                ExtendedRandomAccessibleInterval<FloatType, RandomAccessibleInterval< FloatType >>
                        eView = Views.extendZero(getSource( t, level ));
                RealRandomAccessible< FloatType > realRandomAccessible = Views.interpolate( eView, interpolators.get(Interpolation.NEARESTNEIGHBOR) );
                return realRandomAccessible;
            }

            @Override
            public void getSourceTransform(int t, int level, AffineTransform3D affineTransform3D) {
                affineTransform3D.identity();
                affineTransform3D.scale(mp.sizePixX, mp.sizePixY, thicknessInMm);
                affineTransform3D.translate(-mp.sX / 2.0, -mp.sY / 2.0, getSlicingAxisPosition()+getZShiftCorrection());
            }

            @Override
            public FloatType getType() {
                return new FloatType();
            }

            @Override
            public String getName() {
                return "alpha-slice";
            }

            @Override
            public VoxelDimensions getVoxelDimensions() {
                return voxD;
            }

            @Override
            public int getNumMipmapLevels() {
                return 1;
            }
        };

        runRegistration(centerPositioner, new SourcesIdentity(), new SourcesIdentity());
        runRegistration(preTransform, new SourcesIdentity(), new SourcesIdentity());
        runRegistration(zPositioner, new SourcesIdentity(), new SourcesIdentity());
        waitForEndOfTasks();
        updateZPosition();
        //mp.positionZChanged(slice);
        positionChanged();

        computeZThickness();

        try {


            SourceAndConverter rootSac = SourceAndConverterInspector.getRootSourceAndConverter(original_sacs[0]);
            if (SourceAndConverterServices.getSourceAndConverterService()
                    .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)==null) {
                // Not linked to a spimdata
                name =rootSac.getSpimSource().getName();
            } else {
                AbstractSpimData asd =
                        ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                                .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).asd;

                int viewSetupId = ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                        .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).setupId;

                BasicViewSetup bvs = (BasicViewSetup) asd.getSequenceDescription().getViewSetups().get(viewSetupId);

                // TODO : keep in sync name with QuPath -> for that, read imagename through opener

                if (bvs.getAttribute(ImageName.class)!=null) {
                    name = bvs.getAttribute(ImageName.class).getName();
                } else {
                    name =rootSac.getSpimSource().getName();
                }

            }


            /*
            if (SourceAndConverterServices.getSourceAndConverterService()
                    .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)==null) {
                //sliceInfo+="No information available";
            } else {
                AbstractSpimData asd =
                        ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                                .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).asd;

                int viewSetupId = ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                        .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).setupId;

                Collection<String> datasetKeys = SourceAndConverterServices.getSourceAndConverterService().getMetadataKeys(asd);

                if (datasetKeys!=null) {
                    StringBuilder sb = new StringBuilder();
                    datasetKeys.forEach(key -> {
                        String value = "";
                        Object v = SourceAndConverterServices.getSourceAndConverterService().getMetadata(asd,key);
                        if (v!=null) value = v.toString();
                        sb.append(key+":"+value+"\n");
                    });
                    sliceInfo+=sb.toString();
                }

                BasicViewSetup bvs = (BasicViewSetup) asd.getSequenceDescription().getViewSetups().get(viewSetupId);

                if (bvs.hasName()) {
                    sliceInfo+="viewsetup:"+bvs.getName()+" ["+bvs.getId()+"]\n";
                }

                if (QuPathBdvHelper.isSourceLinkedToQuPath(original_sacs[0])) {
                    int entryId = QuPathBdvHelper.getEntryId(original_sacs[0]);
                    sliceInfo+="Project: "+QuPathBdvHelper.getProjectFile(this.original_sacs[0]).getAbsolutePath()+"\n";
                    sliceInfo+="- Entry Id: "+entryId;//qpent.getName()+" ["+qpent.getId()+"]";
                }*/



        } catch(Exception e) {
            mp.errlog.accept("Couldn't name slice");
            e.printStackTrace();
        }


    }

    private void positionChanged() {
        // TODO
    }

    protected double zThicknessCorrection;

    protected double zShiftCorrection;

    public double getZThicknessCorrection() {
        return zThicknessCorrection;
    }

    public double getZShiftCorrection() {
        return zShiftCorrection;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public SourceAndConverter<?>[] getRegisteredSources() {
        return registered_sacs;
    }

    public double getSlicingAxisPosition() {
        return slicingAxisPosition;
    }

    protected void setSlicingAxisPosition(double newSlicingAxisPosition) {
        slicingAxisPosition = newSlicingAxisPosition;
        updateZPosition();
    }

    public void setSliceThickness(double zBeginInMm, double zEndInMm) {
        if ((this.slicingAxisPosition<zBeginInMm)||(this.slicingAxisPosition>zEndInMm)) {
            mp.errlog.accept("Wrong slice position. Cannot set slice bounds");
            return;
        }
        if (zBeginInMm>zEndInMm) {
            mp.errlog.accept("z(End) inferior to z(Begin). Cannot set slice bounds");
            return;
        }
        setSliceThickness(zEndInMm-zBeginInMm);
        zShiftCorrection = ((zEndInMm+zBeginInMm) / 2) - slicingAxisPosition;
        updateZPosition();
    }

    public void setSliceThickness(double sizeInMm) {

        RealPoint pt1 = new RealPoint(3);
        RealPoint pt2 = new RealPoint(3);

        // adjustement based on the first channel (indexed 0) of the pretransformed image
        SourceAndConverter sourceUsedForInitialMeasurement = registered_sacs_sequence.get(1).sacs[0]; // 0 = center 1 = pretransform -> the one to take

        // this is more tricky than it appears ...
        // Let's compute the position in real space of the extreme opposite corners

        long[] dimensions = new long[3];
        sourceUsedForInitialMeasurement.getSpimSource().getSource(0,0).dimensions(dimensions);

        pt2.setPosition(dimensions);

        AffineTransform3D at3D = new AffineTransform3D();
        sourceUsedForInitialMeasurement.getSpimSource().getSourceTransform(0,0,at3D);

        at3D.apply(pt1,pt1);
        at3D.apply(pt2,pt2);

        double currentZSliceOccupancy = Math.abs(pt1.getDoublePosition(2)-pt2.getDoublePosition(2));

        if (currentZSliceOccupancy == 0) {
            mp.errlog.accept("Error : slice thickness is 0! Cannot set slice thickness");
            return;
        }

        zThicknessCorrection = sizeInMm/currentZSliceOccupancy;
        computeZThickness();
        updateZPosition();
    }

    public double getThicknessInMm() {
        return thicknessInMm;
    }

    double thicknessInMm;

    private void computeZThickness() {

        RealPoint pt1 = new RealPoint(3);
        RealPoint pt2 = new RealPoint(3);

        // adjustement based on the first channel (indexed 0) of the pretransformed image
        SourceAndConverter sourceUsedForInitialMeasurement = registered_sacs_sequence.get(1).sacs[0]; // 0 = center 1 = pretransform -> the one to take

        // this is more tricky than it appears ...
        // Let's compute the position in real space of the extreme opposite corners

        long[] dimensions = new long[3];
        sourceUsedForInitialMeasurement.getSpimSource().getSource(0,0).dimensions(dimensions);

        pt2.setPosition(dimensions);

        AffineTransform3D at3D = new AffineTransform3D();
        sourceUsedForInitialMeasurement.getSpimSource().getSourceTransform(0,0,at3D);

        at3D.apply(pt1,pt1);
        at3D.apply(pt2,pt2);

        double currentZSliceOccupancy = Math.abs(pt1.getDoublePosition(2)-pt2.getDoublePosition(2));

        thicknessInMm = zThicknessCorrection * currentZSliceOccupancy;

    }

    public SourceAndConverter<?>[] getOriginalSources() {
        return original_sacs;
    }

    public void select() {
        if (!this.isSelected) {
            this.isSelected = true;
            mp.sliceSelected(this);
        }
    }

    public void deSelect() {
        if (this.isSelected) {
            this.isSelected = false;
            mp.sliceDeselected(this);
        }
    } // TODO : thread lock!

    public boolean isSelected() {
        return this.isSelected;
    }

    public int getIndex() {
        return currentSliceIndex;
    }

    private void updateZPosition() {
        AffineTransform3D zShiftAffineTransform = new AffineTransform3D();
        zShiftAffineTransform.scale(1, 1, zThicknessCorrection);
        zShiftAffineTransform.translate(0, 0, slicingAxisPosition + zShiftCorrection);
        zPositioner.setAffineTransform(zShiftAffineTransform); // Moves the registered slices to the correct position
        si.updateBox();
        mp.positionZChanged(this);
    }

    protected void setIndex(int idx) {
        currentSliceIndex = idx;
    }

    public String getActionState(CancelableAction action) {
        if ((action!=null)&&(action == actionInProgress)) {
            return "(pending)";
        }
        if (mapActionTask.containsKey(action)) {
            if (tasks.contains(mapActionTask.get(action))) {
                CompletableFuture<Boolean> future = tasks.get(tasks.indexOf(mapActionTask.get(action)));
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
        Set<SourceAndConverter> originalSacsSet = new HashSet<>();
        for (SourceAndConverter sac : original_sacs) {
            originalSacsSet.add(sac);
        }
        return (sacs.stream().distinct().anyMatch(originalSacsSet::contains));
    }

    public void waitForEndOfTasks() {
        if (tasks.size()>0) {
            try {
                CompletableFuture<Boolean> lastTask = tasks.get(tasks.size()-1);
                lastTask.get();
            } catch (Exception e) {
                e.printStackTrace();
                mp.errlog.accept("Tasks were cancelled for slice "+this.toString()+" Error:"+e.getMessage());
            }
        }
    }

    public boolean waitForEndOfAction(CancelableAction action) {
        if (!mapActionTask.containsKey(action)) {
            logger.debug("(waitForEndOfAction) action "+action+" not found or unrelated to slice "+this);
            return false;
        } else {
            try {
                return mapActionTask.get(action).get();
            } catch (InterruptedException e) {
                logger.debug("(waitForEndOfAction) slice ["+this+"] interrupted action "+action+" "+e.getMessage());
                return false;
            } catch (ExecutionException e) {
                logger.debug("(waitForEndOfAction) slice [\"+this+\"] execution exception for action "+action+" "+e.getMessage());
                return false;
            }
        }
    }

    public void transformSourceOrigin(AffineTransform3D at3D) {
        preTransform.setAffineTransform(at3D);
        mp.slicePreTransformChanged(this);
    }

    public AffineTransform3D getTransformSourceOrigin() {
        return preTransform.getAffineTransform();
    }

    public void rotateSourceOrigin(int axis, double angle) {
        AffineTransform3D at3d = preTransform.getAffineTransform();
        at3d.rotate(axis, angle);
        transformSourceOrigin(at3d);
    }

    public int getNumberOfRegistrations() {
        return registrations.size()-3;
    }

    protected void appendRegistration(Registration<SourceAndConverter<?>[]> reg) {

        if (reg instanceof RealTransformSourceAndConverterRegistration) {
            RealTransformSourceAndConverterRegistration sreg = (RealTransformSourceAndConverterRegistration) reg;
            if (!(sreg.getRealTransform() instanceof BoundedRealTransform)) {
                BoundedRealTransform brt = new BoundedRealTransform((InvertibleRealTransform) sreg.getRealTransform(), si);
                si.updateBox();
                sreg.setRealTransform(brt);
            }
        }

        registered_sacs = reg.getTransformedImageMovingToFixed(registered_sacs);

        for (SourceAndConverter sac: registered_sacs) {
            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .register(sac);
            if (alphaSource!=null) {
                AlphaSourceHelper.setAlphaSource(sac, alphaSource);
            }
        }

        registered_sacs_sequence.add(new RegistrationAndSources(reg, registered_sacs));
        registrations.add(reg);

    }

    public void sourcesChanged() {
        // TODO : notify
        mp.notifySourcesChanged(this);
    }

    private boolean performRegistration(Registration<SourceAndConverter<?>[]> reg,
                                       SourcesProcessor preprocessFixed,
                                       SourcesProcessor preprocessMoving) {
        reg.setFixedImage(preprocessFixed.apply(mp.reslicedAtlas.nonExtendedSlicedSources));
        reg.setMovingImage(preprocessMoving.apply(registered_sacs));

        // For the mask : we set it as the label image, pre processed identically
        // 0 - remove channel select from pre processor
        SourcesProcessor fixedProcessor = SourcesProcessorHelper.removeChannelsSelect(preprocessFixed);
        // 1 - adds a channel select for the atlas
        fixedProcessor = new SourcesProcessComposer(fixedProcessor, new SourcesChannelsSelect(mp.reslicedAtlas.getLabelSourceIndex()));
        reg.setFixedMask(fixedProcessor.apply(mp.reslicedAtlas.nonExtendedSlicedSources));

        boolean out = reg.register();
        if (!out) {
            mp.errlog.accept(reg.getClass().getSimpleName()+": "+reg.getExceptionMessage());
        } else {
            appendRegistration(reg);
        }
        return out;
    }

    /*
     * Asynchronous handling of registrations + combining with manual sequential registration if necessary
     *
     * @param reg the registration to perform
     */
    protected boolean runRegistration(Registration<SourceAndConverter<?>[]> reg,
                                   SourcesProcessor preprocessFixed,
                                   SourcesProcessor preprocessMoving) {
        if (RegistrationPluginHelper.isManual(reg)) {
            //Waiting for manual lock release...
            synchronized (MultiSlicePositioner.manualActionLock) {
                //Manual lock released
                return performRegistration(reg,preprocessFixed, preprocessMoving);
            }
        } else {
            return performRegistration(reg,preprocessFixed, preprocessMoving);
        }
    }

    protected boolean removeRegistration(Registration reg) {
        if (registrations.contains(reg)) {
            int idx = registrations.indexOf(reg);
            if (idx == registrations.size() - 1) {

                registrations.remove(reg);

                registered_sacs_sequence.remove(registered_sacs_sequence.get(registered_sacs_sequence.size()-1));

                registered_sacs = registered_sacs_sequence.get(registered_sacs_sequence.size()-1).sacs;

                sourcesChanged();

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected void enqueueRunAction(CancelableAction action, Runnable postRun) {
        CompletableFuture<Boolean> startingPoint;
        if (tasks.size() == 0) {
            startingPoint = CompletableFuture.supplyAsync(() -> true);
        } else {
            startingPoint = tasks.get(tasks.size() - 1);
        }
        tasks.add(startingPoint.thenApplyAsync((out) -> {
            if (out) {
                mp.addTask();
                actionInProgress = action;
                logger.debug(this+": action "+action+" started");

                for (MultiSlicePositioner.SliceChangeListener listener: mp.listeners) {
                    listener.actionStarted(action.getSliceSources(), action);
                }

                boolean result = action.run();

                for (MultiSlicePositioner.SliceChangeListener listener: mp.listeners) {
                    listener.actionFinished(action.getSliceSources(), action, result);
                }

                logger.debug(this+": action "+action+" result "+result);
                if (result) {
                    actionInProgress = null;
                    postRun.run();
                } else {
                    mp.nonBlockingErrorMessageForUser.accept("Action failed", action.toString());
                    if (mapActionTask.containsKey(action)) {
                        CompletableFuture future = mapActionTask.get(action);
                        tasks.remove(future);
                    }
                    mapActionTask.remove(action);
                    mp.userActions.remove(action);
                }
                mp.removeTask();
                return result;
            } else {
                mp.errorMessageForUser.accept("Action not started","Upstream tasked failed, canceling action "+action);
                if (mapActionTask.containsKey(action)) {
                    CompletableFuture future = mapActionTask.get(action);
                    tasks.remove(future);
                }
                mapActionTask.remove(action);
                mp.userActions.remove(action);
                return false;
            }
        }));
        mapActionTask.put(action, tasks.get(tasks.size() - 1));
    }

    protected void enqueueCancelAction(CancelableAction action, Runnable postRun) {
        // Has the action started ?
        if (mapActionTask.containsKey(action)) {
            if (mapActionTask.get(action).isDone() || ((action!=null)&&(action == this.actionInProgress))) {

                if (action==actionInProgress) {
                   if (actionInProgress instanceof RegisterSliceAction) {
                       mp.addTask();
                       mp.listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelStarted(action.getSliceSources(), action));
                       boolean result;

                       // Special case : let's abort ASAP the registration to avoid overloading the server
                        logger.debug("Aborting register slice action :  "+actionInProgress);
                        ((RegisterSliceAction) actionInProgress).getRegistration().abort();
                        //postRun.run();
                        result = action.cancel();
                        if (mapActionTask.containsKey(action)) {
                            CompletableFuture future = mapActionTask.get(action);
                            tasks.remove(future);
                        }
                        mapActionTask.remove(action);
                        mp.userActions.remove(action);
                        postRun.run();
                        mp.listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelFinished(action.getSliceSources(), action, result));
                        mp.removeTask();
                   }
                } else {
                    CompletableFuture<Boolean> startingPoint;
                    if (tasks.size() == 0) {
                        startingPoint = CompletableFuture.supplyAsync(() -> true);
                    } else {
                        startingPoint = tasks.get(tasks.size() - 1);
                    }
                    tasks.add(startingPoint.thenApplyAsync((out) -> {
                        if (out) {
                            mp.addTask();
                            mp.listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelStarted(action.getSliceSources(), action));
                            boolean result = action.cancel();
                            tasks.remove(mapActionTask.get(action));
                            mapActionTask.remove(action);
                            postRun.run();
                            mp.listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelFinished(action.getSliceSources(), action, result));
                            mp.removeTask();
                            return result;
                        } else {
                            logger.error("Weird edge case!");
                            return false;
                        }
                    }));
                }
            } else {
                mp.addTask();
                mp.listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelStarted(action.getSliceSources(), action));
                // Not done yet! - let's remove right now from the task list
                boolean result = mapActionTask.get(action).cancel(true);
                tasks.remove(mapActionTask.get(action));
                mapActionTask.remove(action);
                postRun.run();
                mp.listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelFinished(action.getSliceSources(), action, result));
                mp.removeTask();
            }
        } else if (action instanceof CreateSliceAction) {
            mp.addTask();
            mp.listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelStarted(action.getSliceSources(), action));
            waitForEndOfTasks();
            boolean result = action.cancel();
            mp.listeners.forEach(sliceChangeListener -> sliceChangeListener.actionCancelFinished(action.getSliceSources(), action, result));
            mp.removeTask();
        } else {
            mp.errlog.accept("Unregistered action");
        }
    }

    <T extends NumericType<T> & NativeType<T>> void computeLabelImage(AffineTransform3D at3D) {
        labelImageBeingComputed = true;

        // 0 - slicing model : empty source but properly defined in space and resolution
        SourceAndConverter singleSliceModel = new EmptySourceAndConverterCreator("SlicingModel", at3D,
                mp.nPixX,
                mp.nPixY,
                1
        ).get();

        SourceResampler<T> resampler = new SourceResampler<>(null,
                singleSliceModel, toString()+"_Model", false, false, false, 0
        );

        AffineTransform3D translateZ = new AffineTransform3D();
        translateZ.translate(0, 0, -slicingAxisPosition);

        SourceAndConverter<T> sac =
                mp.reslicedAtlas.nonExtendedSlicedSources[mp.reslicedAtlas.getLabelSourceIndex()]; // By convention the label image is the last one

        sac = resampler.apply(sac);
        sac = SourceTransformHelper.createNewTransformedSourceAndConverter(translateZ, new SourceAndConverterAndTimeRange(sac, 0));

        Map<SourceAndConverter<T>, Integer> mapSacToMml = new HashMap<>();
        mapSacToMml.put(sac, 0);

        List<SourceAndConverter<?>> sourceList = new ArrayList<>();
        sourceList.add(sac);

        if (!(sourceList.get(0).getSpimSource().getType() instanceof IntegerType)) {
            logger.error("Label image is not integer typed! Type = "+sourceList.get(0).getSpimSource().getType().getClass().getSimpleName());
            return;
        }

        RandomAccessibleInterval<IntegerType<?>> raiLabel = (RandomAccessibleInterval<IntegerType<?>>) sourceList.get(0).getSpimSource().getSource(0,0);

        RandomAccessibleInterval<FloatType> cvtRai = convertedRai(raiLabel);

        impLabelImage = ImageJFunctions.wrap(cvtRai, "Labels");

        cvtRoisOrigin = constructROIsFromImgLabel(mp.getAtlas().getOntology(), impLabelImage);

        at3DLastLabelImage = at3D;
        labelImageBeingComputed = false;

        // Now Left Right:
        sac = mp.reslicedAtlas.nonExtendedSlicedSources[mp.reslicedAtlas.getLeftRightSourceIndex()]; // Don't know why this is working

        sac = resampler.apply(sac);
        sac = SourceTransformHelper.createNewTransformedSourceAndConverter(translateZ, new SourceAndConverterAndTimeRange(sac, 0));

        mapSacToMml = new HashMap<>();
        mapSacToMml.put(sac, 0);

        sourceList = new ArrayList<>();
        sourceList.add(sac);
        ImagePlus leftRightImage =
                ImagePlusHelper.wrap(sourceList.stream().map(s -> (SourceAndConverter<T>) s).collect(Collectors.toList()), mapSacToMml,
                        0, 1, 1);

        leftRightOrigin.set(ConvertibleRois.labelImageToRoiArrayKeepSinglePixelPrecision(leftRightImage));
    }

    private RandomAccessibleInterval<FloatType> convertedRai(RandomAccessibleInterval<IntegerType<?>> raiLabel) {
        Converter<IntegerType<?>, FloatType> cvt = new Converter<IntegerType<?>, FloatType>() {
            @Override
            public void convert(IntegerType<?> integerType, FloatType floatType) {
                floatType.set(Float.intBitsToFloat(integerType.getInteger()));
            }
        };
        return Converters.convert(raiLabel, cvt, new FloatType());
    }

    double rotXLastExport = Double.MAX_VALUE;
    double rotYLastExport = Double.MAX_VALUE;

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
                    if ((mp.getReslicedAtlas().getRotateX() == rotXLastExport)&&(mp.getReslicedAtlas().getRotateY() == rotYLastExport)) {
                        logger.debug("Slice " + this + ": Label image already computed, skips computation.");
                        computeLabelImageNecessary = false;
                    }
                }
            }
        }

        if (computeLabelImageNecessary) {
            logger.debug("Slice "+this+": Computing label image BEGIN.");
            rotXLastExport = mp.getReslicedAtlas().getRotateX();
            rotYLastExport = mp.getReslicedAtlas().getRotateY();
            computeLabelImage(at3D);
            logger.debug("Slice "+this+": Computing label image END.");
        }

        computeTransformedRois();

        // Renaming
        IJShapeRoiArray roiList = (IJShapeRoiArray) cvtRoisTransformed.to(IJShapeRoiArray.class);
        for (int i=0;i<roiList.rois.size();i++) {

            CompositeFloatPoly roi = roiList.rois.get(i);

            int atlasId = Integer.parseInt(roi.name);
            AtlasNode node = mp.getAtlas().getOntology().getNodeFromId(atlasId);
            roi.name = node.data().get(namingChoice);
            int[] color = node.getColor();
            roi.color = new Color(color[0], color[1], color[2], color[3]);//mp.getAtlas().getOntology().getColor(node);
        }

        IJShapeRoiArray roiArray = (IJShapeRoiArray) leftRightTranformed.to(IJShapeRoiArray.class);

        for (CompositeFloatPoly cfp: roiArray.rois) {
            int value = Integer.parseInt(cfp.getRoi().getName());
            if (value==mp.getAtlas().getMap().labelLeft()) {
                logger.debug("Left region detected");
                Roi left = cfp.getRoi();
                left.setStrokeColor(new Color(0,255,0));
                left.setName("Left");
                roiList.rois.add(new CompositeFloatPoly(left));
            } else if (value==mp.getAtlas().getMap().labelRight()) {
                logger.debug("Right region detected");
                Roi right = cfp.getRoi();
                right.setStrokeColor(new Color(255,0,255));
                right.setName("Right");
                roiList.rois.add(new CompositeFloatPoly(right));
            } else {
                logger.error("Unrecognized left right label : "+value);
            }
        }

    }

    public void exportRegionsToROIManager(String namingChoice) {
        prepareExport(namingChoice);
        cvtRoisTransformed.to(RoiManager.class);
    }

    public List<Roi> getRois(String namingChoice) {
        prepareExport(namingChoice);
        IJShapeRoiArray roiArray = (IJShapeRoiArray) cvtRoisTransformed.to(IJShapeRoiArray.class);
        List<Roi> rois = new ArrayList<>();
        for (CompositeFloatPoly cfp: roiArray.rois) {
            rois.add(cfp.getRoi());
        }
        return rois;
    }

    public void exportToQuPathProject(boolean erasePreviousFile) {
        prepareExport("id");
        ImageJRoisFile ijroisfile = (ImageJRoisFile) cvtRoisTransformed.to(ImageJRoisFile.class);
        storeInQuPathProjectIfExists(ijroisfile, erasePreviousFile);
    }

    public void exportRegionsToFile(String namingChoice, File dirOutput, boolean erasePreviousFile) {

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
                    mp.errlog.accept("ROI File already exists!");
                }
            } else {
                // Save in user specified folder
                Files.copy(Paths.get(ijroisfile.f.getAbsolutePath()),Paths.get(f.getAbsolutePath()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public RealTransform getSlicePixToCCFRealTransform() {
        return getSlicePixToCCFRealTransform(0,mp.getAtlas().getMap().getAtlasPrecisionInMillimeter()/5.0, 200);
    }

    boolean isWrapped(RealTransform rt) {
        return (rt instanceof BoundedRealTransform)
              ||(rt instanceof WrappedIterativeInvertibleRealTransform)
              ||(rt instanceof Wrapped2DTransformAs3D);
    }

    RealTransform getWrapped(RealTransform rt) {
        if (rt instanceof BoundedRealTransform) {
            return ((BoundedRealTransform)rt).getTransform();
        }
        if (rt instanceof WrappedIterativeInvertibleRealTransform) {
            return ((WrappedIterativeInvertibleRealTransform)rt).getTransform();
        }
        if (rt instanceof Wrapped2DTransformAs3D) {
            return ((Wrapped2DTransformAs3D)rt).getTransform();
        }
        return rt;
    }

    void fixOptimizer(RealTransform rt, double tolerance, int maxIteration) {
        RealTransform transform = rt;
        while (isWrapped(transform)) {
            transform = getWrapped(transform);
            if (transform instanceof WrappedIterativeInvertibleRealTransform) {
                ((WrappedIterativeInvertibleRealTransform<?>) transform).getOptimzer().setTolerance(tolerance);
                ((WrappedIterativeInvertibleRealTransform<?>) transform).getOptimzer().setMaxIters(maxIteration);
                break;
            }
        }
    }

    public RealTransform getSlicePixToCCFRealTransform(int resolutionLevel, double tolerance, int maxIteration) {
        RealTransformSequence rts = new RealTransformSequence();
        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();

        AffineTransform3D at3D;

        at3D = mp.getAffineTransformFromAlignerToAtlas();

        rts.add(at3D.inverse().copy());
        irts.add(at3D.inverse().copy());

        Collections.reverse(this.registrations);

        for (Registration reg : this.registrations) {
            RealTransform current = reg.getTransformAsRealTransform();
            if (current == null) return null;
            RealTransform copied = current.copy();
            fixOptimizer(copied, tolerance, maxIteration);
            rts.add(copied);
            if ((copied instanceof InvertibleRealTransform) && (irts != null)) {
                irts.add((InvertibleRealTransform) copied);
            } else {
                irts = null;
            }
        }

        Collections.reverse(this.registrations);

        this.original_sacs[0].getSpimSource().getSourceTransform(0,resolutionLevel,at3D);

        rts.add(at3D.inverse().copy());
        if (irts!=null) irts.add(at3D.inverse().copy());

        return (irts==null)?rts:irts;
    }

    private void storeInQuPathProjectIfExists(ImageJRoisFile ijroisfile, boolean erasePreviousFile) {

        if (!QuPathBdvHelper.isSourceLinkedToQuPath(original_sacs[0])) {
            mp.errlog.accept("Slice "+this+" not linked to a QuPath dataset");
        }
        File dataEntryFolder = null;

        try {
            dataEntryFolder = QuPathBdvHelper.getDataEntryFolder(original_sacs[0]);
            logger.debug("DataEntryFolder = "+dataEntryFolder);

            String projectFolderPath = QuPathBdvHelper.getProjectFile(original_sacs[0]).getParent();
            logger.debug("QuPath Project Folder = "+projectFolderPath);

            File f = new File(dataEntryFolder, "ABBA-RoiSet-"+mp.getAtlas().getName()+".zip");
            mp.log.accept("Save slice ROI to quPath project " + f.getAbsolutePath());

            if (f.exists()) {
                if (erasePreviousFile) {
                    Files.delete(Paths.get(f.getAbsolutePath()));
                    // Save in user specified folder
                    Files.copy(Paths.get(ijroisfile.f.getAbsolutePath()),Paths.get(f.getAbsolutePath()));
                    writeOntotogyIfNotPresent(mp, projectFolderPath);
                    //----------------- LEGACY
                    // For compatibility with previous versions
                    if (mp.getAtlas() instanceof AllenBrainAdultMouseAtlasCCF2017Command) {
                        // Save it the old fashioned way
                        f = new File(dataEntryFolder, "ABBA-RoiSet.zip");
                        if (f.exists()) {
                            Files.delete(Paths.get(f.getAbsolutePath()));
                        }
                        Files.copy(Paths.get(ijroisfile.f.getAbsolutePath()),Paths.get(f.getAbsolutePath()));
                    }
                    //----------------- LEGACY

                } else {
                    mp.errlog.accept("Error : QuPath ROI file already exists");
                }
            } else {
                Files.copy(Paths.get(ijroisfile.f.getAbsolutePath()),Paths.get(f.getAbsolutePath()));
                writeOntotogyIfNotPresent(mp, projectFolderPath);
                //----------------- LEGACY
                if (mp.getAtlas() instanceof AllenBrainAdultMouseAtlasCCF2017Command) {
                    // Save it the old fashioned way
                    f = new File(dataEntryFolder, "ABBA-RoiSet.zip");
                    Files.copy(Paths.get(ijroisfile.f.getAbsolutePath()),Paths.get(f.getAbsolutePath()));
                }
                //----------------- LEGACY
            }
        } catch (Exception e) {
            mp.errlog.accept("QuPath Entry data folder not found! ");
        }

        try {
            RealTransform transform = getSlicePixToCCFRealTransform();

            if (transform!=null) {
                File ftransform = new File(dataEntryFolder, "ABBA-Transform-"+mp.getAtlas().getName()+".json");
                mp.log.accept("Save transformation to quPath project " + ftransform.getAbsolutePath());

                Gson gson = ScijavaGsonHelper.getGsonBuilder(mp.scijavaCtx, false).setPrettyPrinting().create();

                String transform_string = gson.toJson(transform, RealTransform.class);

                if (ftransform.exists()) {
                    if (erasePreviousFile) {
                        Files.delete(Paths.get(ftransform.getAbsolutePath()));
                        FileWriter writer = new FileWriter(ftransform.getAbsolutePath());
                        writer.write(transform_string);
                        writer.flush();
                        writer.close();
                        //----------------- LEGACY
                        if (mp.getAtlas() instanceof AllenBrainAdultMouseAtlasCCF2017Command) {
                            // Save it the old fashioned way
                            ftransform = new File(dataEntryFolder, "ABBA-Transform.json");
                            writer = new FileWriter(ftransform.getAbsolutePath());
                            writer.write(transform_string);
                            writer.flush();
                            writer.close();
                        }
                        //----------------- LEGACY
                    } else {
                        mp.errlog.accept("Error : Transformation file already exists");
                    }
                } else {
                    FileWriter writer = new FileWriter(ftransform.getAbsolutePath());
                    writer.write(transform_string);
                    writer.flush();
                    writer.close();
                    //----------------- LEGACY
                    if (mp.getAtlas() instanceof AllenBrainAdultMouseAtlasCCF2017Command) {
                        // Save it the old fashioned way
                        ftransform = new File(dataEntryFolder, "ABBA-Transform.json");
                        writer = new FileWriter(ftransform.getAbsolutePath());
                        writer.write(transform_string);
                        writer.flush();
                        writer.close();
                    }
                    //----------------- LEGACY
                }
            }
        } catch (Exception e) {
            mp.errlog.accept("Error while saving transform file!");
        }

    }

    static public synchronized void writeOntotogyIfNotPresent(MultiSlicePositioner mp, String quPathFilePath) {
        File ontology = new File(quPathFilePath, mp.getAtlas().getName()+"-Ontology.json");
        if (!ontology.exists()) {
            AtlasHelper.saveOntologyToJsonFile(mp.getAtlas().getOntology(), ontology.getAbsolutePath());
        }
        //---------------- LEGACY
        if (mp.getAtlas() instanceof AllenBrainAdultMouseAtlasCCF2017Command) {
            ontology = new File(quPathFilePath, "AllenMouseBrainOntology.json");
            if (!ontology.exists()) {
                try {
                    URL ontologyURL = mp.getAtlas().getOntology().getDataSource();
                    if (ontologyURL.getFile()==null) {
                        mp.errlog.accept("No ontology file found at location "+ontologyURL);
                        return;
                    }
                    Path originalOntologyFile = Paths.get(ontologyURL.toURI());
                    Files.copy(originalOntologyFile, Paths.get(quPathFilePath, "AllenMouseBrainOntology.json"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //----------------- LEGACY
    }

    public String toString() {
        if (!name.equals("") ) {
            return name;
        } else {
            int index = mp.getSlices().indexOf(this);
            return "Slice_"+index;
        }
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

        // Reversing the transformations is somehow cheating...
        // There's something weird in the fact that affine transforms and realtransforms do not behave the same way

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

    protected void editLastRegistration(SourcesProcessor preprocessFixed,
                                     SourcesProcessor preprocessMoving) {
        Registration reg = this.registrations.get(registrations.size() - 1);
        if (RegistrationPluginHelper.isEditable(reg)) {
            mp.log.accept("Edition will begin when the manual lock is acquired");
            synchronized (MultiSlicePositioner.manualActionLock) {
                this.removeRegistration(reg);
                // preprocessFixed has an issue...
                reg.setFixedImage(
                        preprocessFixed.apply(mp.reslicedAtlas.nonExtendedSlicedSources)
                ); // No filtering -> all channels
                reg.setMovingImage(
                        preprocessMoving.apply(registered_sacs)
                ); // NO filtering -> all channels

                // 0 - remove channel select from pre processor
                SourcesProcessor fixedProcessor = SourcesProcessorHelper.removeChannelsSelect(preprocessFixed);
                // 1 - adds a channel select for the atlas
                fixedProcessor = new SourcesProcessComposer(fixedProcessor, new SourcesChannelsSelect(mp.reslicedAtlas.getLabelSourceIndex()));
                reg.setFixedMask(fixedProcessor.apply(mp.reslicedAtlas.nonExtendedSlicedSources));

                reg.edit();
                this.appendRegistration(reg);
            }
        } else {
            mp.log.accept("The last registration of class "+reg.getClass().getSimpleName()+" is not editable.");
        }
    }

    public String getInfo() {
        String sliceInfo = "";

        SourceAndConverter rootSac = SourceAndConverterInspector.getRootSourceAndConverter(original_sacs[0]);

        if (SourceAndConverterServices.getSourceAndConverterService()
                .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)==null) {
            sliceInfo+="No information available";
        } else {
            AbstractSpimData asd =
                    ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                            .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).asd;

            int viewSetupId = ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                    .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).setupId;

            Collection<String> datasetKeys = SourceAndConverterServices.getSourceAndConverterService().getMetadataKeys(asd);

            if (datasetKeys!=null) {
                StringBuilder sb = new StringBuilder();
                datasetKeys.forEach(key -> {
                    String value = "";
                    Object v = SourceAndConverterServices.getSourceAndConverterService().getMetadata(asd,key);
                    if (v!=null) value = v.toString();
                    sb.append(key+":"+value+"\n");
                });
                sliceInfo+=sb.toString();
            }

            BasicViewSetup bvs = (BasicViewSetup) asd.getSequenceDescription().getViewSetups().get(viewSetupId);

            if (bvs.hasName()) {
                sliceInfo+="viewsetup:"+bvs.getName()+" ["+bvs.getId()+"]\n";
            }

            if (QuPathBdvHelper.isSourceLinkedToQuPath(original_sacs[0])) {
                int entryId = QuPathBdvHelper.getEntryId(original_sacs[0]);
                sliceInfo+="Project: "+QuPathBdvHelper.getProjectFile(this.original_sacs[0]).getAbsolutePath()+"\n";
                sliceInfo+="- Entry Id: "+entryId;//qpent.getName()+" ["+qpent.getId()+"]";
            }
            /*if ((bvs.getAttribute(QuPathEntryEntity.class)!=null)&&(bvs.getAttribute(QuPathEntryEntity.class).getId()!=-1)) {
                TODO : make sure it works with legacy project!!

                // QuPathEntryEntity qpent = bvs.getAttribute(QuPathEntryEntity.class);
                // QuPathBdvHelper.getProjectFile(this.original_sacs[0]).getAbsolutePath();
                int entryId = QuPathBdvHelper.getEntryId(original_sacs[0]);
                sliceInfo+="Project: "+QuPathBdvHelper.getProjectFile(this.original_sacs[0]).getAbsolutePath()+"\n";
                sliceInfo+="- Entry Id: "+entryId;//qpent.getName()+" ["+qpent.getId()+"]";
            } else {
                //QuPathEntryEntity not found
            }*/
        }
        return sliceInfo;
    }

    boolean setAsKeySlice = false;

    public void keySliceOn() {
        setAsKeySlice = true;
        mp.listeners.forEach(sliceChangeListener -> sliceChangeListener.sliceKeyOn(this));
    }

    public void keySliceOff() {
        setAsKeySlice = false;
        mp.listeners.forEach(sliceChangeListener -> sliceChangeListener.sliceKeyOff(this));
    }

    public boolean isKeySlice() {
        return setAsKeySlice;
    }

    // Hack to avoid overriding position during sorting, see sortslices in MultiSlicePositioner
    double tempAxisPosition;
    public void setTempSlicingAxisPosition() {
        tempAxisPosition = slicingAxisPosition;
    }

    public double getTempSlicingAxisPosition() {
        return tempAxisPosition;
    }

    public SourceAndConverter[] getRegisteredSources(int stepBack) {
        if (stepBack==0) {
            return getRegisteredSources();
        } else {
            return registered_sacs_sequence.get(Math.max(2,registered_sacs_sequence.size() - 1 - stepBack)).sacs;
        }
    }

    public IAlphaSource getAlpha() {
        return alphaSource;
    }

    public static class RegistrationAndSources {

        final Registration reg;
        final SourceAndConverter[] sacs;

        public RegistrationAndSources(Registration reg, SourceAndConverter[] sacs) {
            this.reg = reg;
            this.sacs = sacs;
        }
    }

    SliceInterval si = new SliceInterval();

    class SliceInterval implements RealInterval {

        RealPoint ptMin = new RealPoint(3);
        RealPoint ptMax = new RealPoint(3);

        void updateBox() {
            ptMin.setPosition(mp.reslicedAtlas.realMin(0),0);
            ptMin.setPosition(mp.reslicedAtlas.realMin(1),1);
            ptMin.setPosition(slicingAxisPosition-thicknessInMm/2.0, 2);

            ptMax.setPosition(mp.reslicedAtlas.realMax(0),0);
            ptMax.setPosition(mp.reslicedAtlas.realMax(1),1);
            ptMax.setPosition(slicingAxisPosition+thicknessInMm/2.0, 2);
        }

        @Override
        public double realMin(int i) {
            return ptMin.getDoublePosition(i);
        }

        @Override
        public double realMax(int i) {
            return ptMax.getDoublePosition(i);
        }

        @Override
        public int numDimensions() {
            return 3;
        }
    }

    private static ConvertibleRois constructROIsFromImgLabel(AtlasOntology ontology, ImagePlus labelImg) {

        ImageProcessor ip = labelImg.getProcessor();
        float[][] pixels = ip.getFloatArray();

        boolean[][] movablePx = new boolean[ip.getWidth()+1][ip.getHeight()+1];
        for (int x=1;x<ip.getWidth();x++) {
            for (int y=1;y<ip.getHeight();y++) {
                boolean is3Colored = false;
                boolean isCrossed = false;
                float p1p1 = pixels[x][y];
                float p1m1 = pixels[x][y-1];
                float m1p1 = pixels[x-1][y];
                float m1m1 = pixels[x-1][y-1];
                float min = p1p1;
                if (p1m1<min) min = p1m1;
                if (m1p1<min) min = m1p1;
                if (m1m1<min) min = m1m1;
                float max = p1p1;
                if (p1m1>max) max = p1m1;
                if (m1p1>max) max = m1p1;
                if (m1m1>max) max = m1m1;
                if (min!=max) {
                    if ((p1p1!=min)&&(p1p1!=max)) is3Colored=true;
                    if ((m1p1!=min)&&(m1p1!=max)) is3Colored=true;
                    if ((p1m1!=min)&&(p1m1!=max)) is3Colored=true;
                    if ((m1m1!=min)&&(m1m1!=max)) is3Colored=true;

                    if (!is3Colored) {
                        if ((p1p1==m1m1)&&(p1m1==m1p1)) {
                            isCrossed=true;
                        }
                    }
                } // if not it's monocolored
                movablePx[x][y]=(!is3Colored)&&(!isCrossed);
            }
        }

        // Hack: re set the label image according to the real id ( Mouse Allen Brain Hack )
        // Gets all existing values in the image
        HashSet<Integer> existingLabelValues = new HashSet<>();
        for (int x=0;x<ip.getWidth();x++) {
            for (int y=0;y<ip.getHeight();y++) {
                existingLabelValues.add(Float.floatToRawIntBits(pixels[x][y]));
            }
        }

        FloatProcessor fp = new FloatProcessor(ip.getWidth(), ip.getHeight());
        fp.setFloatArray(pixels);
        ImagePlus imgFloatCopy = new ImagePlus("FloatLabel",fp);

        HashSet<Integer> existingIdValues = new HashSet<>();
        existingLabelValues.forEach(v -> {
            fp.setThreshold( Float.intBitsToFloat(v), Float.intBitsToFloat(v), ImageProcessor.NO_LUT_UPDATE);
            Roi roi = SelectToROIKeepLines.run(imgFloatCopy, movablePx, true);
            AtlasNode node = ontology.getNodeFromId(v);
            if (node!=null) {
                int correctedId = node.getId();
                existingIdValues.add(correctedId);
                fp.setColor(Float.intBitsToFloat(correctedId));
                fp.fill(roi);
            }
        });

        // All the parents of the existing label will be met at some point
        // keep a list of possible values encountered in the tree
        HashSet<Integer> possibleIdValues = new HashSet<>();
        existingIdValues.forEach(id -> {
            possibleIdValues.addAll(AtlasHelper.getAllParentIds(ontology, id));
            possibleIdValues.add(id);
        });

        // We should keep, for each possible values, a way to know
        // if their are some labels which belong to children labels in the image.
        Map<Integer, Set<Integer>> childrenContained = new HashMap<>();
        possibleIdValues.forEach(idValue -> {
            AtlasNode node = ontology.getNodeFromId(idValue);
            if (node != null) {
                Set<Integer> valuesMetInTheImage = node.children().stream()
                        .map(n -> (AtlasNode) n)
                        .map(AtlasNode::getId)
                        .filter(possibleIdValues::contains)
                        .collect(Collectors.toSet());
                childrenContained.put(idValue, valuesMetInTheImage);
            }
        });

        HashSet<Integer> isLeaf = new HashSet<>();
        childrenContained.forEach((k,v) -> {
            if (v.size()==0) {
                isLeaf.add(k);
            }
        });

        boolean containsLeaf=true;

        ArrayList<Roi> roiArray = new ArrayList<>();

        while (containsLeaf) {
            List<Integer> leavesValues = existingIdValues
                    .stream()
                    .filter(v -> isLeaf.contains(v))
                    .collect(Collectors.toList());
            leavesValues.forEach(v -> {
                        fp.setThreshold( Float.intBitsToFloat(v), Float.intBitsToFloat(v), ImageProcessor.NO_LUT_UPDATE);
                        Roi roi = SelectToROIKeepLines.run(imgFloatCopy, movablePx, true);

                        roi.setName(Integer.toString(v));
                        roiArray.add(roi);

                        if (ontology.getNodeFromId(v)!=null) {
                            AtlasNode parent = (AtlasNode) ontology.getNodeFromId(v).parent();
                            if (parent!=null) {

                                int parentId = parent.getId();
                                fp.setColor(Float.intBitsToFloat(parentId));
                                fp.fill(roi);
                                if (childrenContained.get(parentId)!=null) {
                                    if (childrenContained.get(v).size()==0) {
                                        childrenContained.get(parentId).remove(v);
                                    }
                                    existingIdValues.add(parentId);
                                }
                            }
                        }
                    }
            );
            existingIdValues.removeAll(leavesValues);
            leavesValues.forEach(childrenContained::remove);
            isLeaf.clear();
            childrenContained.forEach((k,v) -> {
                        if (v.size()==0) {
                            isLeaf.add(k);
                        }
                    }
            );
            containsLeaf = existingIdValues.stream().anyMatch(v -> isLeaf.contains(v));
        }

        ConvertibleRois cr_out = new ConvertibleRois();
        IJShapeRoiArray output = new IJShapeRoiArray(roiArray);
        output.smoothenWithConstrains(movablePx);
        output.smoothenWithConstrains(movablePx);
        cr_out.set(output);
        return cr_out;
    }

    public void setDisplayRange(int channelIndex, double min, double max) {
        Displaysettings ds = new Displaysettings(-1);
        Displaysettings.GetDisplaySettingsFromCurrentConverter(getRegisteredSources()[channelIndex], ds);
        ds.min = min;
        ds.max = max;
        registered_sacs_sequence.stream().forEach(registrationAndSources -> {
            Displaysettings.applyDisplaysettings(registrationAndSources.sacs[channelIndex],ds);
        });
        Displaysettings.applyDisplaysettings(registered_sacs[channelIndex],ds);
        mp.converterChanged(this);
    }

    public void setDisplayColor(int channelIndex, int r, int g, int b, int a) {
        Displaysettings ds = new Displaysettings(-1);
        Displaysettings.GetDisplaySettingsFromCurrentConverter(getRegisteredSources()[channelIndex], ds);
        ds.color = new int[]{r,g,b,a};
        registered_sacs_sequence.stream().forEach(registrationAndSources -> {
            Displaysettings.applyDisplaysettings(registrationAndSources.sacs[channelIndex],ds);
        });
        Displaysettings.applyDisplaysettings(registered_sacs[channelIndex],ds);
        mp.converterChanged(this);
    }

    public void setDisplaySettings(Displaysettings[] displaysettings) {
        for (int channelIndex = 0; channelIndex<nChannels; channelIndex++) {
            for (RegistrationAndSources registrationAndSources: registered_sacs_sequence) {
                Displaysettings.applyDisplaysettings(registrationAndSources.sacs[channelIndex], displaysettings[channelIndex]);
            }
            Displaysettings.applyDisplaysettings(registered_sacs[channelIndex], displaysettings[channelIndex]);
        }
    }

}