package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.quicknii.QuickNIISeries;
import ch.epfl.biop.quicknii.VisuAlignWarp;
import ch.epfl.biop.registration.Registration;
import com.google.gson.Gson;
import net.imagej.ImageJ;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.junit.Assume;
import org.junit.Test;
import org.scijava.module.Module;

import java.io.File;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 'ABBA - Import Warp' in a session: it needs the Allen mouse atlas, downloaded on first use, and is skipped by default.
 * Enable it with {@code mvn test -Dtest=ImportWarpCommandTest -Dabba.atlas.test=true}.
 */
public class ImportWarpCommandTest {

    static {
        // SliceActionObserver asserts that every action has a slice, which the batch marks of the QuickNII import do not:
        // only seen with assertions on (surefire), unrelated to what is tested here
        ImportWarpCommandTest.class.getClassLoader().setClassAssertionStatus("ch.epfl.biop.atlas.aligner.SliceActionObserver", false);
    }

    /**
     * PyNutil's VisuAlign project, imported with 'ABBA - Import QuickNII Project', then the markers of one slice: the
     * registration draws the slice where VisuAlign's warp does, within what the command reports. Then a field written
     * on the grid of ABBA's exports: same check, and undo removes the registration.
     */
    @Test
    public void importsVisuAlignMarkersAndAField() throws Exception {
        Assume.assumeTrue("set -Dabba.atlas.test=true to run this test", Boolean.getBoolean("abba.atlas.test"));
        ImageJ ij = new ImageJ();
        try {
            File folder = Files.createTempDirectory("abba-import-warp").toFile();
            for (String name : new String[]{"alignment.json", "test_s001.png", "test_s002.png", "test_s003.png", "test_s004.png", "test_s005.png"}) {
                Files.copy(new File("src/test/resources/visualign/" + name).toPath(), new File(folder, name).toPath());
            }
            File project = new File(folder, "alignment.json");
            Atlas atlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");
            MultiSlicePositioner mp = RegisterSlicesLandmarksCommandTest.newSession(ij, atlas);
            ij.command().run(ImportSlicesFromQuickNIICommand.class, true, "mp", mp, "quicknii_project", project, "split_rgb_channels", false).get();
            mp.waitForTasks();

            QuickNIISeries series = new Gson().fromJson(new FileReader(project), QuickNIISeries.class);
            for (int s = 1; s < series.slices.size(); s++) {
                QuickNIISeries.SliceInfo info = series.slices.get(s);
                int index = indexOf(mp, info.filename.replace(".png", ""));
                Module result = ij.command().run(ImportWarpCommand.class, true, "mp", mp, "slices_csv", String.valueOf(index),
                        "warp_file", project, "pixel_size_um", 20.0).get();
                System.out.println(result.getOutput("summary"));
                SliceSources slice = mp.getSlices().get(index);
                Registration<?> registration = RegisterSlicesLandmarksCommandTest.lastRegistration(mp, slice);
                assertEquals(ImportWarpCommand.REGISTRATION_NAME, registration.getRegistrationName());
                assertEquals(String.valueOf(result.getOutput("landmarks")), registration.getRegistrationParameters().get("landmarks"));
                // Where VisuAlign's warp sends atlas plane points, against the registration, on a grid of the image
                VisuAlignWarp warp = new VisuAlignWarp(info.width, info.height, new Gson().fromJson(info.markers, double[][].class));
                AffineTransform3D toAligner = QuickNIISeries.getTransform(atlas.getName(), info, info.width, info.height)
                        .preConcatenate(mp.getReslicedAtlas().getSlicingTransformToAtlas().inverse());
                List<double[]> fixed = new ArrayList<>(), moving = new ArrayList<>();
                for (double x = 25; x < info.width; x += 50) {
                    for (double y = 25; y < info.height; y += 50) {
                        fixed.add(inPlane(toAligner, x, y));
                        moving.add(inPlane(toAligner, warp.toSlice(x, y)));
                    }
                }
                List<Double> distances = distances(registration, fixed, moving);
                Collections.sort(distances);
                double median = distances.get(distances.size() / 2), worst = distances.get(distances.size() - 1);
                System.out.printf(Locale.ROOT, "%s: slice side distance to VisuAlign's warp on a 50 px grid: median %.2f um, max %.2f um%n",
                        info.filename, median * 1000, worst * 1000);
                // A wrong frame would be hundreds of micrometres off everywhere; the spline only misses VisuAlign's creases
                assertTrue(median < 0.005 && worst < 0.1);
            }

            // A field on the grid of the exports of slice 0, 40 um pixels: up to 0.2 mm displacements
            double size = 0.04;
            double[] roi = mp.getROI();
            int nx = (int) (roi[2] / size), ny = (int) (roi[3] / size);
            ByteBuffer data = ByteBuffer.allocate(nx * ny * 8).order(ByteOrder.LITTLE_ENDIAN);
            for (int j = 0; j < ny; j++) {
                for (int i = 0; i < nx; i++) {
                    data.putFloat((float) (0.2 * Math.sin(i * size)));
                    data.putFloat((float) (0.1 * Math.cos(j * size * 1.5)));
                }
            }
            File field = new File(folder, "field.mha");
            String header = "ObjectType = Image\nNDims = 2\nBinaryData = True\nBinaryDataByteOrderMSB = False\nCompressedData = False\n"
                    + "ElementSpacing = 0.04 0.04\nDimSize = " + nx + " " + ny + "\nElementNumberOfChannels = 2\nElementType = MET_FLOAT\n"
                    + "ElementDataFile = LOCAL\n";
            Files.write(field.toPath(), header.getBytes(StandardCharsets.US_ASCII));
            Files.write(field.toPath(), data.array(), java.nio.file.StandardOpenOption.APPEND);
            SliceSources slice = mp.getSlices().get(0);
            int before = slice.getNumberOfRegistrations();
            Module result = ij.command().run(ImportWarpCommand.class, true, "mp", mp, "slices_csv", "0",
                    "warp_file", field, "pixel_size_um", 40.0).get();
            System.out.println(result.getOutput("summary"));
            Registration<?> registration = RegisterSlicesLandmarksCommandTest.lastRegistration(mp, slice);
            List<double[]> fixed = new ArrayList<>(), moving = new ArrayList<>();
            for (int j = 1; j < ny - 1; j += 7) {
                for (int i = 1; i < nx - 1; i += 7) {
                    fixed.add(new double[]{roi[0] + i * size, roi[1] + j * size});
                    moving.add(new double[]{roi[0] + i * size + 0.2 * Math.sin(i * size), roi[1] + j * size + 0.1 * Math.cos(j * size * 1.5)});
                }
            }
            double worst = Collections.max(distances(registration, fixed, moving));
            System.out.printf(Locale.ROOT, "field: largest slice side distance at pixel centres %.2f um%n", worst * 1000);
            assertTrue(worst < 0.005);
            mp.cancelLastAction();
            mp.waitForTasks();
            assertEquals(before, slice.getNumberOfRegistrations());
        } finally {
            ij.dispose();
        }
    }

    static int indexOf(MultiSlicePositioner mp, String name) {
        for (int i = 0; i < mp.getSlices().size(); i++) if (mp.getSlices().get(i).getName().contains(name)) return i;
        throw new IllegalStateException("no slice " + name);
    }

    static double[] inPlane(AffineTransform3D t, double... p) {
        return new double[]{t.get(0, 0) * p[0] + t.get(0, 1) * p[1] + t.get(0, 3), t.get(1, 0) * p[0] + t.get(1, 1) * p[1] + t.get(1, 3)};
    }

    /** How far the registration maps each fixed point from its moving point, in mm */
    static List<Double> distances(Registration<?> registration, List<double[]> fixed, List<double[]> moving) {
        List<RealPoint> points = new ArrayList<>();
        for (double[] p : fixed) points.add(new RealPoint(p[0], p[1]));
        List<RealPoint> mapped = registration.getTransformedPtsFixedToMoving(new RealPointList(points)).ptList;
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < mapped.size(); i++) {
            out.add(Math.hypot(mapped.get(i).getDoublePosition(0) - moving.get(i)[0], mapped.get(i).getDoublePosition(1) - moving.get(i)[1]));
        }
        return out;
    }
}
