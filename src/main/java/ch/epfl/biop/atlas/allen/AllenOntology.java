package ch.epfl.biop.atlas.allen;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import ch.epfl.biop.atlas.AtlasOntology;

public class AllenOntology implements AtlasOntology {

	static File cachedJSONFile;


    public Integer getRootIndex() {
        return new Integer(8); // 997 -1 ou 1 ou 8
    }

    @Override
    public String getNamingDisplayProperty() {
        return "acronym";
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
	    fetchOntologyJSON();	
	}
	
    public void fetchOntologyJSON() {
        // Could be in resources
       if (ontologyJSON==null) {
           //System.out.print("Fetching ontology...");
    	   
           ontologyJSON = new JSONObject(jsonGetRequest(
        		   //"http://api.brain-map.org/api/v2/structure_graph_download/1.json"
        		   this.getDataSource().toString()
        		   ));
           //System.out.println("Tidy names, acronym, id, into hash map");
           putOntologyIntoHashMaps();
       }
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
	
	public JSONObject ontologyJSON;// = null; // Ontology JSON Object 1
    public Map<String, Integer> ontologyNameToId;
    public Map<String, Integer> ontologyAcronymToId;    
    public Map<String, Integer> ontologyIdentifierToStructureId; // Keys merged Name, acronym, structure Id

    public Map<Integer, String> ontologyIdToName;
    public Map<Integer, String> ontologyIdToAcronym;

    public Map<Integer, Integer> ontologyIdToParentId;
    public Map<Integer, List<Integer>> ontologyIdToChildrenIds;
    
    public static ArrayList<String> properties;// = {"name", "acronym"};
    
    void putOntologyIntoHashMaps() {
        ontologyNameToId = new HashMap<>();
        ontologyAcronymToId = new HashMap<>();
        ontologyIdToName = new HashMap<>();
        ontologyIdToAcronym = new HashMap<>();
        ontologyIdToParentId = new HashMap<>();
        ontologyIdToChildrenIds = new HashMap<>();
        ontologyIdentifierToStructureId = new HashMap<>();
        JSONObject root = (JSONObject) ontologyJSON.getJSONArray("msg").get(0);
        registerOntologyObject(root, -1);
    }
	
    void registerOntologyObject(JSONObject obj, int idOrigin) {
        JSONArray structures = (JSONArray) obj.get("children");
        ArrayList<Integer> childrenIds = new ArrayList<>();
        int id;
        String name, acronym;
        for (int i=0;i<structures.length();i++) {
            JSONObject jsonObject = (JSONObject) structures.get(i);
            id = jsonObject.getInt("id");
            childrenIds.add(id);
            name = jsonObject.getString("name");
            acronym = jsonObject.getString("acronym");
            ontologyIdToAcronym.put(id, acronym);
            ontologyAcronymToId.put(acronym.trim().toUpperCase(), id);
            ontologyIdToName.put(id, name);
            ontologyIdentifierToStructureId.put(name.trim().toUpperCase(), id);
            ontologyIdentifierToStructureId.put(acronym.trim().toUpperCase(), id);
            ontologyIdentifierToStructureId.put(Integer.toString(id), id);
            ontologyNameToId.put(name.trim().toUpperCase(), id);
            if (jsonObject.has("parent_structure_id")) {
                //id_Parent = jsonObject.getString("parent_structure_id");
                ontologyIdToParentId.put(id, jsonObject.getInt("parent_structure_id"));
            }
            registerOntologyObject(jsonObject, id);//.getJSONObject("children"));
        }
        //}
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
    	return null;
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
	public List<Integer> getChildren(int id) {
		return this.ontologyIdToChildrenIds.get(id);
	}
	
}
