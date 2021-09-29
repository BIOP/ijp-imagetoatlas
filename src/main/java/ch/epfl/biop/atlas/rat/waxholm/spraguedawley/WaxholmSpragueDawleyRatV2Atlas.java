package ch.epfl.biop.atlas.rat.waxholm.spraguedawley;

import ch.epfl.biop.atlas.AtlasMap;
import ch.epfl.biop.atlas.AtlasOntology;
import ch.epfl.biop.atlas.BiopAtlas;

import java.net.URL;

public class WaxholmSpragueDawleyRatV2Atlas implements BiopAtlas {

    WaxholmSpragueDawleyRatV2Map map;
    WaxholmSpragueDawleyRatV2Ontology ontology;

    @Override
    public AtlasMap getMap() {
        return map;
    }

    @Override
    public AtlasOntology getOntology() {
        return ontology;
    }

    @Override
    public void initialize(URL mapURL, URL ontologyURL) {
        ontology = new WaxholmSpragueDawleyRatV2Ontology();
        ontology.setDataSource(ontologyURL);

        try {
            ontology.initialize();
            map = new WaxholmSpragueDawleyRatV2Map();
            map.setDataSource(mapURL);
            map.initialize(this.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
