package ch.epfl.biop.atlas.aligner;

import ch.epfl.biop.wrappers.deepslice.DeepSliceTaskSettings;
import ch.epfl.biop.wrappers.deepslice.DefaultDeepSliceTask;
import ij.IJ;
import ij.gui.WaitForUserDialog;
import org.scijava.platform.PlatformService;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DeepSliceHelper {

    /**
     * @param atlasName either the BrainGlobe API name or the specific Java packaged Atlases
     * @return true if the DeepSlice mouse model will be compatible with this atlas
     */
    public static boolean isDeepSliceMouseCompatible(String atlasName) {
        switch (atlasName) {
            case "example_mouse_100um":
            case "allen_mouse_100um":
            case "allen_mouse_50um":
            case "allen_mouse_25um":
            case "allen_mouse_10um":
            case "kim_mouse_100um":
            case "kim_mouse_50um":
            case "kim_mouse_25um":
            case "kim_mouse_10um":
            case "osten_mouse_100um":
            case "osten_mouse_50um":
            case "osten_mouse_25um":
            case "osten_mouse_10um":
            case "Adult Mouse Brain - Allen Brain Atlas V3":
            case "Adult Mouse Brain - Allen Brain Atlas V3p1":
                return true;
        }
        return false;
    }

    /**
     *
     * @param atlasName either the BrainGlobe API name or the specific Java packaged Atlases
     * @return true if the DeepSlice mouse model will be compatible with this atlas
     */
    public static boolean isDeepSliceRatCompatible(String atlasName) {
        switch (atlasName) {
            case "Rat - Waxholm Sprague Dawley V4":
            case "Rat - Waxholm Sprague Dawley V4p2":
            case "whs_sd_rat_39um":
                return true;
        }
        return false;
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
