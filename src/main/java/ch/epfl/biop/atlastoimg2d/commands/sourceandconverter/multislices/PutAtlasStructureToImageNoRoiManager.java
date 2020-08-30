package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

//TODO : adds a distinction between right and left ROI
public class PutAtlasStructureToImageNoRoiManager implements Command {
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

    @Parameter(label="Roi Naming",choices={"name","acronym","id","Roi Manager Index (no suffix)"})
    public String namingChoice;

    @Parameter
    public String roiPrefix="";

    public IJShapeRoiArray output;

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

        // Gets the Roi Manager
        List<Roi> listOut = new ArrayList<>();
        
        // Duplicate ROIs (of interest for our purpose) into RoiManager with user specified naming convention
        rois.rois.stream()
                .map (roi -> roi.getRoi())
        .filter(roi -> isInteger(roi.getName()))
        .filter(roi -> ids.contains(Integer.valueOf(roi.getName())))
        .forEach(roi -> {
            String name = atlas.ontology.getProperties(Integer.valueOf(roi.getName())).get(namingChoice);
            roi.setName(name);
            listOut.add(roi);
        });

        if (namingChoice.equals("Roi Manager Index (no suffix)")) {
            for (int i=0;i<listOut.size();i++) {
                listOut.get(i).setName(roiPrefix+Integer.toString(i));
            }
        }

        output = new IJShapeRoiArray(listOut);

        for (int i=0;i<output.rois.size();i++) {
            System.out.println("rename");
            System.out.println(rois.rois.get(i).getRoi().getProperty("hierarchy"));
            output.rois.get(i).getRoi().setProperty("hierarchy", rois.rois.get(i).getRoi().getProperty("hierarchy"));
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
