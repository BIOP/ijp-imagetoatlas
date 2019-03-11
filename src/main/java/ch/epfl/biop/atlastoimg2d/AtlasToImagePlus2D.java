package ch.epfl.biop.atlastoimg2d;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.registration.Registration;
import ij.ImagePlus;
import ij.measure.Calibration;
import org.scijava.Context;

import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.function.Function;

public class AtlasToImagePlus2D implements AtlasToImg2D<ImagePlus> {


    BiopAtlas ba;
    Object atlasLocation;


    boolean interactive=true;

    Context ctx;

    ArrayList<Registration<ImagePlus>> registrationSequence = new ArrayList<>();

    ArrayList<ImagePlus> registeredImageSequence = new ArrayList<>();

    ImagePlus imageUsedForRegistration;



    @Override
    public void setScijavaContext(Context ctx) {
        this.ctx = ctx;
    }

    public void setInteractive(boolean flag) {
        interactive = flag;
    }

    @Override
    public void setAtlas(BiopAtlas ba) {
        this.ba=ba;
    }

    @Override
    public boolean isInitialized() {
        return (ba!=null);
    }

    ImagePlus imgAtlas;

    @Override
    public void setAtlasLocation(Object location) {

        if (this.atlasLocation==null) {
            this.atlasLocation = this.ba.map.getCurrentLocation();
        } else {
            this.ba.map.setCurrentLocation(this.atlasLocation);
        }

        imgAtlas = this.ba.map.getCurrentStructuralImage();
        imgAtlas.setCalibration(new Calibration());
        atlasLocation=location;
    }

    boolean registrationSet = false;

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
        reg.setFixedImage(imgAtlas);
        ImagePlus imgIn;
        if (registrationSequence.size()==0) {
            imgIn = this.imageUsedForRegistration;
        } else {
            imgIn = this.registeredImageSequence.get(registeredImageSequence.size()-1);
        }
        reg.setMovingImage(imgIn);
        reg.register();
        this.registrationSequence.add(reg);
        ImagePlus trImg = reg.getImageRegistration().apply(imgIn);
        trImg.setCalibration(new Calibration());
        this.registeredImageSequence.add(trImg);
        this.registeredImageSequence.get(registeredImageSequence.size()-1).show();
    }

    @Override
    public void rmLastRegistration() {
        assert registeredImageSequence.size()>0;
        assert registeredImageSequence.size()==registrationSequence.size();
        registrationSequence.remove(registrationSequence.size()-1);
        registeredImageSequence.remove(registeredImageSequence.size()-1);
    }

    @Override
    public void resetRegistrations() {
        registrationSet = false;
        registrationSequence = new ArrayList<>();
    }

    @Override
    public boolean isRegistrationSet() {
        return registrationSet;
    }

    @Override
    public ArrayList<Registration<ImagePlus>> getRegistrations() {
        return null;
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
    public void register() {

    }

    @Override
    public void putRoisToImageJ(ConvertibleRois cr) {

    }

    @Override
    public void putRoisToQuPath(ConvertibleRois cr) {

    }

    @Override
    public File save(String path) {
        return null;
    }

    @Override
    public void load(URL url) {

    }

    @Override
    public void load(File file) {

    }
}
