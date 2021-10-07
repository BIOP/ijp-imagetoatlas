package ch.epfl.biop.atlas.allen;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import ch.epfl.biop.atlas.AtlasNode;
import ch.epfl.biop.atlas.BiopAtlasHelper;
import com.google.gson.Gson;

import ch.epfl.biop.atlas.AtlasOntology;

/**
 * TODO : make this directly serializable
 */

public class AllenOntology implements AtlasOntology {

    URL dataSource;
    AllenOntologyJson ontology;
    AtlasNode root;
    Map<Integer, AtlasNode> idToAtlasNodeMap;

    @Override
    public void initialize() throws Exception {
        // called before or after setdatasource ?

        try (Reader fileReader = new BufferedReader(
                new FileReader(new File(getDataSource().toURI())))){
            Gson gson = new Gson();
            ontology = gson.fromJson(fileReader, AllenOntologyJson.class);
        }
        root = new AllenBrainRegionsNode(ontology.msg.get(0), null);
        idToAtlasNodeMap = BiopAtlasHelper.buildIdToAtlasNodeMap(root);
        Map<Integer, AtlasNode> moduloAdd = new HashMap<>();
        idToAtlasNodeMap.forEach((id,v) ->
            moduloAdd.put(id % 65000, v) // because the map is modulo 65000
        );
        idToAtlasNodeMap.putAll(moduloAdd);
        // The hierarchy is fully set thanks to the way the tree is constructed in
        // AllenOntologyJson.AllenBrainRegionsNode
    }

    @Override
    public void setDataSource(URL dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public URL getDataSource() {
        return this.dataSource;
    }

    @Override
    public AtlasNode getRoot() {
        return root;
    }

    /**
     * https://stackoverflow.com/questions/4129666/how-to-convert-hex-to-rgb-using-java
     * @param colorStr e.g. "#FFFFFF"
     * @return
     */
    public static Color hex2Rgb(String colorStr) {
        return new Color(
                Integer.valueOf( colorStr.substring( 0, 2 ), 16 ),
                Integer.valueOf( colorStr.substring( 2, 4 ), 16 ),
                Integer.valueOf( colorStr.substring( 4, 6 ), 16 ) );
    }

    @Override
    public Color getColor(AtlasNode node) {
        return hex2Rgb(((AllenBrainRegionsNode) node).properties.get("color_hex_triplet"));
    }

    @Override
    public AtlasNode getNodeFromId(int id) {
        return idToAtlasNodeMap.get(id);
    }

}