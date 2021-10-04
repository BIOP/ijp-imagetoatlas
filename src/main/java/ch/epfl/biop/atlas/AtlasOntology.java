package ch.epfl.biop.atlas;

import java.awt.*;
import java.net.URL;

public interface AtlasOntology {
	
	void initialize() throws Exception;

	void setDataSource(URL dataSource);

	URL getDataSource();

	AtlasNode getRoot();

	Color getColor(AtlasNode node);

	AtlasNode getNodeFromLabelMap(int mapValue);

	AtlasNode getNodeFromId(int id);

}
