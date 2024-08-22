package ch.epfl.biop;

import ch.epfl.biop.registration.sourceandconverter.spline.Elastix2DSplineRegistration;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;

public class SimpleIJLaunch {
    public static void main(String[] args) {

        DebugTools.setRootLevel("WARN");
        DebugTools.enableLogging ("WARN");
        final ImageJ ij = new ImageJ();
        //
        ij.ui().showUI();

        Elastix2DSplineRegistration r;
        SourceAndConverterService s;
        //DebugTools.enableLogging ("DEBUG");
        //RemoteElastixTask.timeOutInMs = 100000;
        //AlphaBlendedResampledSource
    }
}
