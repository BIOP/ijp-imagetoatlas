package ch.epfl.biop.atlastoimg2d;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.commands.ConstructROIsFromImgLabel;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class AtlasToImagePlus2D implements AtlasToImg2D<ImagePlus> {

    BiopAtlas ba;
    Object atlasLocation;

    Context ctx;

    ArrayList<Registration<ImagePlus>> registrationSequence = new ArrayList<>();

    ArrayList<ImagePlus> registeredImageSequence = new ArrayList<>();

    ImagePlus imageUsedForRegistration;

    ImagePlus imgAtlas;

    ConvertibleRois untransformedRois;

    ConvertibleRois transformedRois;

    @Override
    public void setScijavaContext(Context ctx) {
        this.ctx = ctx;
    }

    @Override
    public void setAtlas(BiopAtlas ba) {
        this.ba=ba;
    }

    @Override
    public void setAtlasLocation(Object location) {

        if (this.atlasLocation==null) {
            this.atlasLocation = this.ba.map.getCurrentLocation();
        } else {
            this.ba.map.setCurrentLocation(this.atlasLocation);
        }

        imgAtlas = this.ba.map.getCurrentStructuralImage().duplicate();
        imgAtlas.setCalibration(new Calibration());
        atlasLocation=location;

        ImagePlus imgLabel = this.ba.map.getCurrentLabelImage();

        CommandService cs = ctx.getService(CommandService.class);
        try {
            untransformedRois = (ConvertibleRois) cs.run(ConstructROIsFromImgLabel.class, true, "atlas", this.ba, "labelImg", imgLabel, "smoothen", false).get().getOutput("cr_out");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BiopAtlas getAtlas() {
        return ba;
    }

    @Override
    public Object getAtlasLocation() {
        return atlasLocation;
    }

    @Override
    public void addRegistration(Registration<ImagePlus> reg) {
       this.addRegistration(reg, imagePlus -> imagePlus, imagePlus -> imagePlus);
    }

    @Override
    public void addRegistration(Registration<ImagePlus> reg, Function<ImagePlus, ImagePlus> preprocessFixedImage, Function<ImagePlus, ImagePlus> preprocessMovingImage) {
        ImagePlus f = preprocessFixedImage.apply(imgAtlas);
        imgAtlas.hide();

        reg.setFixedImage(f);
        ImagePlus imgIn;
        if (registrationSequence.size()==0) {
            imgIn = this.imageUsedForRegistration;
        } else {
            imgIn = this.registeredImageSequence.get(registeredImageSequence.size()-1);
        }
        ImagePlus m = preprocessMovingImage.apply(imgIn);
        reg.setMovingImage(m);
        reg.register();
        m.hide();
        f.hide();

        this.registrationSequence.add(reg);
        ImagePlus trImg = reg.getImageRegistration().apply(imgIn);
        trImg.setCalibration(new Calibration());
        this.registeredImageSequence.add(trImg);
        this.registeredImageSequence.get(registeredImageSequence.size()-1).show();
        if (this.registrationSequence.size()>1) {
            imgIn.hide();
        }
        m.changes = false;
        m.close();

        f.changes = false;
        f.close();

        this.computeTransformedRois();
    }

    @Override
    public void showLastImage() {
        if (registeredImageSequence.size()==0) {
            this.imageUsedForRegistration.show();
        } else {
            this.registeredImageSequence.get(registeredImageSequence.size() - 1).show();
        }
    }

    @Override
    public void rmLastRegistration() {
        assert registeredImageSequence.size()>1;
        assert registeredImageSequence.size()==registrationSequence.size();
        this.registeredImageSequence.get(registeredImageSequence.size()-1).changes=false;
        this.registeredImageSequence.get(registeredImageSequence.size()-1).close();
        registrationSequence.remove(registrationSequence.size()-1);
        registeredImageSequence.remove(registeredImageSequence.size()-1);

        this.computeTransformedRois();
    }

    @Override
    public void resetRegistrations() {
        registrationSequence = new ArrayList<>();
    }

    @Override
    public ArrayList<Registration<ImagePlus>> getRegistrations() {
        return this.registrationSequence;
    }

    @Override
    public void setImage(ImagePlus img) {
        imageUsedForRegistration = img;
        imageUsedForRegistration.setCalibration(new Calibration());
    }

    @Override
    public ImagePlus getImage() {
        return this.imageUsedForRegistration;
    }

    @Override
    public void putTransformedRoisToImageJROIManager() {
        transformedRois.to(RoiManager.class);
    }

    @Override
    public void save(String path) {
        System.err.println("Unsupported save operation");
    }

    @Override
    public void load(URL url) {
        System.err.println("Unsupported load operation from URL");
    }

    @Override
    public void load(File file) {
        System.err.println("Unsupported load operation from file");
    }

    public void computeTransformedRois() {
        transformedRois = new ConvertibleRois();
        IJShapeRoiArray arrayIni = (IJShapeRoiArray) this.untransformedRois.to(IJShapeRoiArray.class);
        transformedRois.set(arrayIni);
        RealPointList list = ((RealPointList) transformedRois.to(RealPointList.class));
        // Perform reverse transformation, in the reverse order:
        //  - From atlas coordinates -> image coordinates
        Collections.reverse(this.registrationSequence);
        for (Registration reg : this.registrationSequence) {
            list = reg.getPtsRegistration(list);
        }
        Collections.reverse(this.registrationSequence);
        transformedRois.clear();
        list.shapeRoiList = new IJShapeRoiArray(arrayIni);
        transformedRois.set(list);
    }

    @Override
    public void putTransformedRoisToObjectService() {
        //computeTransformedRois();
        ctx.getService(ObjectService.class).addObject(transformedRois);
    }
}
