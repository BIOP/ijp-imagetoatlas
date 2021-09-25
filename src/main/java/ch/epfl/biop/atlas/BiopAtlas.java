package ch.epfl.biop.atlas;

import java.net.URL;

abstract public class BiopAtlas {

    // An atlas is : an ontology and an xml hdf5 data source

    //--------------------------- Source
    // Sources contains the xml hdf5 label image, leaves only data
    // Source -> then several imaging modalities Different visualisation
    public AtlasMap map;
    
    //--------------------------- Ontology
    public AtlasOntology ontology;

    
    abstract public void initialize(URL mapURL, URL ontologyURL);

}
