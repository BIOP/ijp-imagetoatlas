package ch.epfl.biop;

import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;


public class DummyCommand {

	public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        /*ImagePlus imp = IJ.openImage("/home/nico/Dropbox/BIOP/2019-02 Laura Ca/ModelEx-stack.tif");
        imp.show();*/

        //ImagePlus imp = IJ.openImage("/home/nico/Desktop/Label.tif");
        //imp.show();
        //IJ.run("Allen Brain Adult Mouse Brain CCF 2017", "mapurl=file:/home/nico/Dropbox/BIOP/ABA/Data/new/ccf2017-mod65000.xml ontologyurl=file:/home/nico/Dropbox/BIOP/ABA/BrainServerTest/1.json");
        //IJ.run(imp, "ConstructROIs", "atlas=[Adult Mouse Brain - Allen Brain Atlas V3] smoothen=false");
        //rm.runCommand(imp,"Show All");
        //IJ.run("Put Structure to Roi Manager", "atlas=[Adult Mouse Brain - Allen Brain Atlas V3] structure_list=997 adddescendants=true addancestors=false addleavesonly=false clearroimanager=true namingchoice=acronym roiprefix= addrlsuffix=false outputlabelimage=false");
//IJ.setTool("zoom");
        //IJ.run("Close");


	}

}