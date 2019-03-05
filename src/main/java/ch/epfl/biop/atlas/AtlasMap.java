package ch.epfl.biop.atlas;

import java.net.URL;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineTransform3D;

public interface AtlasMap {
	
	void initialize(String atlasName);
	void setDataSource(URL dataSource);
	URL getDataSource();
	
	void show();
	void hide();

	void setStructureImageChannel(int channel_index);

	ImagePlus getCurrentStructuralImage();
	ImagePlus getCurrentLabelImage();

	Object getCurrentLocation();
	void setCurrentLocation(Object location);
	
}
