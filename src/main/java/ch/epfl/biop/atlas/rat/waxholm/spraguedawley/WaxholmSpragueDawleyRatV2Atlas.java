package ch.epfl.biop.atlas.rat.waxholm.spraguedawley;

import ch.epfl.biop.atlas.AtlasMap;
import ch.epfl.biop.atlas.AtlasOntology;
import ch.epfl.biop.atlas.BiopAtlas;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

    //https://www.nitrc.org/citation/?group_id=1081
    @Override
    public List<String> getDOIs() {
        List<String> dois = new ArrayList<>();
        dois.add("10.1016/j.neuroimage.2014.04.001");
        dois.add("10.1016/j.neuroimage.2014.10.017");
        dois.add("10.1016/j.neuroimage.2014.12.080");
        dois.add("10.1016/j.neuroimage.2019.05.016");
        return dois;
    }

    @Override
    public String getURL() {
        return "https://www.nitrc.org/projects/whs-sd-atlas";
    }


}
