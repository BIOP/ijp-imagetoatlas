package ch.epfl.biop.atlas.allen;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import ch.epfl.biop.atlas.aligner.serializers.RegistrationAdapter;
import org.json.JSONArray;
import org.json.JSONObject;

import ch.epfl.biop.atlas.AtlasOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * What a terrible mess, but ConstructROIsFromImgLabel has to be fixed
 * before we can simplify this and the {@link AtlasOntology} interface
 *
 * @author ashamed of this mess, not putting his name
 */

public class AllenOntology implements AtlasOntology {

    protected static Logger logger = LoggerFactory.getLogger(AllenOntology.class);

	static File cachedJSONFile;

    public Integer getRootIndex() {
        return new Integer(997); // 997 -1 ou 1 ou 8
    }

    @Override
    public Color getColor(int id) {
        return ontologyIdToColor.get(id);
    }

    @Override
    public String getNamingDisplayProperty() {
        return "acronym";
    }

    @Override
    public Integer getOriginalId(int id) {
        return ontologyIdToOriginalId.get(id);
    }

    @Override
	public List<String> getKeys(String key) {
		return properties;
	}

	@Override
	public Map<String, String> getProperties(int id) {
		HashMap<String,String> hm = new HashMap<>();
		hm.put("name", this.ontologyIdToName.get(id));
		hm.put("acronym", this.ontologyIdToAcronym.get(id));
		hm.put("id", Integer.toString(id));
		return hm;
	}
	
	@Override
	public void initialize() {
		properties = new ArrayList<>();
	    properties.add("name");
	    properties.add("acronym");
	    properties.add("id");
	    //properties.add("color_hex_triplet");
	    fetchOntologyJSON();
	}

    public void fetchOntologyJSON() {
        // Could be in resources
       if (ontologyJSON==null) {
           logger.debug("Fetching ontology...");
    	   
           ontologyJSON = new JSONObject(jsonGetRequest(
        		   //"http://api.brain-map.org/api/v2/structure_graph_download/1.json"
        		   this.getDataSource().toString()
        		   ));
           logger.debug("Tidy names, acronym, id, into hash map");
           putOntologyIntoHashMaps();
       }
    }
    
