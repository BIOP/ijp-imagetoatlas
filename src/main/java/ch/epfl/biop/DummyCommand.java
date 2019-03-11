package ch.epfl.biop;

import net.imagej.ImageJ;

public class DummyCommand {

	public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
	}

}