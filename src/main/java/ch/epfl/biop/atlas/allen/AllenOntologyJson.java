package ch.epfl.biop.atlas.allen;

import ch.epfl.biop.atlas.AtlasOntology;
import com.google.gson.Gson;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class AllenOntologyJson implements AtlasOntology {
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

   /* public static AllenOntologyJson getOntologyFromFile(File f) {
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
    }*/

    Map<Integer, AllenBrainRegion > idToRegion = new HashMap<>();

    public AllenBrainRegion getRegionFromId(int id) {
        return idToRegion.get(id);
    }

    URL dataSource;
    AllenOntologyJson ontology;

    @Override
    public void initialize() {
        ontology = new Gson().fromJson(jsonGetRequest(
                //"http://api.brain-map.org/api/v2/structure_graph_download/1.json"
                this.getDataSource().toString()
        ), AllenOntologyJson.class);

        fillIdMap(ontology.msg);
    }

    public static String jsonGetRequest(String urlQueryString) {
        String json = null;
        try {
            URL url = new URL(urlQueryString);

            URLConnection connection = (URLConnection) url.openConnection();
            connection.setDoOutput(true);
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
                ((HttpURLConnection) connection).setRequestMethod("GET");
            }
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("charset", "utf-8");
            connection.connect();
            InputStream inStream = connection.getInputStream();
            json = streamToString(inStream); // input stream to string
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return json;
    }

    static String streamToString(InputStream inputStream) {
        String text = new Scanner(inputStream, "UTF-8").useDelimiter("\\Z").next();
        //System.out.println(text);
        return text;
    }

    @Override
    public void setDataSource(URL dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public URL getDataSource() {
        return dataSource;
    }

    @Override
    public Map<Integer, List<Integer>> getParentToChildrenMap() {

        return null;
    }

    @Override
    public Map<Integer, Integer> getParentToParentMap() {
        return null;
    }

    @Override
    public List<Integer> getAllIds() {
        return null;
    }

    @Override
    public List<Integer> getAllLeaves(int id) {
        return null;
    }

    @Override
    public List<Integer> getAllChildren(int id) {
        return null;
    }

    @Override
    public List<Integer> getAllParents(int id) {
        return null;
    }

    @Override
    public Integer getParent(int id) {
        return null;
    }

    @Override
    public List<Integer> getChildren(int id) {
        return null;
    }

    @Override
    public List<String> getKeys(String key) {
        return null;
    }

    @Override
    public Map<String, String> getProperties(int id) {
        return null;
    }

    @Override
    public Integer getIdFromPooledProperties(String prop) {
        return null;
    }

    @Override
    public Integer getRootIndex() {
        return 997;
    }

    @Override
    public Color getColor(int id) {
        return null;
    }

    @Override
    public String getNamingDisplayProperty() {
        return null;
    }

    @Override
    public Integer getOriginalId(int id) {
        return null;
    }

}
