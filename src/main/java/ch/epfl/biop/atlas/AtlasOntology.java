package ch.epfl.biop.atlas;

import java.net.URL;
import java.util.List;
import java.util.Map;

public interface AtlasOntology {
	
	public void initialize();
	public void setDataSource(URL dataSource);
	public URL getDataSource();
	
	public Map<Integer,List<Integer>> getParentToChildrenMap(); 
	public Map<Integer,Integer> getParentToParentMap(); 
	public List<Integer> getAllLeaves(int id);
	public List<Integer> getAllChildren(int id);
	public List<Integer> getAllParents(int id);
	public Integer getParent(int id);
	public List<Integer> getChildren(int id);
	public List<String> getKeys(String key);
	public Map<String, String> getProperties(int id);
	public Integer getIdFromPooledProperties(String prop);
	public Integer getRootIndex();

	public String getNamingDisplayProperty();

}
