package ch.epfl.biop;

import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.types.ImageJRoisFile;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.frame.RoiManager;
import net.imagej.ImageJ;
import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.RealRandomAccess;
import net.imglib2.Sampler;
import net.imglib2.type.numeric.RealType;

import java.io.File;


public class DummyCommand {

	public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        /*ImagePlus imp = IJ.openImage("/home/nico/Dropbox/BIOP/2019-02 Laura Ca/ModelEx-stack.tif");
        imp.show();*/

       /* ImagePlus imp = IJ.openImage("C:\\Users\\chiarutt\\Dropbox\\LabelPb.tif");
        imp.show();
        IJ.run("Allen Brain Adult Mouse Brain CCF 2017","");// "mapurl=file:/home/nico/Dropbox/BIOP/ABA/Data/new/ccf2017-mod65000.xml ontologyurl=file:/home/nico/Dropbox/BIOP/ABA/BrainServerTest/1.json");
        IJ.run(imp, "ConstructROIs", "atlas=[Adult Mouse Brain - Allen Brain Atlas V3] smoothen=false");*/
        //rm.runCommand(imp,"Show All");
        //IJ.run("Put Structure to Roi Manager", "atlas=[Adult Mouse Brain - Allen Brain Atlas V3] structure_list=945 adddescendants=true addancestors=true addleavesonly=false clearroimanager=true namingchoice=id roiprefix= addrlsuffix=false outputlabelimage=false");
//IJ.setTool("zoom");
        //IJ.run("Close");




	}

}