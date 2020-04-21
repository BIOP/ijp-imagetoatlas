package ch.epfl.biop.atlas;

import java.net.URL;

import bdv.viewer.SourceAndConverter;
import ij.ImagePlus;

/**
 * Interface to define an AtlasMap
 *
 * Pairs with AtlasOntology
 *
 * The Atlas Map contains :
 * - 3D images among which there are:
 *     - structural images, which are different modalities acquired for an atlas -> (fluorescence, brightfield)
 *     - a single Label Image
 *
 *
 */
public interface AtlasMap {

	/**
	 * Set where to catch the source of the Atlas, remote or local
	 * @param dataSource
	 */
	void setDataSource(URL dataSource);

	/**
	 * Triggers the initialisation of the Atlas
	 * @param atlasName
	 */
	void initialize(String atlasName);

	/**
	 * For convenience
	 * @return
	 */
	URL getDataSource();
	
	void show();
	void hide();

	ImagePlus getCurrentStructuralImageAsImagePlus();
	ImagePlus getCurrentLabelImageAsImagePlus();

	SourceAndConverter[] getCurrentStructuralImageAsSacs();
	SourceAndConverter[] getCurrentLabelImageAsSacs();

	Object getCurrentLocation();
	void setCurrentLocation(Object location);
	
}
