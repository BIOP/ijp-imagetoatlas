package ch.epfl.biop.atlas.commands;

import java.util.*;
import java.util.stream.Collectors;

import ch.epfl.biop.atlas.AtlasNode;
import ch.epfl.biop.atlas.AtlasOntologyHelper;
import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.SelectToROIKeepLines;
import ij.ImagePlus;
import ij.gui.Roi;
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
			possibleValues.addAll(AtlasOntologyHelper.getAllParentLabels(atlas.ontology, (int)(float) id));
			possibleValues.add((int)(float)id);
		});

		// We should keep, for each possible values, a way to know
		// if their are some labels which belong to children labels in the image.
		Map<Integer, Set<Integer>> childrenContained = new HashMap<>();
		possibleValues.forEach(labelValue -> {
			AtlasNode node = atlas.ontology.getNodeFromLabelMap(labelValue);
			if (node == null) {
				// Get rid of background (0 -1, whatever)
				// childrenContained.put(labelValue, new HashSet<>());
			} else {
				Set<Integer> valuesMetInTheImage = node.children().stream()
						.map(n -> (AtlasNode) n)
						.map(AtlasNode::getLabelValue)
						.filter(possibleValues::contains)
						.collect(Collectors.toSet());
				childrenContained.put(labelValue, valuesMetInTheImage);
			}
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
		HashSet<Float> monitored = new HashSet<>();

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

					//if (atlas.ontology.getParentToParentMap().containsKey((int) (double)v)) {
				    if (atlas.ontology.getNodeFromLabelMap((int) (double)v)!=null) {
						AtlasNode parent = (AtlasNode) atlas.ontology.getNodeFromLabelMap((int) (double)v).parent();
						if (parent!=null) {

							int parentId = parent.getLabelValue();
							if (monitored.contains(v)) {
								//System.out.println("id="+v+" has a parent : "+parentId);
							}
							fp.setColor(parentId);
							fp.fill(roi);
							if (childrenContained.get(parentId)!=null) {
								if (childrenContained.get(new Integer((int) (float)v)).size()==0) {
									childrenContained.get(parentId).remove(new Integer((int) (float) v));
								}
								existingPixelValues.add((float)parentId);
							}
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

		roiArray.forEach(roi -> putOriginalId(roi, roi.getName()));

		IJShapeRoiArray output = new IJShapeRoiArray(roiArray);

		output.smoothenWithConstrains(movablePx);
		output.smoothenWithConstrains(movablePx);

		cr_out.set(output);
		if (os!=null) {
			os.addObject(cr_out);
		} else {
			System.err.println("Object Service not set");
		}
	}

	private void putOriginalId(Roi roi, String name) {
		int idRoi = Integer.valueOf(name);
		AtlasNode node = atlas.ontology.getNodeFromLabelMap(idRoi);
		if (node != null) {
			roi.setName(Integer.toString(atlas.ontology.getNodeFromLabelMap(idRoi).getId()));//.getOriginalId(idRoi)));
		}
	}

}
