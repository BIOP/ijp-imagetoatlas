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

    ConvertibleRois untransformedRois;

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

        //ConvertibleRois cr = new ConvertibleRois();
        ImagePlus imgLabel = this.ba.map.getCurrentLabelImage();
        //cr.set(imgLabel);

        CommandService cs = ctx.getService(CommandService.class);
        try {
            untransformedRois = (ConvertibleRois) cs.run(ConstructROIsFromImgLabel.class, true, "atlas", this.ba, "labelImg", imgLabel, "smoothen", false).get().getOutput("cr_out");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        //
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
        if (this.registrationSequence.size()>1) {
            imgIn.hide();
        }
        imgAtlas.hide();
    }

    @Override
    public void showLastImage() {
        this.registeredImageSequence.get(registeredImageSequence.size()-1).show();
    }

    @Override
    public void rmLastRegistration() {
        assert registeredImageSequence.size()>0;
        assert registeredImageSequence.size()==registrationSequence.size();
        this.registeredImageSequence.get(registeredImageSequence.size()-1).changes=false;
        this.registeredImageSequence.get(registeredImageSequence.size()-1).close();
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

    @Override
    public void putTransformedRoisToObjectService() {
        System.out.println("--------------------------------- ");
        ConvertibleRois cr = new ConvertibleRois();
        IJShapeRoiArray arrayIni = (IJShapeRoiArray) this.untransformedRois.to(IJShapeRoiArray.class);
        cr.set(arrayIni);
        RealPointList list = ((RealPointList) cr.to(RealPointList.class));
        System.out.println("list.size="+list.ptList.size());
        System.out.println("list.get(0).getDoublePosition(0) BEFORE="+list.ptList.get(0).getDoublePosition(0));
        Collections.reverse(this.registrationSequence);
        for (Registration reg : this.registrationSequence) {
            System.out.println("apply transform");
            list = reg.getPtsRegistration(list);
           // RealPointList list_out = (reg.getPtsRegistration()).apply(list);//.getPtsRegistration().apply(list);
        }
        System.out.println("list.get(0).getDoublePosition(0) AFTER="+list.ptList.get(0).getDoublePosition(0));
        Collections.reverse(this.registrationSequence);
        cr.clear();
        list.shapeRoiList = new IJShapeRoiArray(arrayIni);
        cr.set(list);
        cr.to(RoiManager.class);
        ctx.getService(ObjectService.class).addObject(cr);
    }
}
