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

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

//TODO : adds a distinction between right and left ROI
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Put Structure to Roi Manager")
public class PutAtlasStructureToImage implements Command {
	@Parameter
	public BiopAtlas atlas; // To get the ontology
	
	@Parameter(label="Pre loaded ROIs")
    public ConvertibleRois cr;
    
    @Parameter(label="name, acronym, id. Can be a comma separated list. Ex: TH, HIP")
    public String structure_list="";

    @Parameter
    public Boolean addDescendants=false;

    @Parameter
    public Boolean addAncestors=false;

    @Parameter
    public Boolean clearRoiManager=false;

    @Parameter(label="Roi Naming",choices={"name","acronym","id","Roi Manager Index (no suffix)"})
    public String namingChoice;
    
    @Parameter
    public String roiPrefix="";

    //@Parameter(label="Right Left",choices={"Both","Left","Right"})
    //String rlChoice;

    @Parameter
    boolean outputLabelImage;

    public RoiManager roiManagerIn = null;

	@Override
	public void run() {
        HashSet<Integer> ids = new HashSet<>();
        
        // User may have entered a comma separated list
		StringTokenizer st = new StringTokenizer(structure_list,",");
		
		// Fetch all structures required by the user and put structure ids into a HashSet
		while (st.hasMoreTokens()) {
	        Integer structureId;
			String structure_key = st.nextToken().trim().toUpperCase();
			if (atlas.ontology.getIdFromPooledProperties(structure_key)!=null) {
			//if (AtlasDataPreferences.ontologyIdentifierToStructureId.containsKey(structure_key)) {
                //structureId = AtlasDataPreferences.ontologyIdentifierToStructureId.get(structure_key);
                structureId = atlas.ontology.getIdFromPooledProperties(structure_key);
				System.out.println("Fetching structure id = "+structureId);
    	        ids.add(structureId);
    	        if (addDescendants) ids.addAll(atlas.ontology.getAllChildren(structureId));
    	        if (addAncestors) ids.addAll(atlas.ontology.getAllParents(structureId));
    	        /*if (addLeavesOnly) {
    	        	ids.remove(structureId);
    	        	ids.addAll(atlas.ontology.getAllLeaves(structureId));
    	        }*/
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
        if (roiManagerIn!=null) roiManager = roiManagerIn;
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
            // The line below do not work because a pixel can have multiple indexes
            //((ImagePlus) cr.to(ImagePlus.class)).show();

            System.err.println("Impossible to output a correct label image\n" +
                    " !!!!! OUTPUTING a label image is not possible because:\n" +
                    "            // * We cannot output an image with enough range because some ids are above > 65536\n" +
                    "            // * We can output an image with 32 bits float, but there is not enough precsion because\n" +
                    "            // some ids are very big but with very little difference :\n" +
                    "            // Like:\n" +
                    "            // * 41234567891231\n" +
                    "            // * 41234567891234\n" +
                    "            // This difference is lost in translation in float 32 bits...");

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
