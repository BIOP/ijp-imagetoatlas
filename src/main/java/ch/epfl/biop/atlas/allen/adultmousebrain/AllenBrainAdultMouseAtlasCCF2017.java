package ch.epfl.biop.atlas.allen.adultmousebrain;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.allen.AllenAtlas;
import ch.epfl.biop.atlas.allen.AllenOntology;
import ij.Prefs;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

// Take ply files from : http://download.alleninstitute.org/informatics-archive/current-release/mouse_ccf/annotation/ccf_2017/structure_meshes/ply/

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Allen Brain Adult Mouse Brain CCF 2017")
public class AllenBrainAdultMouseAtlasCCF2017 extends AllenAtlas implements Command {

	public String toString() {
		return "Adult Mouse Brain - Allen Brain Atlas V3";
	}
	
	public static String keyPrefix = AllenBrainAdultMouseAtlasCCF2017.class.getName()+".";

	public static File cachedSampleDir = new File(System.getProperty("user.home"),"cached_atlas");

	// AWS server : http://ec2-18-222-96-84.us-east-2.compute.amazonaws.com:8081/ccf_2017/
	//static String defaultMapUrl = System.getProperty("user.home")+File.separator+"cached_atlas"+File.separator+"mouse_brain_ccfv3.xml";//"http://ec2-18-222-96-84.us-east-2.compute.amazonaws.com:8081/ccf_2017/";//"file:/home/nico/Dropbox/BIOP/ABA/Data/new/ccf2017-mod65000.xml";
	@Parameter(label = "URL path to allen brain map data, leave empty for downloading and caching")
	String mapUrl = Prefs.get(keyPrefix+"mapUrl","");

	//static String defaultOntologyUrl = ;//"http://ec2-18-222-96-84.us-east-2.compute.amazonaws.com/1.json";//file:/home/nico/Dropbox/BIOP/ABA/BrainServerTest/1.json";
    @Parameter(label = "URL path to allen brain ontology data, leave empty for downloading and caching")
    String ontologyUrl = Prefs.get(keyPrefix+"ontologyUrl","");

	@Parameter(type= ItemIO.OUTPUT)
	BiopAtlas ba;

	@Override
	public void run() {
        try {
        	URL mapURL, ontologyURL;
        	if ((mapUrl == null)||(mapUrl.equals(""))||(ontologyUrl == null)||(ontologyUrl.equals(""))) {
				mapURL = AllenBrainCCFv3Downloader.getMapUrl();
				ontologyURL = AllenBrainCCFv3Downloader.getOntologyURL();
			} else {
				mapURL = new URL(mapUrl);
				ontologyURL = new URL(ontologyUrl);
			}

			this.initialize(mapURL, ontologyURL);

			((AllenOntology)this.ontology).mutateToModulo(65000); // Solves issue of very big indexes in allen brain ontology. The map has also been moduloed.
			this.map.show();
			
	        Prefs.set(keyPrefix + "mapUrl", mapURL.toString());
	        Prefs.set(keyPrefix + "ontologyUrl", ontologyURL.toString());

			ba=this; // put current object to output -> then processed by plugin

        } catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

}
