package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.wrappers.deepslice.DeepSliceTask;
import ch.epfl.biop.wrappers.deepslice.DeepSliceTaskSettings;
import ch.epfl.biop.wrappers.deepslice.DefaultDeepSliceTask;
import ij.IJ;
import ij.gui.WaitForUserDialog;
import org.apposed.appose.Appose;
import org.apposed.appose.Environment;
import org.apposed.appose.Service;
import org.scijava.platform.PlatformService;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    public static File deepSliceLocalApposeRunner(DeepSliceTaskSettings settings, File input_folder) {
        ApposeDeepSliceTask task = new ApposeDeepSliceTask();
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

        public void run() throws Exception {
            final Environment env = Appose
                    .pixi()
                    .channels("conda-forge")
                    .conda( "appose", "python==3.12", "numpy")
                    .pypi("DeepSlice==1.2.6")
                    .name("deepslice-v0")
                    //.base()
                    .logDebug() // log problems
                    .build();

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
                task.listen(evt -> System.out.println(evt.message));
                task.start();
                task.waitFor();

                if (task.status != Service.TaskStatus.COMPLETE) {
                    throw new RuntimeException("DeepSlice failed: " + task.error);
                }

                System.out.println("Output written to: " + task.outputs.get("output_path"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * These imports have to be executed from the main thread because of a numpy limitation
         * @return imports to import in the main thread
         */
        private String callImports()
        {
            return ""
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
    }
}
