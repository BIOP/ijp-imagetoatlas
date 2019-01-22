package ch.epfl.biop.atlas.commands;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.ROIReShape;
import ch.epfl.biop.java.utilities.roi.SelectToROIKeepLines;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>ConstructROIs")
public class ConstructROIsFromImgLabel implements Command {
	@Parameter
	public BiopAtlas atlas; // To get the ontology
	
	@Parameter
	public ImagePlus labelImg;

	@Parameter
	public boolean smoothen=true;
	
	@Parameter(type = ItemIO.OUTPUT)
	public ConvertibleRois cr_out;
	
	@Parameter
	public ObjectService os;
	
	@Override
	public void run() {

		ArrayList<Roi> roiArray = new ArrayList<>();
		ImageProcessor ip = labelImg.getProcessor();
		float[][] pixels = ip.getFloatArray();
		
		HashSet<Float> existingPixelValues = new HashSet<>();
		for (int x=0;x<ip.getWidth();x++) {
			for (int y=0;y<ip.getHeight();y++) {
				existingPixelValues.add((pixels[x][y]));
			}
		}

		HashSet<Integer> possibleValues = new HashSet<>();
		existingPixelValues.forEach(id -> {
			possibleValues.addAll(atlas.ontology.getAllParents((int)(float)id));
		});
		
		Map<Integer, ArrayList<Integer>> childrenContained = new HashMap<>();
		
		atlas.ontology.getParentToChildrenMap().forEach((k,v) -> {
		//AtlasDataPreferences.ontologyIdToChildrenIds.forEach((k, v) -> {
			ArrayList<Integer> filtered = new ArrayList<>();
			filtered.addAll(v.stream().filter(id -> possibleValues.contains(id)).collect(Collectors.toList()));
			childrenContained.put(k, filtered);
		});
		
		HashSet<Integer> isLeaf = new HashSet<>();
		childrenContained.forEach((k,v) -> {
			if (v.size()==0) {
				isLeaf.add(k);
			}
		});
		
		FloatProcessor fp = new FloatProcessor(ip.getWidth(), ip.getHeight());	
		fp.setFloatArray(pixels);
		ImagePlus imgFloatCopy = new ImagePlus("FloatLabel",fp);
		
		boolean[][] movablePx = new boolean[ip.getWidth()+1][ip.getHeight()+1];
		for (int x=1;x<ip.getWidth();x++) {
			for (int y=1;y<ip.getHeight();y++) {
				boolean is3Colored = false;
				boolean isCrossed = false;
				float p1p1 = pixels[x][y];
				float p1m1 = pixels[x][y-1];
				float m1p1 = pixels[x-1][y];
				float m1m1 = pixels[x-1][y-1];
				float min = p1p1;
				if (p1m1<min) min = p1m1;
				if (m1p1<min) min = m1p1;
				if (m1m1<min) min = m1m1;
				float max = p1p1;
				if (p1m1>max) max = p1m1;
				if (m1p1>max) max = m1p1;
				if (m1m1>max) max = m1m1;
				if (min!=max) {
					if ((p1p1!=min)&&(p1p1!=max)) is3Colored=true; 
					if ((m1p1!=min)&&(m1p1!=max)) is3Colored=true; 
					if ((p1m1!=min)&&(p1m1!=max)) is3Colored=true; 
					if ((m1m1!=min)&&(m1m1!=max)) is3Colored=true;
					
					if (!is3Colored) {
						if ((p1p1==m1m1)&&(p1m1==m1p1)) {
							isCrossed=true;
						}
					}
				} // if not it's monocolored
				movablePx[x][y]=(!is3Colored)&&(!isCrossed);
			}
		}
		

		//SelectToROIKeepLines.filterMergable=true;
		//SelectToROIKeepLines.splitable = movablePx;
		
		boolean containsLeaf=true;
		while (containsLeaf) {
			List<Float> leavesValues = existingPixelValues
				.stream()
				.filter(v -> isLeaf.contains((int) (float) v))
				.collect(Collectors.toList());
			leavesValues.forEach(v -> {
					//ip.setThreshold( v,v,ImageProcessor.NO_LUT_UPDATE);
					fp.setThreshold( v,v,ImageProcessor.NO_LUT_UPDATE);
				
					fp.setThreshold( v,v,ImageProcessor.NO_LUT_UPDATE);
					Roi roi = SelectToROIKeepLines.run(imgFloatCopy, movablePx, true);//ThresholdToSelection.run(imgFloatCopy);
					
					
					//Roi roi = ThresholdToSelection.run(labelImg);
					roi.setName(Integer.toString((int) (double) v));
					roiArray.add(roi);
					if (atlas.ontology.getParentToParentMap().containsKey((int) (double)v)) {
						int parentId = atlas.ontology.getParentToParentMap().get((int) (double)v);
						//ip.setColor(parentId);
						//ip.fill(roi);
						fp.setColor(parentId);
						fp.fill(roi);
						if (childrenContained.get(parentId)!=null) {
							childrenContained.get(parentId).remove(new Integer((int) (float) v));
							existingPixelValues.add((float)parentId);	
						}
					}
				}
			);
			existingPixelValues.removeAll(leavesValues);
			leavesValues.stream().map(v -> new Integer((int)(float)v)).forEach(e ->
				childrenContained.remove(e));
				isLeaf.clear();
				childrenContained.forEach((k,v) -> {
					if (v.size()==0) {
						isLeaf.add(k);
					}
				}
			);
			containsLeaf = existingPixelValues.stream().anyMatch(v -> isLeaf.contains((int) (float) v));
		}
		
		
		
		
		
		cr_out = new ConvertibleRois();
		ArrayList<Roi> roiArrayCV = ConvertibleRois.convertRoisToPolygonRois(roiArray); // Dissociates ShapeROI into multiple Polygon Rois
		

		
		roiArrayCV.replaceAll(roi -> ROIReShape.smoothenWithConstrains(roi, movablePx));
		roiArrayCV.replaceAll(roi -> ROIReShape.smoothenWithConstrains(roi, movablePx));
		
		
		
		
		
		
		
		
		
		
		
		//if (smoothen) {
		//	roiArrayCV.replaceAll(r -> ROIReShape.smoothen(r)); // smoothen ROI
		//}
		//if (resampleRoiLength>0) {
		//	roiArrayCV.replaceAll(r -> ROIReShape.reSample(r,this.resampleRoiLength)); // resample roi
		//}
		cr_out.set(roiArrayCV);
		cr_out.to(RoiManager.class);
		if (os!=null) {
			os.addObject(cr_out);
		} else {
			System.err.println("Object Service not set");
		}
	}

}
