package ch.epfl.biop.atlastoimg2d;

import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.registration.Registration;
import org.scijava.Context;

import ch.epfl.biop.atlas.BiopAtlas;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.function.Function;

public interface AtlasToImg2D<T> { // T = image type (Image Plus or QuPath Image or ImgLib2)

	// ---- Initialization : an Image and an Atlas
	void setAtlas(BiopAtlas ba);
	BiopAtlas getAtlas();
	void setScijavaContext(Context ctx);

	// ---- Initial image to be registered
	void setImage(T img); // Let's try to handle QuPath and Fiji
	T getImage();

	// ---- Registration
    void setAtlasLocation(Object location);
	Object getAtlasLocation(); // returns null if not set

	void showLastImage();
	void addRegistration(Registration<T> reg);
	void addRegistration(Registration<T> reg, Function<T,T> preprocessFixedImage, Function<T,T> preprocessMovingImage);
	void rmLastRegistration();
	void resetRegistrations();
	ArrayList<Registration<T>> getRegistrations();

	// ---- ROIs
	ConvertibleRois getTransformedRois();
	void putTransformedRoisToImageJROIManager();
	//void putTransformedRoisToObjectService();

	void save(String path);
	void load(URL url);
	void load(File file);

	// WORKFLOW:
	// -> Get Atlas slice location in 2D
	// -> Get Atlas structure slice
	// -> Get registrations
	// -> Compute ROIs
	// -> Display ROIs from any structure to any image

}
