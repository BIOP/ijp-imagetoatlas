package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.atlas.scijava.AtlasChooserCommand;
import ch.epfl.biop.wrappers.deepslice.DeepSliceTask;
import ch.epfl.biop.wrappers.deepslice.DeepSliceTaskSettings;
import ch.epfl.biop.wrappers.deepslice.DefaultDeepSliceTask;
import ij.IJ;
import ij.gui.WaitForUserDialog;
import org.apposed.appose.Appose;
import org.apposed.appose.BuildException;
import org.apposed.appose.Environment;
import org.apposed.appose.Service;
import org.scijava.platform.PlatformService;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeepSliceHelper {

    public static void addJavaAtlases() {
        DeepSliceHelper.addMouseCompatibleAtlas(
                "Adult Mouse Brain - Allen Brain Atlas V3",
                "Adult Mouse Brain - Allen Brain Atlas V3p1",
                "allen_mouse_10um_java",
                "example_mouse_100um",
                "allen_mouse_10um",
                "allen_mouse_25um",
                "allen_mouse_50um",
                "allen_mouse_100um",
                "kim_mouse_10um",
                "kim_mouse_25um",
                "kim_mouse_50um",
                "kim_mouse_100um",
                "osten_mouse_10um",
                "osten_mouse_25um",
                "osten_mouse_50um",
                "osten_mouse_100um",
                "perens_lsfm_mouse_20um",
                "kim_dev_mouse_stp_10um",
                "kim_dev_mouse_idisco_10um",
                "kim_dev_mouse_mri_a0_10um",
                "kim_dev_mouse_mri_adc_10um",
                "kim_dev_mouse_mri_dwi_10um",
                "kim_dev_mouse_mri_fa_10um",
                "kim_dev_mouse_mri_mtr_10um",
                "kim_dev_mouse_mri_t2_10um",
                "allen_mouse_bluebrain_barrels_10um",
                "allen_mouse_bluebrain_barrels_25um",
                "princeton_mouse_20um");

        DeepSliceHelper.addRatCompatibleAtlas(
                "Rat - Waxholm Sprague Dawley V4",
                "Rat - Waxholm Sprague Dawley V4p2",
                "whs_sd_rat_39um_java",
                "whs_sd_rat_39um");

    }

    final private static List<String> atlasNameMouseCompatible = new ArrayList<>();
    final private static List<String> atlasNameRatCompatible = new ArrayList<>();

    public synchronized static void addMouseCompatibleAtlas(String... names) {
        for (String name: names) {
            if(!atlasNameMouseCompatible.contains(name)) atlasNameMouseCompatible.add(name);
        }
    }

    public synchronized static void addRatCompatibleAtlas(String... names) {
        for (String name: names) {
            if (!atlasNameRatCompatible.contains(name)) atlasNameRatCompatible.add(name);
        }
    }

    /**
     * @param atlasName either the BrainGlobe API name or the specific Java packaged Atlases
     * @return true if the DeepSlice mouse model will be compatible with this atlas
     */
    public static boolean isDeepSliceMouseCompatible(String atlasName) {
        // To support composite atlases:
        return atlasNameMouseCompatible.stream().anyMatch(atlasName::contains);
    }

    /**
     *
     * @param atlasName either the BrainGlobe API name or the specific Java packaged Atlases
     * @return true if the DeepSlice mouse model will be compatible with this atlas
     */
    public static boolean isDeepSliceRatCompatible(String atlasName) {
        return atlasNameRatCompatible.stream().anyMatch(atlasName::contains);// atlasNameRatCompatible.contains(atlasName);
    }

    public static File deepSliceLocalRunner(DeepSliceTaskSettings settings, File input_folder) {
        DefaultDeepSliceTask task = new DefaultDeepSliceTask();
        task.setSettings(settings);
        try {
            task.run();
        } catch (Exception e) {
            IJ.log("Could not run DeepSlice: "+e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return new File(input_folder, "results.json");
    }

    public static File deepSliceLocalApposeRunner(DeepSliceTaskSettings settings, File input_folder, Consumer<String> listenEnv, Consumer<String> listenProgress, boolean headless) {
        ApposeDeepSliceTask task = new ApposeDeepSliceTask();
        task.setSettings(settings);
        task.listenEnv(listenEnv);
        task.listenProgress(listenProgress);
        task.isHeadless(headless);
        try {
            task.run();
        } catch (Exception | ExceptionInInitializerError e) {
            // Not rethrown: the caller runs within LockAndRunOnceSliceAction, whose other slices would
            // then wait forever. The missing results.json aborts the registration instead.
            Throwable cause = e instanceof ExceptionInInitializerError ? e.getCause() : e;
            IJ.log("Could not run DeepSlice: "+cause.getMessage());
            e.printStackTrace();
        }
        return new File(input_folder, "results.json");
    }

    public static double getMedian(double[] array) {
        Arrays.sort(array);
        double median;
        if (array.length % 2 == 0)
            median = (array[array.length/2] + array[array.length/2 - 1])/2;
        else
            median = array[array.length/2];
        return median;
    }

    public static class Holder<T> implements Supplier<T>, Consumer<T> {
        T t;
        public Holder(T t) {
            this.t = t;
        }

        public Holder() {

        }

        public T get() {
            return t;
        }

        @Override
        public void accept(T t) {
            this.t = t;
        }
    }

    public static File deepSliceWebRunner(File input_folder, PlatformService ps) {
        IJ.log("Dataset exported in folder " + input_folder.getAbsolutePath());
        new WaitForUserDialog("Now opening DeepSlice webpage",
                "Drag and drop all slices into the webpage.")
                .show();
        try {
            ps.open(new URL("https://www.deepslice.com.au/"));
            ps.open(input_folder.toURI().toURL());
        } catch (Exception e) {
            IJ.error("Couldn't open DeepSlice from Fiji, ",
                    "please go to https://www.deepslice.com.au/ and drag and drop your images located in " + input_folder.getAbsolutePath());
        }
        new WaitForUserDialog("DeepSlice result",
                "Put the 'results.json' file into " + input_folder.getAbsolutePath() + " then press ok.")
                .show();
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return new File(input_folder, "results.json");
    }

    public static class ApposeDeepSliceTask extends DeepSliceTask {

        /** Classpath manifest describing the environment, relative to this class's package. */
        private static final String MANIFEST = "deepslice/pixi.toml";

        /** Placeholder in the manifest, replaced with the conda subdir of the running machine. */
        private static final String PLATFORM_PLACEHOLDER = "@PLATFORM@";

        /** The pixi manifest for this machine: a resource, with its placeholder resolved. */
        private static final String PIXI_TOML = loadManifest();

        /** The DeepSlice version installed into the environment, read back from the manifest. */
        public static final String DS_VERSION = parsePin(PIXI_TOML, "DeepSlice");

        /**
         * The TensorFlow version installed into the environment, read back from the manifest.
         * <p>
         * DeepSlice is written against the Keras 2 API. With TensorFlow 2.16+ the default
         * `tensorflow.keras` is Keras 3, and the rat model - which, unlike the mouse one, wraps
         * Xception into a functional model called with `training=True` - then returns garbage
         * predictions (scale off by ~4x, inconsistent slicing angles, random depths). The mouse model
         * is unaffected. The manifest therefore pins TensorFlow and installs `tf-keras` (the Keras 2
         * backport), which is activated by the TF_USE_LEGACY_KERAS environment variable set in
         * {@link ApposeDeepSliceTask#callImports()}.
         * </p>
         */
        public static final String TF_VERSION = parsePin(PIXI_TOML, "tensorflow");

        /** The environment, built once per JVM by {@link #environment(Consumer)}. */
        private static volatile Environment cachedEnv;

        /** True on macOS, whatever the architecture. */
        private static boolean isMacOS() {
            return System.getProperty("os.name", "").toLowerCase().startsWith("mac");
        }

        /** Conda platform identifier ("subdir") of the running machine. */
        private static String condaSubdir() {
            String arch = System.getProperty("os.arch", "").toLowerCase();
            boolean arm64 = arch.equals("aarch64") || arch.equals("arm64");
            if (isMacOS()) return arm64 ? "osx-arm64" : "osx-64";
            if (System.getProperty("os.name", "").toLowerCase().startsWith("windows")) return "win-64";
            return arm64 ? "linux-aarch64" : "linux-64";
        }

        /**
         * Reads the manifest off the classpath and resolves its placeholder.
         * <p>
         * Loaded relative to this class rather than from the classpath root, because other Appose-based
         * Fiji plugins ship a {@code /pixi.toml} of their own, and inside Fiji they all share one
         * classpath. Set {@code -Ddeepslice.pixi.manifest=/path/to/pixi.toml} to try a different
         * environment without rebuilding.
         * </p>
         */
        private static String loadManifest() {
            String override = System.getProperty("deepslice.pixi.manifest");
            try {
                String toml;
                if (override != null) toml = new String(Files.readAllBytes(Paths.get(override)), StandardCharsets.UTF_8);
                else try (InputStream in = ApposeDeepSliceTask.class.getResourceAsStream(MANIFEST)) {
                    if (in == null) throw new IllegalStateException("Missing classpath resource: "
                            + ApposeDeepSliceTask.class.getPackage().getName().replace('.', '/') + "/" + MANIFEST);
                    toml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                return toml.replace(PLATFORM_PLACEHOLDER, condaSubdir());
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + (override != null ? override : MANIFEST), e);
            }
        }

        /** Extracts the exact version pin of a PyPI package from a manifest, e.g. {@code tensorflow = "==2.21.0"}. */
        private static String parsePin(String toml, String pkg) {
            Matcher matcher = Pattern.compile("^\\s*" + Pattern.quote(pkg) + "\\s*=\\s*\"==([^\"]+)\"",
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(toml);
            if (!matcher.find()) throw new IllegalStateException(
                    "No exact " + pkg + " pin found in the DeepSlice pixi manifest");
            return matcher.group(1);
        }

        /** Builds the DeepSlice environment on the first call, and returns the same one afterwards. */
        static synchronized Environment environment(Consumer<String> listenEnv) throws BuildException {
            if (cachedEnv == null) cachedEnv = buildEnvironment(listenEnv);
            return cachedEnv;
        }

        private static Environment buildEnvironment(Consumer<String> listenEnv) throws BuildException {
            if ("osx-64".equals(condaSubdir())) {
                throw new BuildException("TensorFlow publishes no Intel-Mac (osx-64) wheel since 2.16.2, "
                        + "so DeepSlice cannot run locally on Intel Macs. On Apple silicon, this message "
                        + "means the JVM is an Intel one running under Rosetta: use an arm64 Fiji/JDK.");
            }
            return Appose
                    .pixi()
                    .content(PIXI_TOML)
                    .name("deepslice-v"+DS_VERSION+"-tf"+TF_VERSION)
                    .logDebug() // log problems
                    .subscribeError(listenEnv)
                    .subscribeOutput(listenEnv)
                    .build();
        }

        public void run() throws Exception {
            final Environment env = (cachedEnv == null && !headless) ? environmentWithDialog() : environment(listenEnv);

            try (Service python = env.python().init(callImports())) {
                final Map<String, Object> inputs = new HashMap<>();
                inputs.put("model_name", settings.model);
                inputs.put("input_folder", settings.input_folder);
                inputs.put("output_folder", settings.output_folder); // null is fine, Appose passes it as None
                inputs.put("ensemble", settings.ensemble);
                inputs.put("section_numbers", settings.section_numbers);
                inputs.put("propagate_angles", settings.propagate_angles);
                inputs.put("enforce_index_order", settings.enforce_index_order);
                inputs.put("enforce_index_spacing",
                        settings.use_enforce_index_spacing ? settings.enforce_index_spacing : null);

                final Service.Task task = python.task(getScript(), inputs);
                task.listen((evt) -> {
                    if (evt.message!=null) {
                        listenProgress.accept(evt.message);
                    }
                });
                task.start();
                task.waitFor();

                if (task.status != Service.TaskStatus.COMPLETE) {
                    throw new RuntimeException("DeepSlice failed: " + task.error);
                }

                System.out.println("Output written to: " + task.outputs.get("output_path"));
            }
        }

        /** Builds the environment while a modal dialog tells the user what is going on. */
        private Environment environmentWithDialog() throws Exception {
            JDialog waitDialog = new JDialog((java.awt.Frame) null, "Loading DeepSlice...", true);
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

            JLabel infoLabel = new JLabel("<html>"
                    + "<h2>Loading DeepSlice (v"+DS_VERSION+")</h2>"
                    + "<p><img src='" + DeepSliceHelper.class.getClassLoader().getResource("graphics/DeepSlice.png") + "' width='80' height='80'></p>"
                    + "<p>For more information, visit <a href='https://www.deepslice.org/'>https://www.deepslice.org/</a></p>"
                    + "</html>", SwingConstants.CENTER);
            infoLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            panel.add(infoLabel);

            panel.add(Box.createVerticalStrut(15));

            ImageIcon loadingIcon = new ImageIcon(AtlasChooserCommand.class.getClassLoader().getResource("graphics/loading.gif"));
            loadingIcon.setImage(loadingIcon.getImage().getScaledInstance(64, 64, java.awt.Image.SCALE_DEFAULT));
            JLabel waitLabel = new JLabel("Loading DeepSlice environment, please wait...", loadingIcon, SwingConstants.CENTER);
            waitLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            panel.add(waitLabel);

            waitDialog.getContentPane().add(panel);
            waitDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            waitDialog.pack();
            waitDialog.setLocationRelativeTo(null);

            // SwingWorker builds the environment in background; disposing the modal dialog from done()
            // unblocks setVisible(true) below.
            final AtomicReference<Environment> envRef = new AtomicReference<>();
            final AtomicReference<Exception> errRef = new AtomicReference<>();
            final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    try {
                        envRef.set(environment(listenEnv));
                    } catch (Exception e) {
                        errRef.set(e);
                    }
                    return null;
                }
                @Override
                protected void done() {
                    waitDialog.dispose();
                }
            };
            // Started only once the dialog shows: an environment already on disk builds almost at once,
            // and a dispose() landing before setVisible(true) would leave the dialog open for good.
            waitDialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    worker.execute();
                }
            });
            waitDialog.setVisible(true);

            if (errRef.get() != null) throw errRef.get();
            return envRef.get();
        }

        /**
         * These imports have to be executed from the main thread because of a numpy limitation.
         * TF_USE_LEGACY_KERAS has to be set before TensorFlow is imported - see {@link #TF_VERSION}.
         * @return imports to import in the main thread
         */
        static String callImports()
        {
            return ""
                    + "import os\n"
                    + "os.environ['TF_USE_LEGACY_KERAS'] = '1'\n" // DeepSlice needs the Keras 2 API, see TF_VERSION
                    + "from DeepSlice import DSModel\n"
                    + "from DeepSlice.read_and_write import QuickNII_functions\n"
                    + "import numpy\n";
        }

        private String getScript() {
            return ""
                    + "task.update('Loading model...')\n"
                    + "model = DSModel(model_name)\n"
                    + "\n"
                    + "task.update('Running prediction...')\n"
                    + "model.predict(input_folder, ensemble, section_numbers)\n"
                    + "\n"
                    + "if propagate_angles:\n"
                    + "    task.update('Propagating angles...')\n"
                    + "    model.propagate_angles()\n"
                    + "\n"
                    + "if enforce_index_order:\n"
                    + "    task.update('Enforcing index order...')\n"
                    + "    model.enforce_index_order()\n"
                    + "\n"
                    + "if enforce_index_spacing is not None:\n"
                    + "    task.update('Enforcing index spacing...')\n"
                    + "    thickness = None if enforce_index_spacing == 'None' else float(enforce_index_spacing)\n"
                    + "    model.enforce_index_spacing(section_thickness=thickness)\n"
                    + "\n"
                    + "task.update('Saving results...')\n"
                    + "filename = output_folder if output_folder else input_folder + 'results'\n"
                    + "target = model.config['target_volumes'][model.species]['name']\n"
                    + "aligner = model.config['DeepSlice_version']['prerelease']\n"
                    + "QuickNII_functions.write_QUINT_JSON(\n"
                    + "    df=model.predictions, filename=filename, aligner=aligner, target=target\n"
                    + ")\n"
                    + "\n"
                    + "task.outputs['output_path'] = filename\n"
                    + "task.update('done.')\n";
        }

        Consumer<String> listenEnv = (message) -> {};
        Consumer<String> listenProgress = (message) -> {};

        public void listenEnv(Consumer<String> listenEnv) {
            this.listenEnv = listenEnv;
        }

        public void listenProgress(Consumer<String> listenProgress) {
            this.listenProgress = listenProgress;
        }

        boolean headless;

        public void isHeadless(boolean headless) {
            this.headless = headless;
        }
    }
}
