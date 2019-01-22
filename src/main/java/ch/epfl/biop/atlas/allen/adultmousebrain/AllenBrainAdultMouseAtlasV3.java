package ch.epfl.biop.atlas.allen.adultmousebrain;

import java.net.MalformedURLException;
import java.net.URL;

import ch.epfl.biop.atlas.BiopAtlas;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ch.epfl.biop.atlas.allen.AllenAtlas;
//import ch.epfl.biop.atlas.allen.adultmousebrain.v2.AllenBrainAdultMouseAtlasV2;
import ij.Prefs;

import javax.swing.*;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Allen Brain Adult Mouse Brain V3")
public class AllenBrainAdultMouseAtlasV3 extends AllenAtlas implements Command {

	public String toString() {
		return "Adult Mouse Brain - Allen Brain Atlas V3";//"Adult Mouse Brain Allen Atlas CCF v2";
	}
	
	public static String keyPrefix = AllenBrainAdultMouseAtlasV3.class.getName()+".";
	
	static String defaultMapUrl = "file:/home/nico/Dropbox/BIOP/ABA/BrainServerTest/adultmouseccfv3.xml";
	@Parameter  
	String mapUrl = Prefs.get(keyPrefix+"mapUrl",defaultMapUrl);//"/home/nico/Dropbox/BIOP/ABA/BrainServerTest/export.xml");

	// AWS server : http://ec2-18-218-179-145.us-east-2.compute.amazonaws.com:8081/allen_brain/
	static String defaultOntologyUrl = "file:/home/nico/Dropbox/BIOP/ABA/BrainServerTest/1.json";
    @Parameter
    String ontologyUrl = Prefs.get(keyPrefix+"ontologyUrl",defaultOntologyUrl);

	@Parameter(type= ItemIO.OUTPUT)
	BiopAtlas ba;

	@Override
	public void run() {
        try {
			this.initialize(new URL(mapUrl), new URL(ontologyUrl));
	        this.map.show();
			
	        Prefs.set(keyPrefix + "mapUrl", mapUrl);
	        Prefs.set(keyPrefix + "ontologyUrl", ontologyUrl);

			ba=this; // put current object to output -> then processed by plugin

        } catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

}