    public static String jsonGetRequest(String urlQueryString) {
        String json = null;
        try {
            URL url = new URL(urlQueryString);
            
            URLConnection connection = url.openConnection();
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
        return text;
    }
	
	public JSONObject ontologyJSON;// = null; // Ontology JSON Object 1
    public Map<String, Integer> ontologyNameToId;
    public Map<String, Integer> ontologyAcronymToId;    
    public Map<String, Integer> ontologyIdentifierToStructureId; // Keys merged Name, acronym, structure Id

    public Map<Integer, String> ontologyIdToName;
    public Map<Integer, String> ontologyIdToAcronym;

    public Map<Integer, Color> ontologyIdToColor;
    public Map<Integer, Integer> ontologyIdToParentId;
    public Map<Integer, List<Integer>> ontologyIdToChildrenIds;
    
    public static ArrayList<String> properties;// = {"name", "acronym"};
    public Map<Integer, Integer> ontologyIdToOriginalId; // Used to retrieve the original index if the command modulo has been used


    void putOntologyIntoHashMaps() {
        ontologyNameToId = new HashMap<>();
        ontologyAcronymToId = new HashMap<>();
        ontologyIdToName = new HashMap<>();
        ontologyIdToAcronym = new HashMap<>();
        ontologyIdToParentId = new HashMap<>();
        ontologyIdToChildrenIds = new HashMap<>();
        ontologyIdentifierToStructureId = new HashMap<>();
        ontologyIdToOriginalId = new HashMap();
        ontologyIdToColor = new HashMap<>();

        JSONObject root = (JSONObject) ontologyJSON.getJSONArray("msg").get(0);

        // ------ fix root node

        int id = root.getInt("id");
        String name = root.getString("name");
        String acronym = root.getString("acronym");
        ontologyIdToAcronym.put(id, acronym);
        ontologyIdToOriginalId.put(id,id);
        ontologyAcronymToId.put(acronym.trim().toUpperCase(), id);
        ontologyIdToName.put(id, name);
        ontologyIdentifierToStructureId.put(name.trim().toUpperCase(), id);
        ontologyIdentifierToStructureId.put(acronym.trim().toUpperCase(), id);
        ontologyIdentifierToStructureId.put(Integer.toString(id), id);
        ontologyNameToId.put(name.trim().toUpperCase(), id);

        // ------ end fix root node


        registerOntologyObject(root, 997);
    }

    /**
     * For some weird reason, ids in the allen brain ontology have huge indices:
     * 4584253562 for instance. These values, when converted to float, are losing precision,
     * which means that they become useless.
     *
     * With this function, every index is transformed by its modulo of keymodulo
     * Turns out the modulo 65000 has no duplicate index. Contrary to the modulo 65536
     * which has 1 duplicate (1105) for instance.
     *
     * The annotation map has to be converted the same way...
     * @param keyModulo modulo number to id number of the atlas
     */
    public void mutateToModulo(int keyModulo) {

        ontologyIdToName = mutateMapKeysModulo(ontologyIdToName, keyModulo, new String());
        ontologyIdToAcronym = mutateMapKeysModulo(ontologyIdToAcronym, keyModulo, new String());
        ontologyIdToParentId = mutateMapKeysModulo(ontologyIdToParentId, keyModulo, new Integer(0));
        ontologyIdToOriginalId = mutateMapKeysModulo(ontologyIdToOriginalId, keyModulo, new Integer(0));
        ontologyIdToChildrenIds = mutateMapKeysModulo(ontologyIdToChildrenIds, keyModulo, new ArrayList<Integer>(){});
        ontologyIdToColor = mutateMapKeysModulo(ontologyIdToColor, keyModulo, new Color(255,255,255));


        ontologyIdToParentId = mutateMapValuesModulo(ontologyIdToParentId, keyModulo, new Integer(0));
        ontologyNameToId = mutateMapValuesModulo(ontologyNameToId, keyModulo, new String());
        ontologyAcronymToId = mutateMapValuesModulo(ontologyAcronymToId, keyModulo, new String());
        ontologyIdentifierToStructureId = mutateMapValuesModulo(ontologyIdentifierToStructureId, keyModulo, new String());


        // Case ontologyIdToChildrenIds to be handled separately
        Map<Integer, List<Integer>> map_out = new HashMap<>();
        ontologyIdToChildrenIds.forEach((k,v) -> {
            ArrayList<Integer> newChildIds = new ArrayList<>();
            map_out.put(k,newChildIds);
            v.forEach(id -> {
                newChildIds.add(id % keyModulo);
            });
        });
        ontologyIdToChildrenIds = map_out;

    }

    <T> Map<Integer, T> mutateMapKeysModulo(Map<Integer, T> map_in, int mod, T value) {
        Map<Integer,T> map_out = new HashMap<>();
        map_in.forEach((k,v) -> {
            Integer newKey = k % mod;
            if (map_out.containsKey(newKey)) {
                logger.error("Error: duplicate key k % "+mod+" = "+newKey+" k= "+k);
            } else {
                map_out.put(newKey,v);
            }
        });
        return map_out;
    }

    <T> Map<T, Integer> mutateMapValuesModulo(Map<T, Integer> map_in, int mod, T value) {
        Map<T, Integer> map_out = new HashMap<>();
        map_in.forEach((k,v) -> {
            map_out.put(k,v % mod);
        });
        return map_out;
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

    void registerOntologyObject(JSONObject obj, int idOrigin) {
        JSONArray structures = (JSONArray) obj.get("children");
        ArrayList<Integer> childrenIds = new ArrayList<>();
        int id;
        String name, acronym, colorhex;
        for (int i=0;i<structures.length();i++) {
            JSONObject jsonObject = (JSONObject) structures.get(i);
            id = jsonObject.getInt("id");
            childrenIds.add(id);
            name = jsonObject.getString("name");
            acronym = jsonObject.getString("acronym");
            colorhex = jsonObject.getString("color_hex_triplet");
            ontologyIdToAcronym.put(id, acronym);
            ontologyIdToOriginalId.put(id,id);
            ontologyIdToColor.put(id, hex2Rgb(colorhex));
            ontologyAcronymToId.put(acronym.trim().toUpperCase(), id);
            ontologyIdToName.put(id, name);
            ontologyIdentifierToStructureId.put(name.trim().toUpperCase(), id);
            ontologyIdentifierToStructureId.put(acronym.trim().toUpperCase(), id);
            ontologyIdentifierToStructureId.put(Integer.toString(id), id);
            ontologyNameToId.put(name.trim().toUpperCase(), id);
            if (jsonObject.has("parent_structure_id")) {
                ontologyIdToParentId.put(id, jsonObject.getInt("parent_structure_id"));
            }
            registerOntologyObject(jsonObject, id);
        }
        ontologyIdToChildrenIds.put(idOrigin,childrenIds);
    }

    public ArrayList<Integer> getAllLeaves(int id) {
        ArrayList<Integer> leavesId = new ArrayList<>();
        addAllLeaves(leavesId,id);
        return leavesId;
    }

    ArrayList<Integer> addAllLeaves(ArrayList<Integer> leavesId, int id) {
        if (ontologyIdToChildrenIds.get(id).size()==0) {
            leavesId.add(id);
        } else {
            ontologyIdToChildrenIds.get(id).forEach(idx -> addAllLeaves(leavesId,idx));
        }
        return leavesId;
    }

    public ArrayList<Integer> getAllChildren(int id) {
        ArrayList<Integer> childsId = new ArrayList<>();
        addAllChildren(childsId,id);
        childsId.removeIf(index -> index==id); // removes the root id
        return childsId;
    }

    ArrayList<Integer> addAllChildren(ArrayList<Integer> childsId, int id) {
        childsId.add(id);
        if (ontologyIdToChildrenIds.get(id).size()==0) {

        } else {
            ontologyIdToChildrenIds.get(id).forEach(idx -> {
                    addAllChildren(childsId,idx);
                }
            );
        }
        return childsId;
    }
    
    public Integer getParent(int id) {
    	return ontologyIdToParentId.get(id);
    }

    public ArrayList<Integer> getAllParents(int id) {
        ArrayList<Integer> parentsId = new ArrayList<>();
        addAllParents(parentsId,id);
        parentsId.removeIf(index -> index==id); // removes the root id
        return parentsId;
    }

    ArrayList<Integer> addAllParents(ArrayList<Integer> parentsId, int id) {
        parentsId.add(id);
        if (ontologyIdToParentId.containsKey(id)) {
            addAllParents(parentsId,ontologyIdToParentId.get(id));
        }
        return parentsId;
    }

	@Override
	public Integer getIdFromPooledProperties(String prop) {
		int id = Integer.MIN_VALUE;
		if (this.ontologyAcronymToId.containsKey(prop)) {
			id = this.ontologyAcronymToId.get(prop);
		}
		if (this.ontologyNameToId.containsKey(prop)) {
			id = this.ontologyNameToId.get(prop);
		}
		if (this.ontologyIdentifierToStructureId.containsKey(prop)) {
			id = this.ontologyIdentifierToStructureId.get(prop);
		}
		return id;
	}

	URL dataSource;
	
	@Override
	public void setDataSource(URL dataSource) {
		this.dataSource=dataSource;
	}

	@Override
	public URL getDataSource() {
		return dataSource;
	}

	@Override
	public Map<Integer, List<Integer>> getParentToChildrenMap() {
		return this.ontologyIdToChildrenIds;
	}
	
	@Override
	public Map<Integer, Integer> getParentToParentMap() {
		return this.ontologyIdToParentId;
	}

    @Override
    public List<Integer> getAllIds() {
        return ontologyIdToChildrenIds.keySet().stream().collect(Collectors.toList());
    }

    @Override
	public List<Integer> getChildren(int id) {
		return this.ontologyIdToChildrenIds.get(id);
	}
	
}
