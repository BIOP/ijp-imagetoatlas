package ch.epfl.biop.atlas.aligner.warp;

import bigwarp.transforms.NgffTransformations;
import ch.epfl.biop.quicknii.QuickNIISeries;
import ch.epfl.biop.quicknii.VisuAlignWarp;
import com.google.gson.Gson;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.RawCompression;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.Axis;
import org.janelia.saalfeldlab.n5.universe.metadata.axes.CoordinateSystem;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Readers of displacement fields, VisuAlign's warp and the landmarks that follow a warp. See src/test/resources/warp and visualign. */
public class WarpTest {

    static final String WARP = "src/test/resources/warp/", VISUALIGN = "src/test/resources/visualign/";

    /** Each file read maps the points as ANTs itself mapped them (points_ants.csv), through the field's own geometry */
    @Test
    public void niftiAndMetaImageFieldsMapPointsAsAnts() throws Exception {
        double c = Math.cos(0.3), s = Math.sin(0.3);
        double[] l = {0.5 * c, -0.4 * s, 0.5 * s, 0.4 * c}; // index to LPS millimetres, origin (10, -5): see README.txt
        double det = l[0] * l[3] - l[1] * l[2];
        for (DisplacementField field : new DisplacementField[]{DisplacementField.readNifti(new File(WARP + "o_1Warp.nii.gz")),
                DisplacementField.readMetaImage(new File(WARP + "warp.mha"))}) {
            List<String> rows = Files.readAllLines(new File(WARP + "points_ants.csv").toPath());
            for (String row : rows.subList(1, rows.size())) {
                String[] v = row.split(",");
                double px = Double.parseDouble(v[2]) - 10, py = Double.parseDouble(v[3]) + 5;
                double[] q = new double[2];
                field.apply((l[3] * px - l[1] * py) / det, (-l[2] * px + l[0] * py) / det, q);
                assertEquals(Double.parseDouble(v[4]), 10 + l[0] * q[0] + l[1] * q[1], 1e-5);
                assertEquals(Double.parseDouble(v[5]), -5 + l[2] * q[0] + l[3] * q[1], 1e-5);
            }
        }
    }

    /** A field written by BigWarp, read back: displacements in pixels of the field */
    @Test
    public void ngffFieldWrittenByBigWarpReadsBack() throws Exception {
        ArrayImg<FloatType, ?> img = ArrayImgs.floats(2, 40, 30); // vector axis first, as BigWarp writes it
        net.imglib2.RandomAccess<FloatType> access = img.randomAccess();
        for (int j = 0; j < 30; j++) {
            for (int i = 0; i < 40; i++) {
                access.setPosition(new long[]{0, i, j});
                access.get().set(0.01f * i);
                access.setPosition(new long[]{1, i, j});
                access.get().set(-0.05f);
            }
        }
        File folder = new File(Files.createTempDirectory("ngff").toFile(), "field.zarr");
        ExecutorService exec = Executors.newSingleThreadExecutor();
        try (N5Writer n5 = new N5Factory().openWriter(folder.getAbsolutePath())) {
            CoordinateSystem in = new CoordinateSystem("input", Axis.space("mm", "x", "y"));
            CoordinateSystem out = new CoordinateSystem("output", Axis.space("mm", "x", "y"));
            NgffTransformations.addCoordinateTransformations(n5, "/", NgffTransformations.saveDisplacementFieldNgff(n5, "dfield", "/",
                    in, out, img, new double[]{0.5, 0.25}, new double[]{3, 4}, new int[]{2, 40, 30}, new RawCompression(), exec));
        } finally {
            exec.shutdown();
        }
        DisplacementField field = DisplacementField.readNgff(folder);
        double[] q = new double[2];
        field.apply(10, 10, q);
        assertEquals(40, field.width);
        assertEquals(30, field.height);
        assertEquals(10 + 0.1 / 0.5, q[0], 1e-6);
        assertEquals(10 - 0.05 / 0.25, q[1], 1e-6);
    }

    /** The landmarks of a strong smooth warp (up to 0.3 mm) follow it within 20 um, at points not used to place them */
    @Test
    public void landmarksFollowASmoothWarp() {
        WarpLandmarks fit = WarpLandmarks.fit(new WarpLandmarks.Warp() {
            public double width() {
                return 500;
            }

            public double height() {
                return 400;
            }

            public void pair(double x, double y, double[] fixed, double[] moving) {
                fixed[0] = 1 + 0.02 * x;
                fixed[1] = -2 + 0.02 * y;
                moving[0] = fixed[0] + 0.3 * Math.sin(fixed[0] * 2) * Math.cos(fixed[1]);
                moving[1] = fixed[1] + 0.2 * Math.sin(fixed[1] * 2.5 + fixed[0]);
            }
        }, 417);
        // 289 start pairs alone miss by up to 39 um; two rounds of added pairs bring it to 11 um
        assertTrue(fit.fixed.length <= 417);
        assertTrue("max error " + fit.maxMm, fit.maxMm < 0.02);
    }

    /** VisuAlign's warp is the one PyNutil computes (expected.csv, generate_expected.py) */
    @Test
    public void visuAlignWarpMatchesPyNutil() throws Exception {
        QuickNIISeries series = new Gson().fromJson(new FileReader(VISUALIGN + "alignment.json"), QuickNIISeries.class);
        List<String> rows = Files.readAllLines(new File(VISUALIGN + "expected.csv").toPath());
        for (String row : rows.subList(1, rows.size())) {
            double[] v = java.util.Arrays.stream(row.split(",")).mapToDouble(Double::parseDouble).toArray();
            QuickNIISeries.SliceInfo info = series.slices.get((int) v[0]);
            VisuAlignWarp warp = new VisuAlignWarp(info.width, info.height, new Gson().fromJson(info.markers, double[][].class));
            double[] atlas = warp.toAtlas(v[1], v[2]), slice = warp.toSlice(v[1], v[2]);
            assertEquals(v[3], atlas[0], 1e-6);
            assertEquals(v[4], atlas[1], 1e-6);
            assertEquals(v[5], slice[0], 1e-6);
            assertEquals(v[6], slice[1], 1e-6);
        }
    }
}
