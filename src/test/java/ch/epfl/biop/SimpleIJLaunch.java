package ch.epfl.biop;

import ch.epfl.biop.wrappers.elastix.RemoteElastixTask;
import net.imagej.ImageJ;

public class SimpleIJLaunch {

    public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        RemoteElastixTask.timeOutInMs = 100000;
    }
}
