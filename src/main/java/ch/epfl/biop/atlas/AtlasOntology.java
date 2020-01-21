package ch.epfl.biop.atlas;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface AtlasOntology {
	
	void initialize();
	void setDataSource(URL dataSource);
	URL getDataSource();
	
	Map<Integer,List<Integer>> getParentToChildrenMap();
	Map<Integer,Integer> getParentToParentMap();
	List<Integer> getAllLeaves(int id);
	List<Integer> getAllChildren(int id);
	List<Integer> getAllParents(int id);
	Integer getParent(int id);
	List<Integer> getChildren(int id);
	List<String> getKeys(String key);
	Map<String, String> getProperties(int id);
	Integer getIdFromPooledProperties(String prop);
	Integer getRootIndex();

	String getNamingDisplayProperty();

}
