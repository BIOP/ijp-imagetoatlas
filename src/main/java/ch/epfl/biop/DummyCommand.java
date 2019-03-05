package ch.epfl.biop;

import net.imagej.ImageJ;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.allen.adultmousebrain.AllenBrainAdultMouseAtlasV2;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
//import ch.epfl.biop.wrappers.BiopWrappersCheck;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

public class DummyCommand {

	public static void main(String[] args) {

	    //String out = "123".replaceAll("(\\d+)(-)(\\d+)","$1");
	    //System.out.println(out);
		// Checker board : v=((x%50)<25)*128+((y%50)<25)*128
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();


        
        //BiopWrappersCheck.reportAllWrappers();
        //try {
			//Object o = ij.io().open("/home/nico/Desktop/Label.tif");
			//ij.display().createDisplay(o);
			
			/*ImagePlus imp = new ImagePlus("/home/nico/Desktop/Label.tif");
			imp.show();
			
			//IJ.run(imp,"Median...", "radius=1");//run(arg0, arg1);
		
			
			System.out.println("start");
			ArrayList<Roi> out = ConvertibleRois.labelImageToRoiArrayVectorize(imp);
			ConvertibleRois roi = new ConvertibleRois();
			roi.set(out);
			roi.to(RoiManager.class);
			System.out.println("stop");*/
			
		//} catch (IOException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}
        // save key/value pairs
        //Prefs.set("my.persistent.name", "Grizzly Adams");
        //Prefs.set("my.persistent.number", 10);
        
        
        /*BiopAtlas ba = new AllenBrainAdultMouseAtlasV2();
        File fmap = new File("/home/nico/Dropbox/BIOP/ABA/BrainServerTest/export.xml");
        

        
        File font = new File("/home/nico/Dropbox/BIOP/ABA/BrainServerTest/1.json");
        
        
        try {
            System.out.println(fmap.toURI().toURL().toString());
            System.out.println(font.toURI().toURL().toString());
			ba.initialize(fmap.toURI().toURL(), font.toURI().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
        ba.map.show();
        */
        
        
        
        //AtlasDataPreferences.fetchOntology1();
/*
        ImagePlus imp = IJ.openImage("C:\\Users\\chiarutt\\Dropbox\\BIOP\\ABA\\Data\\annotationSliceTest.tif");
        imp.show();
        ConvertibleRois cr = new ConvertibleRois();1070 1038
        cr.set(imp);
        cr.to(RoiManager.class);
*/
        //Elastix.setExePath(new File("C:\\elastix-4.9.0-win64\\elastix.exe"));
        //Transformix.setExePath(new File("C:\\elastix-4.9.0-win64\\transformix.exe"));
        
        //CommandService cs = new DefaultCommandService();
        //System.out.println(cs.getCommands());
        //cs.run(BrowseBrain.class, true);
        //System.out.println(BiopWrappersCheck.reportAllWrappers());
        //System.out.println(BiopWrappersCheck.reportAllWrappers());
        //System.out.println("idSlice = " + AtlasDataPreferences.getSagittalSectionIdFromZPos(2,4000));
        /*AtlasDataPreferences.fetchOntology1();
        System.out.println("ans="+AtlasDataPreferences.ontologyIdToAcronym.get(860));
        System.out.println("ans="+AtlasDataPreferences.ontologyIdToName.get(860));
        System.out.println("ans DORsm="+AtlasDataPreferences.ontologyAcronymToId.get("DORsm"));
        ArrayList<Integer> ans = AtlasDataPreferences.getAllLeaves(864);

        System.out.println("Il y a "+ans.size()+" feuilles dans cette structure.");
        ans.forEach(id -> {
            System.out.println(AtlasDataPreferences.ontologyIdToAcronym.get(id));
        });
        //RegParamRigid_Default rpr = new RegParamRigid_Default();
        //System.out.println(rpr.toString(rpr));
        Elastix.setExePath(new File("C:\\elastix-4.9.0-win64\\elastix.exe"));
        //System.out.println(AtlasDataPreferences.getCoronalAtlasIdFromXPos(8450));*/
	}

}