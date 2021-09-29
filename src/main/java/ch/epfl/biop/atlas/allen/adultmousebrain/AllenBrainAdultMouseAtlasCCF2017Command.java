package ch.epfl.biop.atlas.allen.adultmousebrain;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.allen.AllenAtlas;
import ij.Prefs;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.net.MalformedURLException;
import java.net.URL;

// Take ply files from : http://download.alleninstitute.org/informatics-archive/current-release/mouse_ccf/annotation/ccf_2017/structure_meshes/ply/

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Allen Brain Adult Mouse Brain CCF 2017")
public class AllenBrainAdultMouseAtlasCCF2017Command extends AllenAtlas implements Command {

	public String toString() {
		return "Adult Mouse Brain - Allen Brain Atlas V3";
	}
	
	public static String keyPrefix = AllenBrainAdultMouseAtlasCCF2017Command.class.getName()+".";

	@Parameter(label = "URL path to allen brain map data, leave empty for downloading and caching", persist = false)
	String mapUrl = Prefs.get(keyPrefix+"mapUrl","");

	@Parameter(label = "URL path to allen brain ontology data, leave empty for downloading and caching", persist = false)
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

	        Prefs.set(keyPrefix + "mapUrl", mapURL.toString());
	        Prefs.set(keyPrefix + "ontologyUrl", ontologyURL.toString());

			ba=this; // put current object to output -> then processed by plugin

        } catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

}
