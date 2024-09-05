package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.wrappers.deepslice.DeepSliceTaskSettings;
import ch.epfl.biop.wrappers.deepslice.DefaultDeepSliceTask;
import ij.IJ;
import ij.gui.WaitForUserDialog;
import org.scijava.platform.PlatformService;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DeepSliceHelper {

    final private static List<String> atlasNameMouseCompatible = new ArrayList<>();
    final private static List<String> atlasNameRatCompatible = new ArrayList<>();

    public synchronized static void addMouseCompatibleAtlas(String name) {
        if(!atlasNameMouseCompatible.contains(name)) atlasNameMouseCompatible.add(name);
    }

    public synchronized static void addRatCompatibleAtlas(String name) {
        if(!atlasNameRatCompatible.contains(name)) atlasNameRatCompatible.add(name);
    }

    /**
     * @param atlasName either the BrainGlobe API name or the specific Java packaged Atlases
     * @return true if the DeepSlice mouse model will be compatible with this atlas
     */
    public static boolean isDeepSliceMouseCompatible(String atlasName) {
        return atlasNameMouseCompatible.contains(atlasName);
    }

    /**
     *
     * @param atlasName either the BrainGlobe API name or the specific Java packaged Atlases
     * @return true if the DeepSlice mouse model will be compatible with this atlas
     */
    public static boolean isDeepSliceRatCompatible(String atlasName) {
        return atlasNameRatCompatible.contains(atlasName);
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
}
