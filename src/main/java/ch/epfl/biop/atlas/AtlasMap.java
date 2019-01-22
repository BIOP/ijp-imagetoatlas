package ch.epfl.biop.atlas;

import java.net.URL;

import ij.ImagePlus;
import net.imglib2.realtransform.AffineTransform3D;

public interface AtlasMap {
	
	public void initialize(String atlasName);
	public void setDataSource(URL dataSource);
	public URL getDataSource();
	
	public void show();
	public void hide();
	
	public ImagePlus getCurrentStructuralImage();
	public ImagePlus getCurrentLabelImage();

	public Object getCurrentLocation();
	public void setCurrentLocation(Object location);
	
}
