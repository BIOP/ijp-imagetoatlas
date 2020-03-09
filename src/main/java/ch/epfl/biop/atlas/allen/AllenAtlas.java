package ch.epfl.biop.atlas.allen;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.commands.ConstructROIsFromImgLabel;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;

abstract public class AllenAtlas extends BiopAtlas {
	// http://download.alleninstitute.org/informatics-archive/current-release/mouse_ccf/
	@Override
	public void close() throws IOException {
		
	}

	@Override
	public void initialize(URL mapURL, URL ontologyURL) {
		ontology = new AllenOntology();
		ontology.setDataSource(ontologyURL);
		ontology.initialize();
		
		map = new AllenMap();
		map.setDataSource(mapURL);
		map.initialize(this.toString());
	}
	
	public void runOnClose(Runnable onClose) {
		/*((AllenMap) map).bdvh.getViewerFrame().addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onClose.run();
			}
		});*/
	}
	
    public ConvertibleRois getCurrentROIs() {
    	ConstructROIsFromImgLabel cmd = new ConstructROIsFromImgLabel();
    	cmd.atlas=this;
    	cmd.labelImg=this.map.getCurrentLabelImageAsImagePlus();
    	cmd.smoothen=true;
    	cmd.run();
    	cmd.labelImg.close();
    	return cmd.cr_out;
    }

}
