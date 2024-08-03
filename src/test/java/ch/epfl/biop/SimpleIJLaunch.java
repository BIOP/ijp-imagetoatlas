package ch.epfl.biop;

import loci.common.DebugTools;
import net.imagej.ImageJ;

public class SimpleIJLaunch {
    public static void main(String[] args) {

        DebugTools.setRootLevel("WARN");
        DebugTools.enableLogging ("WARN");
        final ImageJ ij = new ImageJ();
        //
        ij.ui().showUI();
        //DebugTools.enableLogging ("DEBUG");
        //RemoteElastixTask.timeOutInMs = 100000;
        //AlphaBlendedResampledSource
    }
}
