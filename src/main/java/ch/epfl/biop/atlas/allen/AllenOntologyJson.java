package ch.epfl.biop.atlas.allen;

import ch.epfl.biop.atlas.AtlasNode;
import com.google.gson.Gson;
import org.scijava.util.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.List;

public class AllenOntologyJson {

    protected static Logger logger = LoggerFactory.getLogger(AllenOntologyJson.class);

    boolean success;
    int id;
    int start_row;
    int num_rows;
    int total_rows;
    List<AllenBrainRegion> msg;

    public class AllenBrainRegion {
        int id;
        int atlas_id;
        int ontology_id;
        String acronym;
        String name;
        String color_hex_triplet;
        int graph_order;
        int st_level;
        int hemisphere_id;
        int parent_structure_id;
        List<AllenBrainRegion> children;
    }

    public static void main(String... args) throws Exception {
        try (Reader fileReader = new BufferedReader(
                new FileReader(new File("src/main/resources/AllenBrainData/1.json")))){
            Gson gson = new Gson();
            AllenOntologyJson ontology = gson.fromJson(fileReader, AllenOntologyJson.class);
        }
    }

    public static class AllenBrainRegionsNode implements AtlasNode {
        final AllenBrainRegion abr;
        final AllenBrainRegionsNode parent;
        final Map<String, String> properties;
        final List<TreeNode<?>> children;

        public AllenBrainRegionsNode(AllenBrainRegion abr, AllenBrainRegionsNode parent) {
            this.abr = abr;
            this.parent = parent;
            //System.out.println("id = "+abr.id);
            Map<String, String> mutableMap = new HashMap<>();
            mutableMap.put("id", Integer.toString(abr.id));
            mutableMap.put("atlas_id", Integer.toString(abr.atlas_id));
            mutableMap.put("ontology_id", Integer.toString(abr.ontology_id));
            mutableMap.put("acronym", abr.acronym);
            mutableMap.put("name", abr.name);
            mutableMap.put("color_hex_triplet", abr.color_hex_triplet);
            mutableMap.put("graph_order", Integer.toString(abr.graph_order));
            mutableMap.put("st_level", Integer.toString(abr.st_level));
            mutableMap.put("hemisphere_id", Integer.toString(abr.hemisphere_id));
            mutableMap.put("parent_structure_id", Integer.toString(abr.parent_structure_id));
            properties = Collections.unmodifiableMap(mutableMap);
            children = new ArrayList<>(abr.children.size());
            abr.children.forEach(child_abr -> {
                children.add(new AllenBrainRegionsNode(child_abr, this));
            });
        }

        @Override
        public int getId() {
            return abr.id;
        }

        @Override
        public int getLabelValue() {
            return abr.id % 65000; // Problem of labels above 65535.. still bijective with mod 65000
        }

        @Override
        public Map<String, String> data() {
            return properties;
        }

        @Override
        public AllenBrainRegionsNode parent() {
            return parent;
        }

        @Override
        public void setParent(TreeNode<?> parent) {
            // Done in the constructor
            throw new UnsupportedOperationException("Cannot set parent, it is already set");
        }

        @Override
        public List<TreeNode<?>> children() {
            return children;
        }
    }

}
