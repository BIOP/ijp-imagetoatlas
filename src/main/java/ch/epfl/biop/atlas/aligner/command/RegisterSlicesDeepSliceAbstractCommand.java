package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import ch.epfl.biop.atlas.aligner.LockAndRunOnceSliceAction;
import ch.epfl.biop.atlas.aligner.MoveSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.ReslicedAtlas;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import ch.epfl.biop.registration.plugin.IRegistrationPlugin;
import ch.epfl.biop.java.utilities.TempDirectory;
import ch.epfl.biop.quicknii.QuickNIIExporter;
import ch.epfl.biop.quicknii.QuickNIISeries;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.sourceandconverter.affine.AffineRegistration;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import com.google.gson.Gson;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginService;

import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ch.epfl.biop.atlas.aligner.ABBAHelper.getResource;

/**
 * Command which is using the amazing DeepSlice workflow by Harry Carey, and William Redmond, in Simon McMullan group
 * (<a href="https://doi.org/10.1038/s41467-023-41645-4">DeepSlice publication</a>)
 * Contrary to other registration methods, DeepSlice can help defining the location in Z of slices.
 * However, sometimes, DeepSlice is swapping slices incorrectly. So there is an option that maintains the
 * slices order in the process. Briefly, if this options is checked, the slice with the biggest difference
 * of rank after DeepSlice registration is moved until no difference of rank exist.
 * DeepSlice provides the fastest way to have an initial:
 * - correct positioning in Z
 * - slicing angle
 * - affine in-plane registration
 * By default, ABBA downsamples to 30 microns (mouse) or 60 microns (rat)
 * per pixel for DeepSlice and saves as an 8 bit rgb jpeg image. Also, make sure that the min max display settings
 * are set correctly, otherwise the images will be saturated upon export and will be registered badly.
 * Make sure you have multiresolution files if you don't want your downscaling to look bad! Also
 * this is currently the only registration method where the display settings matter for the registration.
 * <br>
 * There are two concrete class of this abstract class, one that can run DeepSlice locally (provided a conda env
 * exists with DeepSlice and is specified with the command
 * {@link ch.epfl.biop.wrappers.deepslice.ij2commands.DeepSlicePrefsSet}) and one that can be used in conjunction
 * with the DeepSlice web interface.
 */

@SuppressWarnings("CanBeFinal")
abstract public class RegisterSlicesDeepSliceAbstractCommand implements Command {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    public String message =
            "<html>" +
                    "<p><img src='"+getResource("graphics/DeepSlice.png")+"' width='80' height='80'></img></p>" +
                    "<b>Don't forget to adjust min/max display settings!</b> " +
                    "<br>  Almost 50% of images sent by ABBA users to DeepSlice are over-saturated. <br> " +
                    "(and thus, badly registered) </html>";

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    PlatformService ps;

    @Parameter
    Context ctx;

    @Parameter
    PluginService pluginService;

    @Parameter(choices = {"mouse", "rat"}, label = "('mouse', 'rat') Mouse or Rat ?")
    String model;

    @Parameter(label = "Slices channels, 0-based, comma separated, '*' for all channels", description = "'0,2' for channels 0 and 2")
    String channels = "*";

    @Parameter(label = "Allow change of atlas slicing angle")
    boolean allow_slicing_angle_change = true;

    @Parameter(label = "Resampling pixel size (10 for mouse, 40 for rat)", description = "To go fast, you can use 30 microns for mouse, 60 for rat")
    double px_size_micron = 10;

    boolean allow_change_slicing_position = true;
    boolean maintain_rank = true;
    boolean affine_transform = true;
    boolean convert_to_8_bits = false;
    boolean convert_to_jpg = true;
    boolean interpolate = false;
    final String image_name_prefix = "Section";
    File dataset_folder;
    BiFunction<File, Integer,File> deepSliceProcessor = null;
    QuickNIISeries series;

    protected Integer nSlicesToRegister;

