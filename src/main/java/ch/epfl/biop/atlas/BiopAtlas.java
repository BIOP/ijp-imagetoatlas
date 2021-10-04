package ch.epfl.biop.atlas;

import java.net.URL;
import java.util.List;

public interface BiopAtlas {

    // An atlas is : an ontology and an xml hdf5 data source

    //--------------------------- Source
    // Sources contains the xml hdf5 label image, leaves only data
    // Source -> then several imaging modalities Different visualisation
    AtlasMap getMap();
    
    //--------------------------- Ontology
    AtlasOntology getOntology();

    void initialize(URL mapURL, URL ontologyURL);

    List<String> getDOIs();

    String getURL();

    String getName();
}
