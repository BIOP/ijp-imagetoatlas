package ch.epfl.biop.atlas;

import java.io.Closeable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;

abstract public class BiopAtlas implements Closeable {

    // An atlas is : an ontology and an xml hdf5 data source

    //--------------------------- Source
    // Sources contains the xml hdf5 label image, leaves only data
    // Source -> then several imaging modalities Different visualisation
    public AtlasMap map;
    
    //--------------------------- Ontology
    public AtlasOntology ontology;
    
    abstract public void initialize(URL mapURL, URL ontologyURL);
    
    abstract public void runOnClose(Runnable onClose);
    
    /*public void browse() {
    	map.show();
    }*/
    
    boolean isInteger( String input ) {
        try {
            Integer.parseInt( input );
            return true;
        }
        catch( Exception e ) {
            return false;
        }
    }
    
    abstract public ConvertibleRois getCurrentROIs();
    
    public void putROISToROIManager(
    		ConvertibleRois cr,
    		//ImagePlus maskImage, // For region filtering (for instance left / right)
    		String structure_list,
    		Boolean addDescendants,
    		Boolean addAncestors,
    		Boolean addLeavesOnly,
    		Boolean clearRoiManager,
    		String namingChoice,
    		String roiPrefix
    		) 
    {
    	// HashSet structure avoids duplicate entry
        HashSet<Integer> ids = new HashSet<>();
        
        // User may have entered a comma separated list
		StringTokenizer st = new StringTokenizer(structure_list,",");
		
		// Fetch all structures required by the user and put structure ids into a HashSet
		while (st.hasMoreTokens()) {
	        Integer structureId=-1;
			String structure_key = st.nextToken().trim().toUpperCase();
			if (ontology.getIdFromPooledProperties(structure_key)!=null) {
			//if (AtlasDataPreferences.ontologyIdentifierToStructureId.containsKey(structure_key)) {
                structureId = ontology.getIdFromPooledProperties(structure_key);
                System.out.println("Fetching structure id = "+structureId);
    	        ids.add(structureId);
    	        if (addDescendants) ids.addAll(ontology.getAllChildren(structureId));
    	        if (addAncestors) ids.addAll(ontology.getAllParents(structureId));
    	        if (addLeavesOnly) {
    	        	ids.remove(structureId);
    	        	ids.addAll(ontology.getAllLeaves(structureId));
    	        }
			} else {
                System.err.println("Structure key"+structure_key+" not found in ontology, abort command.");
            }   
		}

		// Convert  regions of interest to roi ArrayList
        ArrayList<Roi> rois = (ArrayList<Roi>) cr.to(ArrayList.class);

        rois.forEach( roi -> {
            roi.setName(roi.getName().replaceAll("(\\d+)(-)(\\d+)","$1")); // changes "123-5" to "123"
        });
        
        // Gets the Roi Manager
        RoiManager roiManager = RoiManager.getRoiManager();
        if (roiManager==null) {
            roiManager = new RoiManager();
        }
        if (clearRoiManager) roiManager.reset();
        RoiManager finalRoiManager = roiManager;
        
        // Duplicate ROIs (of interest for our purpose) into RoiManager with user specified naming convention
        rois.stream()
        .filter(roi -> isInteger(roi.getName()))
        .filter(roi -> ids.contains(Integer.valueOf(roi.getName()))).forEach(roi -> {
            FloatPolygon pol = roi.getFloatPolygon();
            PolygonRoi roiOut = new PolygonRoi(pol.xpoints.clone(), pol.ypoints.clone(), pol.npoints, ij.gui.Roi.POLYGON );
            roiOut.setStrokeColor(roi.getStrokeColor());

            Integer id = Integer.valueOf(roi.getName());
            roiOut.setName(roiPrefix + ontology.getProperties(id).get(namingChoice));
            finalRoiManager.addRoi(roiOut);
        } );

        if (namingChoice.equals("Roi Manager Index (no suffix)")) {
            for (int i=0;i<roiManager.getCount();i++) {
                roiManager.getRoi(i).setName(roiPrefix+Integer.toString(i));
            }
        }
    }


}
