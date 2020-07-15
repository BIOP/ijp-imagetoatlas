package ch.epfl.biop.atlas.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import ij.IJ;
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

	//RoiManager roiManager;

	@Override
	public void run() {
		// Gets pixel array and convert them to Float -> no loss of precision for int 16 or
		// even RGB 24
		ArrayList<Roi> roiArray = new ArrayList<>();
		ImageProcessor ip = labelImg.getProcessor();
		float[][] pixels = ip.getFloatArray();

		// Gets all existing values in the image
		HashSet<Float> existingPixelValues = new HashSet<>();
		for (int x=0;x<ip.getWidth();x++) {
			for (int y=0;y<ip.getHeight();y++) {
				existingPixelValues.add((pixels[x][y]));
			}
		}

		// All the parents of the existing label will be met at some point
		// keep a list of possible values encountered in the tree
		HashSet<Integer> possibleValues = new HashSet<>();
		existingPixelValues.forEach(id -> {
			possibleValues.addAll(atlas.ontology.getAllParents((int)(float)id));
			possibleValues.add((int)(float)id);
		});

		// Goes opposite
		// List all the values of the children that will be encountered for each label
		// For instance some children node will never be met because they are not part of the slice
		Map<Integer, ArrayList<Integer>> childrenContained = new HashMap<>();
		atlas.ontology.getParentToChildrenMap().forEach((k,v) -> {
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
		boolean containsLeaf=true;
		HashSet<Float> monitored = new HashSet<Float>();
		/*monitored.add(945f);
		monitored.add(369f);
		monitored.add(322f);*/

		/*roiManager = RoiManager.getRoiManager();
		if (roiManager==null) {
			roiManager = new RoiManager();
		}
		roiManager.reset();*/

		while (containsLeaf) {
			List<Float> leavesValues = existingPixelValues
				.stream()
				.filter(v -> isLeaf.contains((int) (float) v))
				.collect(Collectors.toList());
			leavesValues.forEach(v -> {
					fp.setThreshold( v,v,ImageProcessor.NO_LUT_UPDATE);
					Roi roi = SelectToROIKeepLines.run(imgFloatCopy, movablePx, true);//ThresholdToSelection.run(imgFloatCopy);

					roi.setName(Integer.toString((int) (double) v));
					roiArray.add(roi);

					//roiManager.addRoi(roi);

					/*if (monitored.contains(v)) {
						System.out.println("id="+v+" step 0");
						roiManager.addRoi(roi);
					}*/

					if (atlas.ontology.getParentToParentMap().containsKey((int) (double)v)) {

						int parentId = atlas.ontology.getParentToParentMap().get((int) (double)v);
						if (monitored.contains(v)) {
							System.out.println("id="+v+" has a parent : "+parentId);
						}
						fp.setColor(parentId);
						fp.fill(roi);
						if (childrenContained.get(parentId)!=null) {
							if (childrenContained.get(new Integer((int) (float)v)).size()==0) {
								childrenContained.get(parentId).remove(new Integer((int) (float) v));
							}
							existingPixelValues.add((float)parentId);
						}
					} else {
						if (monitored.contains(v)) {
							System.out.println("id="+v+" has no parent!");
						}
					}
				}
			);
			existingPixelValues.removeAll(leavesValues);
			leavesValues.stream().map(v -> new Integer((int)(float)v)).forEach(e -> childrenContained.remove(e));
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

		IJShapeRoiArray output = new IJShapeRoiArray(roiArray);
		//output.smoothenWithConstrains(movablePx);
		//output.smoothenWithConstrains(movablePx);

		cr_out.set(output);
		if (os!=null) {
			os.addObject(cr_out);
		} else {
			System.err.println("Object Service not set");
		}
	}

}
