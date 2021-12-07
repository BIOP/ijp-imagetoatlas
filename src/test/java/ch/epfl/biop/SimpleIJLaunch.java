package ch.epfl.biop;

import ch.epfl.biop.wrappers.elastix.RemoteElastixTask;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;

public class SimpleIJLaunch {
    static {
        LegacyInjector.preinit();
    }
    public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        //RemoteElastixTask.timeOutInMs = 100000;
    }
}
