package ch.epfl.biop.atlas.aligner.command;

import bigwarp.landmarks.LandmarkTableModel;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.warp.DisplacementField;
import ch.epfl.biop.atlas.aligner.warp.WarpLandmarks;
import ch.epfl.biop.quicknii.QuickNIISeries;
import ch.epfl.biop.quicknii.VisuAlignWarp;
import com.google.gson.Gson;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Import Warp",
        description = "Adds a warp computed outside ABBA to one slice, as a spline registration made of landmarks placed where "
                + "they follow the warp best (see 'ABBA - Landmark Registration'). Reads displacement fields (NIfTI or MetaImage as "
                + "ITK, ANTs or elastix write them, OME-NGFF as BigWarp writes them) computed between the images 'Export Atlas to ImageJ' "
                + "(fixed) and 'Export Registered Slices to ImageJ' (moving) give for this slice, and the VisuAlign markers of a slice "
                + "imported with 'ABBA - Import QuickNII Project'. How far the spline lands from the warp is given in the outputs and kept "
                + "with the registration. One undo step.")
public class ImportWarpCommand implements Command {

    /** Name of the registrations added, shown in the timeline and saved in the state */
    public static final String REGISTRATION_NAME = "Imported Warp";

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "Slice", required = false, description = SliceIndices.DESCRIPTION + " Exactly one slice.")
    String slices_csv = "";

    @Parameter(style = "open", label = "Warp file",
            description = "A displacement field, fixed to moving: .nii or .nii.gz (ANTs *Warp.nii.gz, elastix deformationField.nii), "
                    + ".mha or .mhd, or the folder of an OME-NGFF field (.zarr, .n5). Its pixels must be those of the images ABBA exports "
                    + "for the slice at the pixel size below, in the current region of interest; only its pixel grid is used, not its origin. "
                    + "Or a VisuAlign project (.json): the markers of the slice of the same file name are used.")
    File warp_file;

    @Parameter(label = "Pixel size of the exported images (micrometers)",
            description = "Displacement fields only: the pixel size given to 'Export Atlas to ImageJ' and 'Export Registered Slices to ImageJ'.")
    double pixel_size_um = 20;

    @Parameter(type = ItemIO.OUTPUT, label = "Summary", description = "One line: the landmarks placed and how far the spline lands from the warp.")
    String summary;

    @Parameter(type = ItemIO.OUTPUT, label = "Landmarks", description = "Number of landmark pairs of the registration.")
    int landmarks;

    @Parameter(type = ItemIO.OUTPUT, label = "Median error (um)",
            description = "Median distance, at held-out points of the warp, between where the registration and the warp draw a slice point.")
    double error_median_um;

    @Parameter(type = ItemIO.OUTPUT, label = "99th percentile error (um)", description = "Its 99th percentile.")
    double error_p99_um;

    @Parameter(type = ItemIO.OUTPUT, label = "Maximum error (um)", description = "Its maximum.")
    double error_max_um;

    @Override
    public void run() {
        mp.waitForTasks();
        List<SliceSources> slices = SliceIndices.parse(mp, slices_csv);
        if (slices.size() != 1) fail("One slice needed", slices.size() + " slices selected: select one slice, or give its index");
        String format = null;
        WarpLandmarks fit = null;
        RealTransform transform = null;
        try {
            if (warp_file == null || !warp_file.exists()) throw new IOException("file not found");
            String name = warp_file.getName().toLowerCase(Locale.ROOT);
            WarpLandmarks.Warp warp;
            if (name.endsWith(".json")) {
                format = "VisuAlign";
                warp = visuAlign(slices.get(0));
            } else {
                DisplacementField field;
                if (name.endsWith(".nii") || name.endsWith(".nii.gz")) {
                    format = "NIfTI";
                    field = DisplacementField.readNifti(warp_file);
                } else if (name.endsWith(".mha") || name.endsWith(".mhd")) {
                    format = "MetaImage";
                    field = DisplacementField.readMetaImage(warp_file);
                } else if (warp_file.isDirectory()) {
                    format = "OME-NGFF";
                    field = DisplacementField.readNgff(warp_file);
                } else {
                    throw new IOException("unknown format: .nii, .nii.gz, .mha, .mhd, .json or an OME-NGFF folder");
                }
                warp = onExportGrid(field);
            }
            fit = WarpLandmarks.fit(warp, WarpLandmarks.BUDGET);
            LandmarkTableModel landmarks = new LandmarkTableModel(2);
            for (int i = 0; i < fit.fixed.length; i++) landmarks.add(fit.moving[i], fit.fixed[i]);
            transform = RegisterSlicesLandmarksCommand.spline(landmarks);
        } catch (IOException | IllegalArgumentException e) {
            fail("Cannot import the warp", warp_file + ": " + e.getMessage());
        }

        landmarks = fit.fixed.length;
        error_median_um = fit.medianMm * 1000;
        error_p99_um = fit.p99Mm * 1000;
        error_max_um = fit.maxMm * 1000;
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("warp_file", warp_file.getName());
        parameters.put("warp_format", format);
        parameters.put("landmarks", landmarks);
        parameters.put("held_out_points", fit.heldOutPoints);
        parameters.put("error_median_um", String.format(Locale.ROOT, "%.3f", error_median_um));
        parameters.put("error_p99_um", String.format(Locale.ROOT, "%.3f", error_p99_um));
        parameters.put("error_max_um", String.format(Locale.ROOT, "%.3f", error_max_um));
        RegisterSlicesLandmarksCommand.addRegistration(mp, slices, transform, REGISTRATION_NAME, parameters);
        mp.waitForTasks();
        summary = String.format(Locale.ROOT, "%s warp %s added to slice %s as %d landmarks: the registration is %.1f um from the warp "
                        + "at the median, %.1f um at the 99th percentile, %.1f um at most (%d held-out points)",
                format, warp_file.getName(), slices.get(0), landmarks, error_median_um, error_p99_um, error_max_um, fit.heldOutPoints);
    }

    /**
     * The field on the pixel grid of ABBA's exports: pixel (i, j) is centred at (x0 + i * size, y0 + j * size), (x0, y0)
     * the corner of the region of interest, as SliceToImagePlus samples them and as the DeepSlice command reads them back
     */
    WarpLandmarks.Warp onExportGrid(DisplacementField field) {
        double[] roi = mp.getROI();
        double size = pixel_size_um / 1000;
        long nx = (long) (roi[2] / size), ny = (long) (roi[3] / size);
        if (field.width != nx || field.height != ny) {
            throw new IllegalArgumentException("the field has " + field.width + " x " + field.height + " pixels, but the images "
                    + "ABBA exports at " + pixel_size_um + " um have " + nx + " x " + ny + " pixels: check the pixel size and the region of interest");
        }
        return new WarpLandmarks.Warp() {
            public double width() {
                return field.width - 1;
            }

            public double height() {
                return field.height - 1;
            }

            public void pair(double x, double y, double[] fixed, double[] moving) {
                field.apply(x, y, moving);
                fixed[0] = roi[0] + x * size;
                fixed[1] = roi[1] + y * size;
                moving[0] = roi[0] + moving[0] * size;
                moving[1] = roi[1] + moving[1] * size;
            }
        };
    }

    /**
     * VisuAlign's warp of the slice, from its markers, placed as 'ABBA - Import QuickNII Project' places the slice:
     * a pixel of the slice image (QuickNII's width x height frame) is where its anchoring puts it, flattened on the slice plane
     */
    WarpLandmarks.Warp visuAlign(SliceSources slice) throws IOException {
        QuickNIISeries series;
        try (Reader reader = new FileReader(warp_file)) {
            series = new Gson().fromJson(reader, QuickNIISeries.class);
        } catch (RuntimeException e) {
            throw new IOException("not a VisuAlign project: " + e.getMessage(), e);
        }
        if (series == null || series.slices == null) throw new IOException("not a VisuAlign project: no slices");
        List<QuickNIISeries.SliceInfo> matches = series.slices.stream()
                .filter(info -> info.filename != null && baseName(info.filename).equals(baseName(slice.getName())))
                .collect(Collectors.toList());
        if (matches.size() != 1) {
            throw new IOException(matches.size() + " slices of the project have a file name matching the slice " + slice.getName());
        }
        QuickNIISeries.SliceInfo info = matches.get(0);
        double[][] markers;
        try {
            markers = new Gson().fromJson(info.markers, double[][].class);
        } catch (RuntimeException e) {
            throw new IOException("the markers of " + info.filename + " are not lists of 4 numbers [x, y, nx, ny]", e);
        }
        if (markers == null || markers.length == 0) throw new IOException("the slice " + info.filename + " has no markers");
        for (double[] m : markers) {
            if (m.length != 4) throw new IOException("the markers of " + info.filename + " are not lists of 4 numbers [x, y, nx, ny]");
        }
        VisuAlignWarp visuAlign = new VisuAlignWarp(info.width, info.height, markers);
        AffineTransform3D toAligner = QuickNIISeries.getTransform(mp.getReslicedAtlas().ba.getName(), info, info.width, info.height)
                .preConcatenate(mp.getReslicedAtlas().getSlicingTransformToAtlas().inverse());
        return new WarpLandmarks.Warp() {
            public double width() {
                return info.width;
            }

            public double height() {
                return info.height;
            }

            public void pair(double x, double y, double[] fixed, double[] moving) {
                double[] s = visuAlign.toSlice(x, y);
                fixed[0] = toAligner.get(0, 0) * x + toAligner.get(0, 1) * y + toAligner.get(0, 3);
                fixed[1] = toAligner.get(1, 0) * x + toAligner.get(1, 1) * y + toAligner.get(1, 3);
                moving[0] = toAligner.get(0, 0) * s[0] + toAligner.get(0, 1) * s[1] + toAligner.get(0, 3);
                moving[1] = toAligner.get(1, 0) * s[0] + toAligner.get(1, 1) * s[1] + toAligner.get(1, 3);
            }
        };
    }

    /** The name without its extension */
    static String baseName(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    protected void fail(String title, String message) {
        mp.errorMessageForUser.accept(title, message);
        throw new IllegalArgumentException(title + ": " + message);
    }
}
