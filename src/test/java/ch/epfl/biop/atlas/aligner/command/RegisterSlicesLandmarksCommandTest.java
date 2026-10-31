package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.source.spline.PrecomputedSplineRegistration;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import net.imagej.ImageJ;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.RealTransform;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import static bdv.util.RealTransformHelper.BigWarpFileFromRealTransform;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests of 'ABBA - Landmark Registration'. The tests of the landmarks file need nothing else; the round trip in a session
 * needs the Allen mouse atlas, downloaded on first use, and is skipped by default. Enable it with
 * {@code mvn test -Dtest=RegisterSlicesLandmarksCommandTest -Dabba.atlas.test=true}.
 */
public class RegisterSlicesLandmarksCommandTest {

    /** Moving (slice) x, y, then fixed (atlas) x, y, in mm, around the centre of the default registration region */
    static final double[][] LANDMARKS = {
            {4.0, 3.0, 4.1, 3.2},
            {7.0, 3.5, 7.3, 3.4},
            {6.0, 6.0, 5.8, 6.3},
            {3.5, 5.5, 3.4, 5.5},
            {5.5, 4.5, 5.6, 4.4}};

    static File writeCsv(double[][] landmarks, boolean lastIsActive) throws IOException {
        File file = Files.createTempFile("landmarks", ".csv").toFile();
        file.deleteOnExit();
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < landmarks.length; i++) {
            csv.append(String.format(Locale.ROOT, "\"Pt-%d\",\"%s\"", i, (i < landmarks.length - 1 || lastIsActive)));
            for (double v : landmarks[i]) csv.append(String.format(Locale.ROOT, ",\"%s\"", v));
            csv.append("\n");
        }
        Files.write(file.toPath(), csv.toString().getBytes());
        return file;
    }

    static RealTransform splineOf(File file) {
        return RegisterSlicesLandmarksCommand.spline(RegisterSlicesLandmarksCommand.readLandmarks(file));
    }

    static double[] apply(RealTransform transform, double x, double y) {
        double[] p = {x, y, 0};
        transform.apply(p, p);
        return p;
    }

    static void assertMapsFixedToMoving(RealTransform transform, double[][] landmarks) {
        for (double[] l : landmarks) {
            double[] p = apply(transform, l[2], l[3]);
            assertEquals(l[0], p[0], 1e-9);
            assertEquals(l[1], p[1], 1e-9);
            assertEquals(0, p[2], 1e-12);
        }
    }

    @Test
    public void landmarksMapFixedPointsToMovingPoints() throws IOException {
        RealTransform transform = splineOf(writeCsv(LANDMARKS, true));
        assertMapsFixedToMoving(transform, LANDMARKS);
    }

    @Test
    public void inactiveLandmarksAreIgnored() throws IOException {
        RealTransform withInactive = splineOf(writeCsv(LANDMARKS, false));
        double[][] active = new double[LANDMARKS.length - 1][];
        System.arraycopy(LANDMARKS, 0, active, 0, active.length);
        RealTransform withoutLast = splineOf(writeCsv(active, true));
        double[] a = apply(withInactive, 5.0, 5.0), b = apply(withoutLast, 5.0, 5.0);
        assertEquals(b[0], a[0], 1e-12);
        assertEquals(b[1], a[1], 1e-12);
    }

    /** The landmarks ABBA gives BigWarp when a spline registration is edited, read back, give the same transform */
    @Test
    public void landmarksOfTheBigWarpEditionReadBack() throws IOException {
        RealTransform transform = splineOf(writeCsv(LANDMARKS, true));
        File exported = new File(BigWarpFileFromRealTransform(transform));
        RealTransform readBack = splineOf(exported);
        assertMapsFixedToMoving(readBack, LANDMARKS);
        for (double x = 2; x <= 8; x += 1.5) {
            for (double y = 2; y <= 7; y += 1.5) {
                double[] a = apply(transform, x, y), b = apply(readBack, x, y);
                assertEquals(a[0], b[0], 1e-9);
                assertEquals(a[1], b[1], 1e-9);
            }
        }
    }

    @Test
    public void unusableFilesAreRefused() throws IOException {
        File threeD = Files.createTempFile("landmarks3d", ".csv").toFile();
        threeD.deleteOnExit();
        Files.write(threeD.toPath(), "\"Pt-0\",\"true\",\"1\",\"2\",\"3\",\"1\",\"2\",\"3\"\n".getBytes());
        File header = Files.createTempFile("landmarks-header", ".csv").toFile();
        header.deleteOnExit();
        Files.write(header.toPath(), "name,active,mx,my,fx,fy\n\"Pt-0\",\"true\",\"1\",\"2\",\"3\",\"4\"\n".getBytes());
        double[][] two = {LANDMARKS[0], LANDMARKS[1]};
        for (File file : new File[]{threeD, header, writeCsv(two, true), new File("missing.csv")}) {
            try {
                RegisterSlicesLandmarksCommand.readLandmarks(file);
                fail("No refusal for " + file.getName());
            } catch (IllegalArgumentException e) {
                assertNotNull(e.getMessage());
            }
        }
    }

    /**
     * In a session: the command adds one registration to the slice, which maps the fixed landmarks to the moving ones,
     * is undone and redone in one step, and is the same after the state is saved and loaded again. A refusal is
     * recorded in the failures of the session.
     */
    @Test
    public void roundTripInASession() throws Exception {
        Assume.assumeTrue("set -Dabba.atlas.test=true to run this test", Boolean.getBoolean("abba.atlas.test"));
        ImageJ ij = new ImageJ();
        try {
            Atlas atlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");
            File folder = Files.createTempDirectory("abba-landmarks").toFile();
            MultiSlicePositioner mp = newSessionWithOneSlice(ij, atlas, folder);
            SliceSources slice = mp.getSlices().get(0);
            int before = slice.getNumberOfRegistrations();

            ij.command().run(RegisterSlicesLandmarksCommand.class, true, "mp", mp, "slices_csv", "0",
                    "landmarks_file", writeCsv(LANDMARKS, true)).get();
            assertEquals(before + 1, slice.getNumberOfRegistrations());
            Registration<?> registration = lastRegistration(mp, slice);
            assertEquals(PrecomputedSplineRegistration.class, registration.getClass());
            assertEquals(RegisterSlicesLandmarksCommand.REGISTRATION_NAME, registration.getRegistrationName());
            assertRegistrationMapsFixedToMoving(registration);

            mp.cancelLastAction();
            mp.waitForTasks();
            assertEquals(before, slice.getNumberOfRegistrations());
            mp.redoAction();
            mp.waitForTasks();
            assertEquals(before + 1, slice.getNumberOfRegistrations());

            File state = new File(folder, "state.abba");
            assertTrue(mp.saveState(state, true));
            MultiSlicePositioner reloaded = newSession(ij, atlas);
            assertTrue(reloaded.loadState(state));
            reloaded.waitForTasks();
            SliceSources reloadedSlice = reloaded.getSlices().get(0);
            assertEquals(slice.getRegistrationNames(), reloadedSlice.getRegistrationNames());
            Registration<?> reloadedRegistration = lastRegistration(reloaded, reloadedSlice);
            assertEquals(PrecomputedSplineRegistration.class, reloadedRegistration.getClass());
            assertRegistrationMapsFixedToMoving(reloadedRegistration);

            int failures = mp.getFailures(0).size();
            try {
                ij.command().run(RegisterSlicesLandmarksCommand.class, true, "mp", mp, "slices_csv", "0",
                        "landmarks_file", new File("missing.csv")).get();
                fail("A missing file was accepted");
            } catch (ExecutionException e) {
                assertEquals(failures + 1, mp.getFailures(0).size());
            }
            assertEquals(before + 1, slice.getNumberOfRegistrations());
        } finally {
            ij.dispose();
        }
    }

    static MultiSlicePositioner newSession(ImageJ ij, Atlas atlas) throws Exception {
        return (MultiSlicePositioner) ij.command().run(ABBAStartCommand.class, true,
                "x_axis", "RL", "y_axis", "SI", "z_axis", "AP", "ba", atlas).get().getOutput("mp");
    }

    /** A session with one slice, read from a file so that the state can be saved and loaded again */
    static MultiSlicePositioner newSessionWithOneSlice(ImageJ ij, Atlas atlas, File folder) throws Exception {
        MultiSlicePositioner mp = newSession(ij, atlas);
        ImagePlus image = new ImagePlus("section", new ByteProcessor(400, 300));
        image.getCalibration().pixelWidth = 20;
        image.getCalibration().pixelHeight = 20;
        image.getCalibration().setUnit("micron");
        File file = new File(folder, "section.tif");
        assertTrue(IJ.saveAsTiff(image, file.getAbsolutePath()));
        ij.command().run(ImportSlicesFromFilesCommand.class, true, "mp", mp, "datasetname", "sections", "files", new File[]{file},
                "split_rgb_channels", false, "first_slice_position_mm", 6.0, "slice_spacing_mm", 0.1).get();
        mp.waitForTasks();
        return mp;
    }

    static Registration<?> lastRegistration(MultiSlicePositioner mp, SliceSources slice) {
        List<CancelableAction> actions = mp.getActionsFromSlice(slice);
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i) instanceof RegisterSliceAction) return ((RegisterSliceAction) actions.get(i)).getRegistration();
        }
        return null;
    }

    static void assertRegistrationMapsFixedToMoving(Registration<?> registration) {
        for (double[] l : LANDMARKS) {
            RealPointList moving = registration.getTransformedPtsFixedToMoving(
                    new RealPointList(Collections.singletonList(new RealPoint(l[2], l[3]))));
            assertEquals(l[0], moving.ptList.get(0).getDoublePosition(0), 1e-9);
            assertEquals(l[1], moving.ptList.get(0).getDoublePosition(1), 1e-9);
        }
    }
}
