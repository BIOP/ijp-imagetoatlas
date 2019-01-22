package ch.epfl.biop.atlastoimg2d;

import org.scijava.object.ObjectService;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ij.ImagePlus;

public interface AtlasToImg2D<T> { // T = image type (Image Plus or QuPath Image or BDV
	
	//
	void setInteractive(boolean flag);
	
	// ---- Initialization : an Image and an Atlas
	void setAtlas(BiopAtlas ba);
	//void setImage(T img); // Let's try to handle QuPath and Fiji
	boolean isInitialized(); // Are the Atlas and the image set ?
	
	// ---- Registration
	void setAtlasLocation(Object location);
	void register(T img);
	void resetRegistration();
	boolean isRegistrationSet(); // flag if registration is set
	
	// ---- Transformations
	// ConvertibleRois transformRoisAtlasToImg(ConvertibleRois in);
	//T transformImgToAtlas();
	void putRoisToImageJ(ConvertibleRois cr);
	void putRoisToQuPath(ConvertibleRois cr);
	void setObjectService(ObjectService os);
}
