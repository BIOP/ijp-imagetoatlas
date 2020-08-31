package ch.epfl.biop.atlas.allen;

import com.google.gson.Gson;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllenOntologyJson {
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
            writeRegions(ontology.msg, 0);
        }
    }

    public static void writeRegions(List<AllenBrainRegion> regions, int level) {
        String tabs = "";
        for (int i=0;i<level;i++) {
            tabs+="\t";
        }
        final String prefix = tabs;
        regions.forEach(region -> {
            System.out.println(prefix+"- "+region.acronym+" ["+region.id+"]");
            writeRegions(region.children, level+1);
        });
    }

    void fillIdMap(List<AllenBrainRegion> regions) {
        regions.forEach(region -> {
            this.idToRegion.put(region.id, region);
            fillIdMap(region.children);
        });
    }

    public static AllenOntologyJson getOntologyFromFile(File f) {
        try (Reader fileReader = new BufferedReader(new FileReader(f))){
            AllenOntologyJson ontology = new Gson().fromJson(fileReader, AllenOntologyJson.class);
            ontology.fillIdMap(ontology.msg);
            return ontology;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    Map<Integer, AllenBrainRegion > idToRegion = new HashMap<>();

    public AllenBrainRegion getRegionFromId(int id) {
        return idToRegion.get(id);
    }

}
