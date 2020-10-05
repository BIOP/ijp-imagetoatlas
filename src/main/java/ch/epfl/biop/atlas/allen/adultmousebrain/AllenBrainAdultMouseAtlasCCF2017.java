package ch.epfl.biop.atlas.allen.adultmousebrain;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.allen.AllenAtlas;
import ch.epfl.biop.atlas.allen.AllenOntology;
import ij.Prefs;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.net.MalformedURLException;
import java.net.URL;

// Take ply files from : http://download.alleninstitute.org/informatics-archive/current-release/mouse_ccf/annotation/ccf_2017/structure_meshes/ply/

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Allen Brain Adult Mouse Brain CCF 2017")
public class AllenBrainAdultMouseAtlasCCF2017 extends AllenAtlas implements Command {

	public String toString() {
		return "Adult Mouse Brain - Allen Brain Atlas V3";
	}
	
	public static String keyPrefix = AllenBrainAdultMouseAtlasCCF2017.class.getName()+".";

	// AWS server : http://ec2-18-222-96-84.us-east-2.compute.amazonaws.com:8081/ccf_2017/
	static String defaultMapUrl = "http://ec2-18-222-96-84.us-east-2.compute.amazonaws.com:8081/ccf_2017/";//"file:/home/nico/Dropbox/BIOP/ABA/Data/new/ccf2017-mod65000.xml";
	@Parameter  
	String mapUrl = Prefs.get(keyPrefix+"mapUrl",defaultMapUrl);

	static String defaultOntologyUrl = "http://ec2-18-222-96-84.us-east-2.compute.amazonaws.com/1.json";//file:/home/nico/Dropbox/BIOP/ABA/BrainServerTest/1.json";
    @Parameter
    String ontologyUrl = Prefs.get(keyPrefix+"ontologyUrl",defaultOntologyUrl);

	@Parameter(type= ItemIO.OUTPUT)
	BiopAtlas ba;

	@Override
	public void run() {
        try {
			this.initialize(new URL(mapUrl), new URL(ontologyUrl));

			((AllenOntology)this.ontology).mutateToModulo(65000); // Solves issue of very big indexes in allen brain ontology. The map has also been moduloed.
			//this.map.show();
			
	        Prefs.set(keyPrefix + "mapUrl", mapUrl);
	        Prefs.set(keyPrefix + "ontologyUrl", ontologyUrl);

			ba=this; // put current object to output -> then processed by plugin

        } catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

}
