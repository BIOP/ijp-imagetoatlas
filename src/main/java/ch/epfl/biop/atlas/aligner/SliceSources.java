package ch.epfl.biop.atlas.aligner;

import bdv.AbstractSpimSource;
import bdv.SpimSource;
import bdv.img.WarpedSource;
import bdv.tools.transformation.TransformedSource;
import bdv.util.BoundedRealTransform;
import bdv.util.DefaultInterpolators;
import bdv.util.QuPathBdvHelper;
import bdv.util.RealTransformHelper;
import bdv.util.ResampledSource;
import bdv.util.source.alpha.AlphaSourceHelper;
import bdv.util.source.alpha.AlphaSourceRAI;
import bdv.util.source.alpha.IAlphaSource;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
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
import ch.epfl.biop.registration.sourceandconverter.mirror.MirrorXRegistration;
import ch.epfl.biop.registration.sourceandconverter.mirror.MirrorXTransform;
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
import net.imglib2.realtransform.BoundingBoxEstimation;
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
import sc.fiji.bdvpg.sourceandconverter.transform.SourceRealTransformer;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;
import sc.fiji.persist.ScijavaGsonHelper;
import spimdata.util.Displaysettings;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static net.imglib2.realtransform.BoundingBoxEstimation.Method.CORNERS;
import static net.imglib2.realtransform.BoundingBoxEstimation.Method.FACES;
import static net.imglib2.realtransform.BoundingBoxEstimation.Method.VOLUME;


/**
 * Class which contains the current registered SourceAndConverter array
 * Each element of the array is a channel
 * This class should be UI independent (no show / bdvhandle, etc)
 */

public class SliceSources {

    protected static final Logger logger = LoggerFactory.getLogger(SliceSources.class);

    //final private SliceSourcesGUIState guiState; // in here ? GOod idea ?

    // What are they ?
    final SourceAndConverter<?>[] original_sacs;

    public final int nChannels;

