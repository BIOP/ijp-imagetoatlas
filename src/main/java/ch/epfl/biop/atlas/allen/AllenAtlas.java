package ch.epfl.biop.atlas.allen;

import java.net.URL;

import ch.epfl.biop.atlas.AtlasMap;
import ch.epfl.biop.atlas.AtlasOntology;
import ch.epfl.biop.atlas.BiopAtlas;

abstract public class AllenAtlas implements BiopAtlas {
	// http://download.alleninstitute.org/informatics-archive/current-release/mouse_ccf/

	AllenOntology ontology;
	AllenMap map;

	@Override
	public void initialize(URL mapURL, URL ontologyURL) {
		ontology = new AllenOntology();
		ontology.setDataSource(ontologyURL);

		try {
			ontology.initialize();
			map = new AllenMap();
			map.setDataSource(mapURL);
			map.initialize(this.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public AtlasOntology getOntology() {
		return ontology;
	}

	@Override
	public AtlasMap getMap() {
		return map;
	}

}
