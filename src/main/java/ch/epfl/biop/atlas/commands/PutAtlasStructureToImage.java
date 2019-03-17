package ch.epfl.biop.atlas.commands;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import org.scijava.command.Command;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

//TODO : adds a distinction between right and left ROI
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Put Structure to Roi Manager")
public class PutAtlasStructureToImage implements Command {
	@Parameter
	BiopAtlas atlas; // To get the ontology
	
	@Parameter(label="Pre loaded ROIs")
    ConvertibleRois cr;
    
    @Parameter(label="name, acronym, id. Can be a comma separated list. Ex: TH, HIP")
    String structure_list="";

    @Parameter
    Boolean addDescendants=false;

    @Parameter
    Boolean addAncestors=false;

    @Parameter
    Boolean addLeavesOnly=false;

    @Parameter
    Boolean clearRoiManager=false;

    @Parameter(label="Roi Naming",choices={"name","acronym","id","Roi Manager Index (no suffix)"})
    String namingChoice;
    
    @Parameter
    String roiPrefix="";

    //@Parameter(label="Right Left",choices={"Both","Left","Right"})
    //String rlChoice;

    @Parameter
    boolean addRLSuffix=false;

    @Parameter
    boolean outputLabelImage;

	@Override
	public void run() {
        HashSet<Integer> ids = new HashSet<>();
        
        // User may have entered a comma separated list
		StringTokenizer st = new StringTokenizer(structure_list,",");
		
		// Fetch all structures required by the user and put structure ids into a HashSet
		while (st.hasMoreTokens()) {
	        Integer structureId=-1;
			String structure_key = st.nextToken().trim().toUpperCase();
			if (atlas.ontology.getIdFromPooledProperties(structure_key)!=null) {
			//if (AtlasDataPreferences.ontologyIdentifierToStructureId.containsKey(structure_key)) {
                //structureId = AtlasDataPreferences.ontologyIdentifierToStructureId.get(structure_key);
                structureId = atlas.ontology.getIdFromPooledProperties(structure_key);
				System.out.println("Fetching structure id = "+structureId);
    	        ids.add(structureId);
    	        if (addDescendants) ids.addAll(atlas.ontology.getAllChildren(structureId));
    	        if (addAncestors) ids.addAll(atlas.ontology.getAllParents(structureId));
    	        if (addLeavesOnly) {
    	        	ids.remove(structureId);
    	        	ids.addAll(atlas.ontology.getAllLeaves(structureId));
    	        }
			} else {
                System.err.println("Structure key"+structure_key+" not found in ontology, abort command.");
            }   
		}

		// Convert  regions of interest to roi ArrayList
        IJShapeRoiArray rois = (IJShapeRoiArray) cr.to(IJShapeRoiArray.class);

        /*rois.rois.forEach( roi -> {
            roi.setName(roi.getName().replaceAll("(\\d+)(-)(\\d+)","$1")); // changes "123-5" to "123"
        });*/
        // Gets the Roi Manager
        RoiManager roiManager = RoiManager.getRoiManager();
        if (roiManager==null) {
            roiManager = new RoiManager();
        }
        if (clearRoiManager) roiManager.reset();
        RoiManager finalRoiManager = roiManager;
        
        // Duplicate ROIs (of interest for our purpose) into RoiManager with user specified naming convention
        rois.rois.stream()
                .map (roi -> roi.getRoi())
        .filter(roi -> isInteger(roi.getName()))
        .filter(roi -> ids.contains(Integer.valueOf(roi.getName())))
        .forEach(roi -> {
            String name = atlas.ontology.getProperties(Integer.valueOf(roi.getName())).get(namingChoice);
            roi.setName(name);
            finalRoiManager.addRoi(roi);
        });

        if (namingChoice.equals("Roi Manager Index (no suffix)")) {
            for (int i=0;i<roiManager.getCount();i++) {
                roiManager.getRoi(i).setName(roiPrefix+Integer.toString(i));
            }
        }

        if (outputLabelImage) {
            ((ImagePlus) cr.to(ImagePlus.class)).show();
        }		
	}
	
    public boolean isInteger( String input ) {
        try {
            Integer.parseInt( input );
            return true;
        }
        catch( Exception e ) {
            return false;
        }
    }
	
}