    public void run() {
        try {
            mp.addTask();
            DeepSliceHelper.addJavaAtlases();

            List<SliceSources> iniList = mp.getSlices().stream().filter(SliceSources::isSelected)
                    .collect(Collectors.toList());

            // We need to reverse the order because DeepSlice expects the slices to be sorted from caudal to rostral

            List<SliceSources> slicesToRegister = new ArrayList<>();

            for (int i = iniList.size() - 1; i >= 0; i--) {
                slicesToRegister.add(iniList.get(i));
            }

            nSlicesToRegister = slicesToRegister.size();

            if (slicesToRegister.isEmpty()) {
                mp.errorMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
                return;
            }

            if (!channels.trim().equals("*")) {
                List<Integer> indices = Arrays.stream(channels.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());

                int maxIndex = indices.stream().mapToInt(e -> e).max().getAsInt();

                if (maxIndex >= mp.getChannelBoundForSelectedSlices()) {
                    mp.errorMessageForUser.accept("Missing channel in selected slice(s).",
                            "Missing channel in selected slice(s)\n One selected slice only has " + mp.getChannelBoundForSelectedSlices() + " channel(s).\n Maximum index : " + (mp.getChannelBoundForSelectedSlices() - 1));
                    return;
                }
            }

            TempDirectory td = new TempDirectory("deepslice");
            dataset_folder = td.getPath().toFile();
            td.deleteOnExit();

            if (!setSettings()) return;

            Map<SliceSources, DeepSliceHelper.Holder<Double>> newAxisPosition = new HashMap<>();
            Map<SliceSources, DeepSliceHelper.Holder<Registration<SourceAndConverter<?>[]>>> newSliceRegistration = new HashMap<>();

            for (SliceSources slice : slicesToRegister) {
                newAxisPosition.put(slice, new DeepSliceHelper.Holder<>());
                newSliceRegistration.put(slice, new DeepSliceHelper.Holder<>());
            }

            Supplier<Boolean> deepSliceRunner = () -> {

                exportDownsampledDataset(slicesToRegister);

                File deepSliceResult = deepSliceProcessor.apply(dataset_folder, nSlicesToRegister);

                if (!deepSliceResult.exists()) {
                    mp.errorMessageForUser.accept("Deep Slice registration aborted",
                            "Could not find DeepSlice result file " + deepSliceResult.getAbsolutePath()
                    );
                    return false;
                }

                // Ok, now comes the big deal. First, read json file

                try {
                    series = new Gson().fromJson(new FileReader(deepSliceResult.getAbsolutePath()), QuickNIISeries.class);
                } catch (Exception e) {
                    mp.errorMessageForUser.accept("Deep Slice Command error", "Could not parse json file " + deepSliceResult.getAbsolutePath());
                    e.printStackTrace();
                    return false;
                }

                if (series.slices.size() != slicesToRegister.size()) {
                    mp.errorMessageForUser.accept("Deep Slice Command error", "Please retry the command, DeepSlice returned less images than present in the input (" + (slicesToRegister.size() - series.slices.size()) + " missing) ! ");
                    return false;
                }

                if (allow_slicing_angle_change) {
                    //logger.debug("Slices pixel number = " + nPixX + " : " + nPixY);
                    adjustSlicingAngle(mp, series, 10, slicesToRegister); //
                }

                if (allow_change_slicing_position) {
                    adjustSlicesZPosition(mp, series, maintain_rank, slicesToRegister, newAxisPosition);
                }

                if (affine_transform) {
                    try {
                        affineTransformInPlane(ctx, mp, series, px_size_micron, slicesToRegister, newSliceRegistration);//, newSliceAffineTransformer);
                    } catch (InstantiableException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            };

            AtomicInteger counter = new AtomicInteger();
            counter.set(0);

            AtomicBoolean result = new AtomicBoolean();

            new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice : slicesToRegister) {
                new LockAndRunOnceSliceAction(mp, slice, counter, slicesToRegister.size(), deepSliceRunner, result).runRequest(true);
                if (allow_change_slicing_position) {
                    new MoveSliceAction(mp, slice, newAxisPosition.get(slice)).runRequest(true);
                }
                if (affine_transform) {
                    DeepSliceHelper.Holder<Registration<SourceAndConverter<?>[]>> regSupplier = newSliceRegistration.get(slice);
                    new RegisterSliceAction(mp, slice, regSupplier,
                            SourcesProcessorHelper.Identity(),
                            SourcesProcessorHelper.Identity(), "DeepSlice Affine").runRequest(true);
                }
            }
            new MarkActionSequenceBatchAction(mp).runRequest();
        } finally {
            mp.removeTask();
        }
    }

    abstract boolean setSettings();

    static protected void adjustSlicingAngle(MultiSlicePositioner mp, QuickNIISeries series, int nIterations, List<SliceSources> slices) {

        ReslicedAtlas reslicedAtlas = mp.getReslicedAtlas();

        double iniX = reslicedAtlas.getRotateX();
        double iniY = reslicedAtlas.getRotateY();

        List<Double> rotCorr;

        DecimalFormat df = new DecimalFormat("#0.0");

        // Knowing which direction to get the atlas slicing right is kind of annoying. So I compute the derivative and go in the right direction
        // I want to apply cx and cy
        // reslicedAtlas.setRotateX((mp.getReslicedAtlas().getRotateX() + cx);
        // reslicedAtlas.setRotateY((mp.getReslicedAtlas().getRotateY() + cy);
        // such as
        // getRotDXDY (rx, ry) tends to (0,0)
        // let's compute drx/dcorx, drx/dcory, dry/dcorx, dry/dcory
        double dangle = 0.05; // radians, used to compute the derivative
        double drxdcx, drxdcy, drydcx, drydcy;

        reslicedAtlas.setRotateX(iniX);
        reslicedAtlas.setRotateY(iniY);
        rotCorr = getRotDXRotDY(mp,series,slices);
        double iniCx = rotCorr.get(0);
        double iniCy = rotCorr.get(1);
        reslicedAtlas.setRotateX(iniX + dangle);
        reslicedAtlas.setRotateY(iniY);
        rotCorr = getRotDXRotDY(mp,series,slices);
        drxdcx = (rotCorr.get(0)-iniCx)/dangle;
        drydcx = (rotCorr.get(1)-iniCy)/dangle;
        reslicedAtlas.setRotateX(iniX);
        reslicedAtlas.setRotateY(iniY+dangle);
        rotCorr = getRotDXRotDY(mp,series,slices);
        drxdcy = (rotCorr.get(0)-iniCx)/dangle;
        drydcy = (rotCorr.get(1)-iniCy)/dangle;
        reslicedAtlas.setRotateX(iniX);
        reslicedAtlas.setRotateY(iniY);

        // Because right now we have:
        // drx = drx/dcx * dcx + drx/dcy * dcy
        // dry = dry/dcx * dcx + dry/dcy * dcy
        // We have to inverse the matrix because we want to find dcx and dcy such as drx = -rx and dry = -ry
        double det = drxdcx*drydcy - drxdcy*drydcx;
        double m00 = drydcy/det;
        double m10 = -drydcx/det;
        double m11 = drxdcx / det;
        double m01 = -drxdcy/det;

        for (int nAdjust = 0;nAdjust<nIterations;nAdjust++) { // Iterative rotation adjustment

            rotCorr = getRotDXRotDY(mp,series,slices);
            double cx = rotCorr.get(0);
            double cy = rotCorr.get(1);
            // We want to nullify cx and cy
            double dx = - m00*cx - m10*cy;
            double dy = - m01*cx - m11*cy;

            reslicedAtlas.setRotateX((reslicedAtlas.getRotateX() + dx));
            reslicedAtlas.setRotateY((reslicedAtlas.getRotateY() + dy));

        }

        String angleUpdatedMessage = "";

        angleUpdatedMessage+="Angle X : "+df.format(reslicedAtlas.getRotateX()/Math.PI*180)+" deg\n ";
        angleUpdatedMessage+="Angle Y : "+df.format(reslicedAtlas.getRotateY()/Math.PI*180)+" deg\n";

        mp.infoMessageForUser.accept("Slicing angle changed", "Slicing angle adjusted. "+ angleUpdatedMessage);
        mp.infoMessageForUser.accept("Angle spread (ignored)", "Range X: ["+df.format(rotCorr.get(2))+":"+df.format(rotCorr.get(3))+"]");
        mp.infoMessageForUser.accept("Angle spread (ignored)", "Range Y: ["+df.format(rotCorr.get(3))+":"+df.format(rotCorr.get(4))+"]");

    }

    private static List<Double> getRotDXRotDY(MultiSlicePositioner mp, QuickNIISeries series, List<SliceSources> slices) {

        AffineTransform3D toABBA = mp.getReslicedAtlas().getSlicingTransformToAtlas().inverse();

        // Transform sources according to anchoring
        double[] rxs = new double[slices.size()];
        double[] rys = new double[slices.size()];

        for (int i = 0; i < slices.size(); i++) {
            QuickNIISeries.SliceInfo slice = series.slices.get(i);

            AffineTransform3D toCCFv3 = QuickNIISeries.getTransform(mp.getReslicedAtlas().ba.getName(), slice, slice.width, slice.height);

            AffineTransform3D nonFlat = toCCFv3.preConcatenate(toABBA);

            // Get the z vector to measure the angle of rotation compared to the actual one

            double zx = nonFlat.get(2, 0);
            double zy = nonFlat.get(2, 1);
            double zz = nonFlat.get(2, 2);

            double zNorm = Math.sqrt(zx * zx + zy * zy + zz * zz);

            zx /= zNorm;
            zy /= zNorm;
            zz /= zNorm;

            double ry = Math.asin(zx);
            double rx = Math.asin(zy);

            rxs[i] = rx;
            rys[i] = ry;

        }

        List<Double> ans = new ArrayList<>();
        ans.add(DeepSliceHelper.getMedian(rxs));
        ans.add(DeepSliceHelper.getMedian(rys));
        ans.add(Arrays.stream(rxs).min().getAsDouble()/Math.PI*180);
        ans.add(Arrays.stream(rxs).max().getAsDouble()/Math.PI*180);
        ans.add(Arrays.stream(rys).min().getAsDouble()/Math.PI*180);
        ans.add(Arrays.stream(rys).max().getAsDouble()/Math.PI*180);
        return ans;
    }

    static protected void adjustSlicesZPosition(MultiSlicePositioner mp, QuickNIISeries series, boolean maintain_rank, final List<SliceSources> slices, Map<SliceSources, DeepSliceHelper.Holder<Double>> newAxisPosition) {
        // The "slices" list is sorted according to the z axis, before deepslice action

        final String regex = "(.*)"+"_s([0-9]+).*";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

        Map<SliceSources, Double> slicesNewPosition = new HashMap<>();

        AffineTransform3D toABBA = mp.getReslicedAtlas().getSlicingTransformToAtlas().inverse();
        for (int i = 0; i < slices.size(); i++) {
            QuickNIISeries.SliceInfo slice = series.slices.get(i);

            final Matcher matcher = pattern.matcher(slice.filename);

            matcher.find();

            int iSliceSource = Integer.parseInt(matcher.group(2));

            //logger.debug("Slice QuickNii "+i+" correspond to initial slice "+iSliceSource);

            AffineTransform3D toCCFv3 = QuickNIISeries.getTransform(mp.getReslicedAtlas().ba.getName(), slice, slice.width, slice.height);

            AffineTransform3D nonFlat = toCCFv3.preConcatenate(toABBA);

            double zLocation = nonFlat.get(2,3);

            slicesNewPosition.put(slices.get(iSliceSource), zLocation);
        }

        Map<Integer, SliceSources> mapNewRankToSlices = new HashMap<>();
        if (maintain_rank) {
            // We should swap the position of the one slice with the biggest rank difference until there's no rank difference
            int biggestRankDifference = -1;
            int indexOfSliceWithBiggestRankDifference = -1;
            int targetIndex = -1;
            int direction = 0;

            while (biggestRankDifference!=0) {
                Integer[] indicesNewlyOrdered = new Integer[slices.size()];

                for (int i = 0; i < indicesNewlyOrdered.length; i++) {
                    indicesNewlyOrdered[i] = i;
                }

                Arrays.sort(indicesNewlyOrdered, Comparator.comparingDouble(i -> -1 * slicesNewPosition.get(slices.get(i)))); // -1 for caudal to rostral convention

                for (int i = 0; i < indicesNewlyOrdered.length; i++) {
                    mapNewRankToSlices.put(i,slices.get(indicesNewlyOrdered[i]));
                }
                biggestRankDifference = 0;
                for (int i = 0; i < indicesNewlyOrdered.length; i++) {
                    int abs = Math.abs(i - indicesNewlyOrdered[i]);
                    if (abs > biggestRankDifference) {
                        biggestRankDifference = abs;
                        indexOfSliceWithBiggestRankDifference = indicesNewlyOrdered[i];
                        targetIndex = indicesNewlyOrdered[i];
                        direction = i - indicesNewlyOrdered[i];
                    }
                }
                if (biggestRankDifference!=0) { // Why move anything if everything is alright ?
                    // Moving slice indexOfSliceWithBiggestRankDifference to a new rank targetIndex
                    double targetLocation = slicesNewPosition.get(mapNewRankToSlices.get(targetIndex)); // NPE !!
                    if (direction < 0) targetLocation -= mp.getAtlas().getMap().getAtlasPrecisionInMillimeter()/10.0;
                    if (direction > 0) targetLocation += mp.getAtlas().getMap().getAtlasPrecisionInMillimeter()/10.0;
                    slicesNewPosition.put(slices.get(indexOfSliceWithBiggestRankDifference), targetLocation);
                }
            }

        }

        for (SliceSources slice : slices) {
            newAxisPosition.get(slice).accept(slicesNewPosition.get(slice));
        }
    }

    static protected void  affineTransformInPlane(Context ctx,
                                                  MultiSlicePositioner mp,
                                                  QuickNIISeries series,
                                                  double px_size_micron,
                                                  final List<SliceSources> slices,
                                                  Map<SliceSources, DeepSliceHelper.Holder<Registration<SourceAndConverter<?>[]>>> newSliceTransform) throws InstantiableException {
        AffineTransform3D toABBA = mp.getReslicedAtlas().getSlicingTransformToAtlas().inverse();

        // Transform sources according to anchoring
        final String regex = "(.*)"+"_s([0-9]+).*";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

        for (int i = 0; i < slices.size(); i++) {
            QuickNIISeries.SliceInfo slice = series.slices.get(i);

            AffineTransform3D toCCF = QuickNIISeries.getTransform(mp.getReslicedAtlas().ba.getName(), slice, slice.width, slice.height);

            AffineTransform3D flat = toCCF.preConcatenate(toABBA);

            // Removes any z transformation -> in plane transformation
            flat.set(0,2,0);
            flat.set(0,2,1);
            flat.set(1,2,2);
            flat.set(0,2,3);

            // flat gives the good registration result for an image which is located at 0,0,0, and
            // which has a pixel size of 1
            // We need to transform the original image this way

            AffineTransform3D preTransform = new AffineTransform3D();
            preTransform.scale(1000.0/px_size_micron);
            preTransform.set(1,2,2);
            preTransform.set(-1000.0/px_size_micron*mp.getROI()[0], 0, 3);
            preTransform.set(-1000.0/px_size_micron*mp.getROI()[1], 1, 3);

            // if pixel size micron is 1 -> scaling factor = 1000
            // if pixel size is 1000 micron -> scaling factor = 1

            final Matcher matcher = pattern.matcher(slice.filename);

            matcher.find();

            int iSliceSource = Integer.parseInt(matcher.group(2));

            //logger.debug("Slice QuickNii "+i+" correspond to initial slice "+iSliceSource);

            IRegistrationPlugin registration = (IRegistrationPlugin)
                    ctx.getService(PluginService.class).getPlugin(AffineRegistration.class).createInstance();
            registration.setScijavaContext(ctx);
            Map<String,Object> parameters = new HashMap<>();

            AffineTransform3D inPlaneTransform = new AffineTransform3D();
            inPlaneTransform.set(flat);
            inPlaneTransform.concatenate(preTransform);

            parameters.put("transform", AffineRegistration.affineTransform3DToString(inPlaneTransform));
            // Always set slice at zero position for registration
            parameters.put("pz", 0);
            AffineTransform3D at3d = new AffineTransform3D();
            at3d.translate(0,0,-slices.get(iSliceSource).getSlicingAxisPosition());

            // Sends parameters to the registration
            registration.setRegistrationParameters(MultiSlicePositioner.convertToString(ctx,parameters));

            newSliceTransform.get(slices.get(iSliceSource)).accept( registration );

        }

    }

    protected void exportDownsampledDataset(List<SliceSources> slices) {

        // todo: inverser les slices pour deepslice (ordre ros) (with caudal to rostral being positive)!!

        SourcesProcessor preprocess = SourcesProcessorHelper.Identity();

        if (!channels.trim().equals("*")) {
            List<Integer> indices = Arrays.stream(channels.trim().split(",")).mapToInt(Integer::parseInt).boxed().collect(Collectors.toList());
            preprocess = new SourcesChannelsSelect(indices);
        }

        try {

            List<File> filePaths = QuickNIIExporter.builder()
                    .roi(mp.getROI())
                    .cvt8bits(convert_to_8_bits)
                    .jpeg(convert_to_jpg)
                    .setProcessor(preprocess)
                    .slices(slices)
                    .name(image_name_prefix)
                    .folder(dataset_folder)
                    .pixelSizeMicron(px_size_micron)
                    .interpolate(interpolate)
                    .create()
                    .export();

            double ratioSaturatedPixelValueThreshold = 0.02; // Should not have saturated pixel in more than 5% of the pixel
            String message = "";
            int iSaturatedCounter = 0;
            for (int i = 0; i< filePaths.size(); i++) {
                double saturationForImage = calculateSaturatedPixelRatio(IJ.openImage(filePaths.get(i).getAbsolutePath()));
                if (saturationForImage > ratioSaturatedPixelValueThreshold) {
                    message += slices.get(i).getName()+" is saturated above "+((int)(ratioSaturatedPixelValueThreshold*100))+" % ("+((int)(saturationForImage*100))+"  %) \n";
                    iSaturatedCounter++;
                }
                if (iSaturatedCounter>20) {
                    message += "...";
                    break;
                }
            }

            if (!message.isEmpty()) {
                mp.warningMessageForUser.accept("Slices saturated!", "DeepSlice will run but results won't be optimal. Please change display settings to avoid saturation: \n"+message);
            }

        } catch (Exception e) {
            mp.errorMessageForUser.accept("Export to Quick NII dataset error. ", e.getMessage());
        }
    }

    // See https://github.com/BIOP/ijp-imagetoatlas/issues/164
    public static double calculateSaturatedPixelRatio(ImagePlus image) {
        if (!(image.getProcessor() instanceof ColorProcessor)) {
            return 0;
        }
        ColorProcessor processor = (ColorProcessor) image.getProcessor();
        int width = processor.getWidth();
        int height = processor.getHeight();
        int saturatedPixelCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = processor.getPixel(x, y);
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                if (red == 255 || green == 255 || blue == 255) {
                    saturatedPixelCount++;
                }
            }
        }

        double totalPixels = width * height;
        return (double) saturatedPixelCount / totalPixels;
    }

}
