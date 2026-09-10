package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.quicknii.QuickNIISeries;
import ch.epfl.biop.wrappers.deepslice.DeepSliceTaskSettings;
import com.google.gson.Gson;
import org.apposed.appose.Environment;
import org.apposed.appose.Service;
import org.junit.Assume;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Smoke test for the DeepSlice Appose environment.
 *
 * <p>Builds the pixi environment from scratch, checks that it is the one the manifest describes, and
 * runs DeepSlice on one synthetic section. This catches the two ways the environment has broken or
 * can break: tensorflow ships platform-specific wheels, so an environment that resolves on Linux can
 * be unsatisfiable elsewhere; and tensorflow.keras silently becomes Keras 3 unless tf-keras is
 * active, under which the rat model returns garbage.</p>
 *
 * <p>Skipped by default, because it downloads about 1 GB (environment plus model weights). Enable with
 * {@code mvn test -Dtest=DeepSliceApposeEnvTest -Ddeepslice.env.test=true}.</p>
 */
public class DeepSliceApposeEnvTest {

    /**
     * DeepSlice model to run, without ensemble. Mouse and rat weights weigh the same (~85 MB, and
     * DeepSlice downloads the secondary ones even without ensemble); rat is the model Keras 3 breaks.
     */
    private static final String MODEL = "rat";

    /** Reports the installed versions, and which Keras implementation backs tensorflow.keras. */
    private static final String VERSIONS_SCRIPT =
            "from importlib.metadata import version\n"
          + "import tensorflow as tf\n"
          + "task.outputs['deepslice_version'] = version('DeepSlice')\n"
          + "task.outputs['tensorflow_version'] = version('tensorflow')\n"
          + "task.outputs['keras_module'] = tf.keras.layers.Dense.__module__\n";

    @Test
    public void buildsEnvironmentAndPredicts() throws Exception {
        Assume.assumeTrue("set -Ddeepslice.env.test=true to run this test",
                Boolean.getBoolean("deepslice.env.test"));

        Environment env = DeepSliceHelper.ApposeDeepSliceTask.environment(msg -> System.out.println("[pixi] " + msg));

        try (Service python = env.python().init(DeepSliceHelper.ApposeDeepSliceTask.callImports())) {
            Service.Task task = python.task(VERSIONS_SCRIPT, Collections.emptyMap());
            task.start();
            task.waitFor();
            assertEquals("version check failed: " + task.error, Service.TaskStatus.COMPLETE, task.status);

            System.out.println("DeepSlice  : " + task.outputs.get("deepslice_version"));
            System.out.println("tensorflow : " + task.outputs.get("tensorflow_version"));
            System.out.println("tf.keras   : " + task.outputs.get("keras_module"));

            assertEquals("installed DeepSlice differs from the pinned version",
                    DeepSliceHelper.ApposeDeepSliceTask.DS_VERSION, task.outputs.get("deepslice_version"));
            assertEquals("installed tensorflow differs from the pinned version",
                    DeepSliceHelper.ApposeDeepSliceTask.TF_VERSION, task.outputs.get("tensorflow_version"));
            assertTrue("tensorflow.keras is not backed by tf-keras (Keras 2), DeepSlice's rat model needs it",
                    ((String) task.outputs.get("keras_module")).startsWith("tf_keras"));
        }

        Path folder = Files.createTempDirectory("deepslice-env-test");
        try {
            // Named like the sections ABBA exports: DeepSlice reads the section number from "_s###".
            writeSyntheticSection(folder.resolve("Section_s001.png").toFile());

            DeepSliceTaskSettings settings = new DeepSliceTaskSettings();
            settings.model = MODEL;
            settings.input_folder = folder.toFile().getAbsolutePath();
            settings.output_folder = null;
            settings.ensemble = false;
            settings.section_numbers = true;
            settings.propagate_angles = false; // needs more than two sections
            settings.enforce_index_order = false;
            settings.use_enforce_index_spacing = false;
            settings.enforce_index_spacing = "";

            DeepSliceHelper.ApposeDeepSliceTask deepSlice = new DeepSliceHelper.ApposeDeepSliceTask();
            deepSlice.setSettings(settings);
            deepSlice.isHeadless(true);
            deepSlice.listenProgress(msg -> System.out.println("[DeepSlice] " + msg));
            deepSlice.run();

            // Where DeepSliceHelper.deepSliceLocalApposeRunner tells ABBA to look for the result.
            File results = new File(folder.toFile(), "results.json");
            assertTrue("DeepSlice wrote no " + results, results.exists());

            QuickNIISeries series;
            try (FileReader reader = new FileReader(results)) {
                series = new Gson().fromJson(reader, QuickNIISeries.class);
            }
            assertEquals(1, series.slices.size());
            double[] anchoring = series.slices.get(0).anchoring;
            System.out.println("anchoring  : " + Arrays.toString(anchoring));
            assertEquals(9, anchoring.length);
            for (double value : anchoring) {
                assertTrue("non-finite anchoring value " + value, Double.isFinite(value));
            }
        } finally {
            try (Stream<Path> paths = Files.walk(folder)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    /** A bright elliptic "section" with a darker core, on a black background. */
    private static void writeSyntheticSection(File file) throws Exception {
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 640, 480);
        g.setColor(new Color(190, 190, 190));
        g.fillOval(80, 60, 480, 360);
        g.setColor(new Color(110, 110, 110));
        g.fillOval(250, 170, 140, 140);
        g.dispose();
        ImageIO.write(image, "png", file);
    }
}