    // Used for registration : like 3D, but tilt and roll ignored because it is handled on the fixed source side
    private volatile SourceAndConverter<?>[] registered_sacs;

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
        double voxY = original_sacs[0].getSpimSource().getVoxelDimensions().dimension(1);
        double voxZ = original_sacs[0].getSpimSource().getVoxelDimensions().dimension(2);
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
                name = rootSac.getSpimSource().getName();
            } else {
                AbstractSpimData asd =
                        ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                                .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).asd;

                int viewSetupId = ((SourceAndConverterService.SpimDataInfo)SourceAndConverterServices.getSourceAndConverterService()
                        .getMetadata(rootSac, SourceAndConverterService.SPIM_DATA_INFO)).setupId;

                BasicViewSetup bvs = (BasicViewSetup) asd.getSequenceDescription().getViewSetups().get(viewSetupId);

                // TODO : keep in sync name with QuPath -> for that, read imagename through opener
                // TODO BUT MAKE SURE IT WORKS WITH LEGACY QUPATH IMAGE LOADER!

                if (bvs.getAttribute(ImageName.class)!=null) {
                    name = bvs.getAttribute(ImageName.class).getName();
                } else {
                    name = rootSac.getSpimSource().getName();
                }

            }
        } catch(Exception e) {
            mp.errlog.accept("Couldn't name slice, empty name chosen");
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
        try {
            updateZPosition();
        } catch (Exception e) {
            System.out.println("CAUGHT ERROR IN UPDATE POSITION (You can safely ignore it, it will just affect the display temporarily)"+e.getMessage());
            //e.printStackTrace();
        }
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
        zShiftCorrection = ((zEndInMm+zBeginInMm) / 2.0) - slicingAxisPosition;
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
    } // TODO : fix thread deadlock!

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
        originalSacsSet.addAll(Arrays.asList(original_sacs));
        return (sacs.stream().distinct().anyMatch(originalSacsSet::contains));
    }

    public void waitForEndOfTasks() {
        if (tasks.size()>0) {
            try {
                CompletableFuture<Boolean> lastTask = tasks.get(tasks.size()-1);
                lastTask.get();
            } catch (Exception e) {
                e.printStackTrace();
                mp.errlog.accept("Tasks were cancelled for slice "+this+" Error:"+e.getMessage());
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

    protected boolean hideLastMirrorRegistration() {
        boolean performed = false;
        for (int iReg = registered_sacs_sequence.size()-1; iReg>0; iReg--) {
            RegistrationAndSources ras = registered_sacs_sequence.get(iReg);
            if (ras.sacs[0].getSpimSource() instanceof WarpedSource) {
                WarpedSource<?> ws = (WarpedSource<?>) ras.sacs[0].getSpimSource();

                RealTransform transform = ws.getTransform();

                if (transform instanceof BoundedRealTransform) {
                    transform = ((BoundedRealTransform) transform).getTransform();
                }

                if (transform instanceof MirrorXTransform) {
                    if (ws.isTransformed()) {
                        // Ok, remove it!
                        performed = true;
                        for (int iCh = 0; iCh<ras.sacs.length; iCh++) {
                            ((WarpedSource<?>)ras.sacs[iCh].getSpimSource()).setIsTransformed(false);
                            if (ras.sacs[iCh].asVolatile()!=null) {
                                ((WarpedSource<?>)ras.sacs[iCh].asVolatile().getSpimSource()).setIsTransformed(false);
                            }
                        }
                        break;
                    }
                    //this.removeRegistration(ras.reg);
                }
            }
        }
        if (!performed) {
            logger.error("No mirror transformation found!");
        }
        return performed;
    }

    protected boolean restoreLastMirrorRegistration() {
        boolean performed = false;
        for (RegistrationAndSources ras : registered_sacs_sequence) {
            if (ras.sacs[0].getSpimSource() instanceof WarpedSource) {
                WarpedSource<?> ws = (WarpedSource<?>) ras.sacs[0].getSpimSource();

                RealTransform transform = ws.getTransform();

                if (transform instanceof BoundedRealTransform) {
                    transform = ((BoundedRealTransform) transform).getTransform();
                }

                if (transform instanceof MirrorXTransform) {
                    if (!ws.isTransformed()) {
                        // Ok, do it!
                        performed = true;
                        for (int iCh = 0; iCh < ras.sacs.length; iCh++) {
                            ((WarpedSource<?>) ras.sacs[iCh].getSpimSource()).setIsTransformed(true);
                            if (ras.sacs[iCh].asVolatile() != null) {
                                ((WarpedSource<?>) ras.sacs[iCh].asVolatile().getSpimSource()).setIsTransformed(true);
                            }
                        }
                        break;
                    }
                }
            }
        }
        if (!performed) {
            logger.error("No mirror transformation to restore!");
        }
        return performed;
    }

    protected void appendRegistration(Registration<SourceAndConverter<?>[]> reg) {

        if (reg instanceof RealTransformSourceAndConverterRegistration) {
            RealTransformSourceAndConverterRegistration sreg = (RealTransformSourceAndConverterRegistration) reg;
            if (!(sreg.getRealTransform() instanceof BoundedRealTransform)) {
                if (sreg.getRealTransform() instanceof InvertibleRealTransform) {
                    BoundedRealTransform brt = new BoundedRealTransform((InvertibleRealTransform) sreg.getRealTransform(), si);
                    si.updateBox();
                    sreg.setRealTransform(brt);
                }
            } else {
                // We need to set the right interval! During deserialisation, the interval is not set correctly
                // That's also required when one wants to transfer a registration sequence from one slice to another one
                RealTransform rt = ((BoundedRealTransform) sreg.getRealTransform()).getTransform();
                BoundedRealTransform brt = new BoundedRealTransform((InvertibleRealTransform) rt, si);
                si.updateBox();
                sreg.setRealTransform(brt);
            }
        }

        registered_sacs = reg.getTransformedImageMovingToFixed(registered_sacs);

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
                return performRegistration(reg, preprocessFixed, preprocessMoving);
            }
        } else {
            return performRegistration(reg, preprocessFixed, preprocessMoving);
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
                logger.error("Could not remove a registration which is not the last one.");
                return false;
            }
        } else {
            logger.error("Registration not found, can't remove it.");
            return false;
        }
    }

    Executor executor = ForkJoinPool.commonPool();

    protected void enqueueRunAction(CancelableAction action, Runnable postRun, boolean runInExtraThread) {
        CompletableFuture<Boolean> startingPoint;
        if (tasks.size() == 0) {
            startingPoint = CompletableFuture.supplyAsync(() -> true);
        } else {
            startingPoint = tasks.get(tasks.size() - 1);
        }
        Executor e = executor;
        // Otherwise the synchronisation mechanism is locked forever...
        // This way of synchronizing is costly, and will not scale for a number of section > 1000, but we'll live with that
        if (runInExtraThread) e = new ThreadPerTaskExecutor();
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
                    //mp.mso.getActionsFromSlice(this).remove(action);
                    mp.getActionsFromSlice(this).remove(action);
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
                mp.getActionsFromSlice(this).remove(action);
                return false;
            }
        }, e));
        mapActionTask.put(action, tasks.get(tasks.size() - 1));
    }

    static final class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) { new Thread(r).start(); }
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
                singleSliceModel, this+"_Model", false, false, false, 0
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

    void prepareExport(String namingChoice, int iChannel) {
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

        computeTransformedRois(iChannel);

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
        prepareExport(namingChoice, 0);
        cvtRoisTransformed.to(RoiManager.class);
    }

    public List<Roi> getRois(String namingChoice) {
        prepareExport(namingChoice, 0);
        IJShapeRoiArray roiArray = (IJShapeRoiArray) cvtRoisTransformed.to(IJShapeRoiArray.class);
        List<Roi> rois = new ArrayList<>();
        for (CompositeFloatPoly cfp: roiArray.rois) {
            rois.add(cfp.getRoi());
        }
        return rois;
    }

    public void exportToQuPathProject(boolean erasePreviousFile) {
        //storeInQuPathProjectIfExists(ijroisfile, erasePreviousFile);
        if (simpleQuPathExportCase()) {
            prepareExport("id", 0);
            ImageJRoisFile ijroisfile = (ImageJRoisFile) cvtRoisTransformed.to(ImageJRoisFile.class);
            storeInQuPathProjectIfExists(ijroisfile, erasePreviousFile);
        } else {
            // That's complicated
            Set<File> dataEntries = new HashSet<>();
            for (int iCh = 0; iCh<nChannels; iCh++) {
                SourceAndConverter<?> source = original_sacs[iCh];
                if (!QuPathBdvHelper.isSourceLinkedToQuPath(source)) {
                    continue;
                }
                File dataEntryFolderTest = QuPathBdvHelper.getDataEntryFolder(source);
                if (dataEntries.contains(dataEntryFolderTest)) {
                    continue;
                }
                dataEntries.add(dataEntryFolderTest);

                exportToQuPathProjectAdvanced(iCh, erasePreviousFile);

            }
        }
    }

    private InvertibleRealTransform getTransformedSequenceToRoot(SourceAndConverter<?> source) {
        // Remove the transform coming from
        //AffineTransform3D lastTransform = new AffineTransform3D();
        //source.getSpimSource().getSourceTransform(0,0, lastTransform);
        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();
        //irts.add(lastTransform.inverse());
        appendPreviousTransform(source.getSpimSource(), irts);
        return irts;
    }

    private void appendPreviousTransform(Source<?> source, InvertibleRealTransformSequence sequence) {
        if (source.getClass().equals(SpimSource.class)) { // end condition
            AbstractSpimSource<?> castSource = ((AbstractSpimSource<?>)source);
            AffineTransform3D transform = new AffineTransform3D();
            castSource.getSourceTransform(0,0,transform);
            sequence.add(transform.inverse());
        } else if (source.getClass().equals(TransformedSource.class)) {
            TransformedSource<?> castSource = ((TransformedSource<?>)source);
            AffineTransform3D transform = new AffineTransform3D();
            castSource.getSourceTransform(0,0,transform);
            sequence.add(transform.inverse());
            appendPreviousTransform(castSource.getWrappedSource(), sequence);
        } else if (source.getClass().equals(WarpedSource.class)) {
            WarpedSource<?> castSource = ((WarpedSource<?>)source);
            RealTransform rt = castSource.getTransform().copy();
            if (rt instanceof InvertibleRealTransform) {
                sequence.add((InvertibleRealTransform) rt);
                appendPreviousTransform(castSource.getWrappedSource(), sequence);
            } else {
                logger.error("The transform of the warped source is not invertible! ");
            }
        }else if (source.getClass().equals(ResampledSource.class)) {
            logger.error("Unhandled source of type "+source.getClass().getSimpleName());
        } else {
            logger.error("Unknown source of type "+source.getClass().getSimpleName());
        }
    }

    private void exportToQuPathProjectAdvanced(int iCh, boolean erasePreviousFile) {
        // This gets complicated...
        // This needs to get fixed!!
        prepareExport("id", iCh);
        // TO FIX!!
        ImageJRoisFile ijroisfile = (ImageJRoisFile) cvtRoisTransformed.to(ImageJRoisFile.class);

        SourceAndConverter<?> source = original_sacs[iCh];
        File dataEntryFolder = QuPathBdvHelper.getDataEntryFolder(source);
        logger.debug("DataEntryFolder = "+dataEntryFolder);

        // There's an extra sequence of transform to go from original_sacs[iCh] to the QuPath source.
        // We have to build this sequence, and get the transformations at each step
        // That's going to be complicated

        InvertibleRealTransform irts = getTransformedSequenceToRoot(source);

        //System.out.println("Channel "+iCh+"\n"+mp.getGsonStateSerializer(new ArrayList<>()).toJson(irts));

        try {

            String projectFolderPath = QuPathBdvHelper.getProjectFile(original_sacs[iCh]).getParent();
            logger.debug("QuPath Project Folder = "+projectFolderPath);

            File f = new File(dataEntryFolder, "ABBA-RoiSet-"+mp.getAtlas().getName()+".zip");
            mp.log.accept("Save slice ROI to quPath project " + f.getAbsolutePath());

            if (f.exists()&&(!erasePreviousFile)) {
                mp.errlog.accept("Error : QuPath ROI file already exists");
            }

            if ((!f.exists())||(erasePreviousFile)) {
                if (f.exists()) {
                    Files.delete(Paths.get(f.getAbsolutePath()));
                }
                Files.copy(Paths.get(ijroisfile.f.getAbsolutePath()),Paths.get(f.getAbsolutePath()));
                writeOntotogyIfNotPresent(mp, projectFolderPath);
            }

        } catch (Exception e) {
            mp.errlog.accept("Error in QuPath export: "+e.getMessage());
            e.printStackTrace();
        }

        try {
            RealTransform transform = getSlicePhysicalToCCFRealTransform(0, getTolerance(), getMaxIteration());
            // Adds the pretransformation
            RealTransform fullSequence;
            if (transform instanceof InvertibleRealTransform) {
                fullSequence = new InvertibleRealTransformSequence();
                ((InvertibleRealTransformSequence)fullSequence).add((InvertibleRealTransform) transform);
                ((InvertibleRealTransformSequence)fullSequence).add(irts);
            } else {
                fullSequence = new RealTransformSequence();
                ((RealTransformSequence)fullSequence).add(transform);
                ((RealTransformSequence)fullSequence).add(irts);
            }
            // end of adding the pretransform sequence
            if (fullSequence!=null) {
                File ftransform = new File(dataEntryFolder, "ABBA-Transform-"+mp.getAtlas().getName()+".json");
                mp.log.accept("Save transformation to quPath project " + ftransform.getAbsolutePath());
                Gson gson = ScijavaGsonHelper.getGsonBuilder(mp.scijavaCtx, false).setPrettyPrinting().create();
                String transform_string = gson.toJson(fullSequence, RealTransform.class);

                if (ftransform.exists()) {
                    if (erasePreviousFile) {
                        Files.delete(Paths.get(ftransform.getAbsolutePath()));
                        FileWriter writer = new FileWriter(ftransform.getAbsolutePath());
                        writer.write(transform_string);writer.flush();writer.close();
                    } else {
                        mp.errlog.accept("Error : Transformation file already exists");
                    }
                } else {
                    FileWriter writer = new FileWriter(ftransform.getAbsolutePath());
                    writer.write(transform_string);writer.flush();writer.close();
                }
            }
        } catch (Exception e) {
            mp.errlog.accept("Error while saving transform file!");
        }

    }

    private boolean simpleQuPathExportCase() {
        // It is a simple case of export if:
        // - All sources from this slice are DIRECTLY coming a from single QuPath entry
        // - DIRECTLY means that the slices are not warped or wrapped into transfomed sources beforehand

        File dataEntryFolder = null;


        for (SourceAndConverter<?> source: original_sacs) {
            // All linked to QuPath ?
            if (!QuPathBdvHelper.isSourceLinkedToQuPath(source)) {
                mp.errlog.accept("Slice "+this+" not linked to a QuPath dataset");
                return false;
            }

            // Same entry ?
            try {
                File dataEntryFolderTest = QuPathBdvHelper.getDataEntryFolder(source);
                if (dataEntryFolder == null) {
                    dataEntryFolder = dataEntryFolderTest;
                } else {
                    if (!dataEntryFolder.equals(dataEntryFolderTest)) {
                        // There are two entries from the same project -> complex case
                        logger.info("Slice "+this+" targets multiple QuPath entries.");
                        return false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                mp.errlog.accept(e.getMessage());
                return false;
            };
        }

        // No wrapping ?
        for (SourceAndConverter<?> source: original_sacs) {
            if (SourceAndConverterServices.getSourceAndConverterService().getMetadata(source, "SPIMDATA") == null) {
                logger.info("Slice "+this+" is not directly a QuPath project entry. It could have been transformed before being imported in ABBA");
                return false;
            }
        }

        return true;
    }

    public IJShapeRoiArray getOriginalAtlasRegions(String namingChoice) {
        prepareExport(namingChoice, 0);
        return (IJShapeRoiArray) cvtRoisOrigin.to(IJShapeRoiArray.class);
    }

    public void exportRegionsToFile(String namingChoice, File dirOutput, boolean erasePreviousFile) {

        prepareExport(namingChoice, 0);

        ImageJRoisFile ijroisfile = (ImageJRoisFile) cvtRoisTransformed.to(ImageJRoisFile.class);

        //--------------------

        File f = new File(dirOutput, this+".zip");
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
        return getSlicePixToCCFRealTransform(0,getTolerance(), getMaxIteration());
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

    double getTolerance() {
        return mp.getAtlas().getMap().getAtlasPrecisionInMillimeter()/5.0;
    }

    @SuppressWarnings("SameReturnValue")
    int getMaxIteration() {
        return 200;
    }

    public RealTransform getSlicePixToCCFRealTransform(int resolutionLevel, double tolerance, int maxIteration) {
        RealTransformSequence rts = new RealTransformSequence();
        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();

        AffineTransform3D at3D;

        at3D = mp.getAffineTransformFromAlignerToAtlas();

        rts.add(at3D.inverse().copy());
        irts.add(at3D.inverse().copy());

        addAllRegistrations(rts, irts, tolerance, maxIteration);

        this.original_sacs[0].getSpimSource().getSourceTransform(0,resolutionLevel,at3D);
        rts.add(at3D.inverse().copy());
        if (irts!=null) irts.add(at3D.inverse().copy());

        return (irts==null)?rts:irts;
    }

    public RealTransform getSlicePhysicalToCCFRealTransform(int resolutionLevel, double tolerance, int maxIteration) {
        RealTransformSequence rts = new RealTransformSequence();
        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();

        AffineTransform3D at3D;

        at3D = mp.getAffineTransformFromAlignerToAtlas();

        rts.add(at3D.inverse().copy());
        irts.add(at3D.inverse().copy());

        addAllRegistrations(rts, irts, tolerance, maxIteration);

        //this.original_sacs[0].getSpimSource().getSourceTransform(0,resolutionLevel,at3D);
        //rts.add(at3D.inverse().copy());
        //if (irts!=null) irts.add(at3D.inverse().copy());

        return (irts==null)?rts:irts;
    }


    private void addAllRegistrations(RealTransformSequence rts, InvertibleRealTransformSequence irts, double tolerance, int maxIteration) {

        Collections.reverse(this.registrations);

        for (Registration reg : this.registrations) {
            RealTransform current = reg.getTransformAsRealTransform();
            if (!(current instanceof MirrorXTransform)) { // We should not take the mirror transform into account
                if (current == null) {
                    mp.errlog.accept("Error : null registration found!");
                    return;
                }
                RealTransform copied = current.copy();
                fixOptimizer(copied, tolerance, maxIteration);
                rts.add(copied);
                if ((copied instanceof InvertibleRealTransform) && (irts != null)) {
                    irts.add((InvertibleRealTransform) copied);
                } else {
                    irts = null;
                }
            }
        }

        Collections.reverse(this.registrations);
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
                    //noinspection deprecation
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
                //noinspection deprecation
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
                        //noinspection deprecation
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
                    //noinspection deprecation
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
        //noinspection deprecation
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

    private void computeTransformedRois(int iChannel) {
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
        listRegions = transformPoints(listRegions, at3D);

        listLeftRight = transformPoints(listLeftRight, at3D);

        // Reversing the transformations is somehow cheating...
        // There's something weird in the fact that affine transforms and realtransforms do not behave the same way

        Collections.reverse(this.registrations);

        for (Registration reg : this.registrations) {
            if (!(reg instanceof MirrorXRegistration)) {// Skips mirror registration
                listRegions = reg.getTransformedPtsFixedToMoving(listRegions);
                listLeftRight = reg.getTransformedPtsFixedToMoving(listLeftRight);
            }
        }
        
        Collections.reverse(this.registrations);

        InvertibleRealTransform transform = getTransformedSequenceToRoot(original_sacs[iChannel]);

        //this.original_sacs[0].getSpimSource().getSourceTransform(0,0,at3D);
        listRegions = transformPoints(listRegions, transform);
        listLeftRight = transformPoints(listLeftRight, transform);

        cvtRoisTransformed.clear();
        listRegions.shapeRoiList = new IJShapeRoiArray(arrayIniRegions);

        leftRightTranformed.clear();
        listLeftRight.shapeRoiList = new IJShapeRoiArray(arrayIniLeftRight);

        cvtRoisTransformed.set(listRegions);
        leftRightTranformed.set(listLeftRight);
    }

    private static RealPointList transformPoints(RealPointList pts, InvertibleRealTransform transform) {
        ArrayList<RealPoint> cvtList = new ArrayList<>();
        for (RealPoint p : pts.ptList) {
            RealPoint pt3d = new RealPoint(3);
            pt3d.setPosition(new double[]{p.getDoublePosition(0), p.getDoublePosition(1),0});
            transform.apply(pt3d, pt3d);
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
            if (QuPathBdvHelper.isSourceLinkedToQuPath(original_sacs[0])) {
                int entryId = QuPathBdvHelper.getEntryId(original_sacs[0]);
                sliceInfo+="QuPath Project: "+QuPathBdvHelper.getProjectFile(this.original_sacs[0]).getParent()+"\n";
                sliceInfo+="Entry Id: "+entryId;//qpent.getName()+" ["+qpent.getId()+"]";
            }
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

    final LinkedList<SourceAndConverter<?>[]> sourcesDeformationFieldNonRasterized = new LinkedList<>();

    final LinkedList<SourceAndConverter<?>[]> sourcesNonRasterized = new LinkedList<>();

    protected void pushRasterDeformation(double gridSpacingInMicrometer) {
        sourcesDeformationFieldNonRasterized.push(registered_sacs);

        RealTransformSequence rts = new RealTransformSequence();
        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();

        this.addAllRegistrations(rts, irts, getTolerance(), getMaxIteration());

        SourceRealTransformer srt;

        Source<?> model = getModelWithGridSize(gridSpacingInMicrometer);

        if (irts!=null) {
            srt = new SourceRealTransformer<>(RealTransformHelper.resampleTransform(irts, model));
        } else {
            srt = new SourceRealTransformer<>(RealTransformHelper.resampleTransform(rts, model));
        }

        registered_sacs = new SourceAndConverter[nChannels]; // Compulsory or the push is useless!

        for (int iChannel = 0; iChannel < this.nChannels; iChannel++) {
            registered_sacs[iChannel] = srt.apply(original_sacs[iChannel]);
            // Attempt to fix issue with transformation caching, but it's not working
            //((WarpedSource<?>)registered_sacs[iChannel].getSpimSource()).setBoundingBoxEstimator(new BoundingBoxEstimation(CORNERS));
            //((WarpedSource<?>)registered_sacs[iChannel].getSpimSource()).setBoundingBoxEstimator(new BoundingBoxEstimation(FACES));
            //((WarpedSource<?>)registered_sacs[iChannel].getSpimSource()).setBoundingBoxEstimator(new BoundingBoxEstimation(VOLUME));
        }

        si.updateBox();
    }

    private Source<?> getModelWithGridSize(double gridSpacingInMicrometer) {

        double transform_field_subsampling = gridSpacingInMicrometer/(mp.getAtlas().getMap().getAtlasPrecisionInMillimeter()*1000.0);

        FinalInterval interval = new FinalInterval((int)(mp.nPixX/transform_field_subsampling),
                (int)(mp.nPixY/transform_field_subsampling),1);

        String unit = original_sacs[0].getSpimSource().getVoxelDimensions().unit();
        double voxX = original_sacs[0].getSpimSource().getVoxelDimensions().dimension(0);
        double voxY = original_sacs[0].getSpimSource().getVoxelDimensions().dimension(1);
        double voxZ = original_sacs[0].getSpimSource().getVoxelDimensions().dimension(2);
        FinalVoxelDimensions voxD = new FinalVoxelDimensions(unit, voxX, voxY, voxZ);

        return new IAlphaSource() {
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
                affineTransform3D.scale(mp.sizePixX*transform_field_subsampling, mp.sizePixY*transform_field_subsampling, thicknessInMm);
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
    }

    protected void popRasterDeformation() {
        registered_sacs = sourcesDeformationFieldNonRasterized.pop();
    }

    public void pushRasterSlice(double voxelSpacingInMicrometer, boolean interpolate) {
        SourceAndConverter<?>[] oriSources = registered_sacs;
        sourcesNonRasterized.push(registered_sacs);

        RealTransformSequence rts = new RealTransformSequence();
        InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();

        this.addAllRegistrations(rts, irts, getTolerance(), getMaxIteration());

        // Let's do the stuff
        // registered_sacs = this.registered_sacs_sequence.get(2).sacs; // Just to test
        Source<?> model = getModelWithGridSize(voxelSpacingInMicrometer);

        SourceAndConverter<?> modelSac = SourceAndConverterHelper.createSourceAndConverter(model);


        registered_sacs = new SourceAndConverter[nChannels]; // Compulsory or the push is useless!

        for (int iChannel = 0; iChannel < this.nChannels; iChannel++) {
            SourceResampler resampler = new SourceResampler(null, modelSac,
                    oriSources[iChannel].getSpimSource().getName()+"_raster_"+voxelSpacingInMicrometer+"_um", true, true, interpolate, 0);
            registered_sacs[iChannel] = resampler.apply(oriSources[iChannel]);
        }

        /*
        for (SourceAndConverter sac: registered_sacs) {
            mp.bindSource(sac);
            /*SourceAndConverterServices
                    .getSourceAndConverterService()
                    .register(sac);*/
        /*    if (alphaSource!=null) {
                AlphaSourceHelper.setAlphaSource(sac, alphaSource);
            }
        }*/

        si.updateBox();
    }

    public void setAlphaSources() {
        for (SourceAndConverter sac: registered_sacs) {
            SourceAndConverterServices
                    .getSourceAndConverterService()
                    .register(sac);
            if (alphaSource!=null) {
                AlphaSourceHelper.setAlphaSource(sac, alphaSource);
            }
        }
    }

    public void popRasterSlice() {
        registered_sacs = sourcesNonRasterized.pop();
    }

    public static class RegistrationAndSources {

        final Registration reg;
        final SourceAndConverter[] sacs;

        public RegistrationAndSources(Registration reg, SourceAndConverter[] sacs) {
            this.reg = reg;
            this.sacs = sacs;
        }
    }

    final SliceInterval si = new SliceInterval();

    class SliceInterval implements RealInterval {

        final RealPoint ptMin = new RealPoint(3);
        final RealPoint ptMax = new RealPoint(3);

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
                    .filter(isLeaf::contains)
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
            containsLeaf = existingIdValues.stream().anyMatch(isLeaf::contains);
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
        registered_sacs_sequence.stream().forEach(registrationAndSources -> Displaysettings.applyDisplaysettings(registrationAndSources.sacs[channelIndex],ds));
        Displaysettings.applyDisplaysettings(registered_sacs[channelIndex],ds);
        mp.converterChanged(this);
    }

    public void setDisplayColor(int channelIndex, int r, int g, int b, int a) {
        Displaysettings ds = new Displaysettings(-1);
        Displaysettings.GetDisplaySettingsFromCurrentConverter(getRegisteredSources()[channelIndex], ds);
        ds.color = new int[]{r,g,b,a};
        registered_sacs_sequence.stream().forEach(registrationAndSources -> Displaysettings.applyDisplaysettings(registrationAndSources.sacs[channelIndex],ds));
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