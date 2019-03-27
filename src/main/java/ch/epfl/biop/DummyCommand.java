package ch.epfl.biop;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;


public class DummyCommand {

	public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        ImagePlus imp = IJ.openImage("/home/nico/Dropbox/BIOP/2019-02 Laura Ca/ModelEx-stack.tif");
        imp.show();
	}

}