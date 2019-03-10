package ch.epfl.biop.atlastoimg2d;

import ch.epfl.biop.wrappers.elastix.RegisterHelper;
import org.scijava.Context;
import org.scijava.object.ObjectService;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;

import java.io.File;
import java.net.URL;

// Keep a link or even a hard file to image src and dest

public interface AtlasToImg2D<T> { // T = image type (Image Plus or QuPath Image or BDV
	
	//
	void setInteractive(boolean flag);
	
	// ---- Initialization : an Image and an Atlas
	void setAtlas(BiopAtlas ba);
	BiopAtlas getAtlas();


	boolean isInitialized(); // Are the Atlas and the image set ?
	
	// ---- Registration
    void setAtlasLocation(Object location);
	Object getAtlasLocation(); // returns null if not set


    void setImage(T img); // Let's try to handle QuPath and Fiji
	T getImage();
	void register();
	void resetRegistration();
	boolean isRegistrationSet(); // flag if registration is set
	Object getRegistration();
	
	// ---- Transformations
	// ConvertibleRois transformRoisAtlasToImg(ConvertibleRois in);
	//T transformImgToAtlas();
	void putRoisToImageJ(ConvertibleRois cr);
	void putRoisToQuPath(ConvertibleRois cr);
	void setScijavaContext(Context ctx);

	File save(String path);
	void load(URL url);
	void load(File file);

	// WORKFLOW:
	// -> Get Atlas slice location in 2D
	// -> Get Atlas structure slice
	// -> Get registration
	// -> Transform label image according to registration
	// -> Compute ROIs
	// -> Display ROIs from any structure to any image



}
