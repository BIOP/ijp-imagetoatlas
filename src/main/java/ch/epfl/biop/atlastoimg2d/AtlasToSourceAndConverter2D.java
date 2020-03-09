package ch.epfl.biop.atlastoimg2d;

import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.commands.ConstructROIsFromImgLabel;
import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.types.IJShapeRoiArray;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.registration.Registration;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.util.Util;
import org.scijava.Context;
import org.scijava.command.CommandService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterUtils;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceResampler;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AtlasToSourceAndConverter2D implements AtlasToImg2D<SourceAndConverter[]> {

    BiopAtlas ba;
    Object atlasLocation;

    Context ctx;

    ArrayList<Registration<SourceAndConverter[]>> registrationSequence = new ArrayList<>();

    ArrayList<SourceAndConverter[]> registeredImageSequence = new ArrayList<>();

    SourceAndConverter[] imageUsedForRegistration;

    SourceAndConverter[] imgAtlas;

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

        //TODO imgAtlas = this.ba.map.getCurrentStructuralImageAsImagePlus().duplicate();
        //TODO imgAtlas.setCalibration(new Calibration());

        imgAtlas = this.ba.map.getCurrentStructuralImageAsSacs();
        atlasLocation=location;

        ImagePlus imgLabel = this.ba.map.getCurrentLabelImageAsImagePlus();

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
    public void addRegistration(Registration<SourceAndConverter[]> reg) {
       this.addRegistration(reg, image -> image, image -> image);
    }

    @Override
    public void addRegistration(Registration<SourceAndConverter[]> reg, Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixedImage, Function<SourceAndConverter[], SourceAndConverter[]> preprocessMovingImage) {
        SourceAndConverter[] f = preprocessFixedImage.apply(imgAtlas);
        //imgAtlas.hide();

        reg.setFixedImage(f);
        SourceAndConverter[] imgIn;
        if (registrationSequence.size()==0) {
            imgIn = this.imageUsedForRegistration;
        } else {
            imgIn = this.registeredImageSequence.get(registeredImageSequence.size()-1);
        }
        SourceAndConverter[] m = preprocessMovingImage.apply(imgIn);
        reg.setMovingImage(m);
        reg.register();
        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .removeFromActiveBdv(m);

        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .removeFromActiveBdv(f);

        this.registrationSequence.add(reg);
        SourceAndConverter[] trImg = reg.getImageRegistration().apply(imgIn);
        //trImg.setCalibration(new Calibration());
        this.registeredImageSequence.add(trImg);

        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .show(this.registeredImageSequence.get(registeredImageSequence.size()-1));
        if (this.registrationSequence.size()>1) {
            SourceAndConverterServices.getSourceAndConverterDisplayService()
                    .removeFromActiveBdv(imgIn);
        }

        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .removeFromActiveBdv(m);


        SourceAndConverterServices.getSourceAndConverterDisplayService()
                .removeFromActiveBdv(f);


        this.computeTransformedRois();
    }

    @Override
    public void showLastImage() {
        if (registeredImageSequence.size()==0) {
            SourceAndConverterServices.getSourceAndConverterDisplayService().show(this.imageUsedForRegistration);
        } else {
            SourceAndConverterServices.getSourceAndConverterDisplayService().show(this.registeredImageSequence.get(registeredImageSequence.size() - 1));
        }
    }

    @Override
    public void rmLastRegistration() {
        assert registeredImageSequence.size()>1;
        assert registeredImageSequence.size()==registrationSequence.size();
        //TODO this.registeredImageSequence.get(registeredImageSequence.size()-1).changes=false;
        //TODO this.registeredImageSequence.get(registeredImageSequence.size()-1).close();
        registrationSequence.remove(registrationSequence.size()-1);
        registeredImageSequence.remove(registeredImageSequence.size()-1);

        this.computeTransformedRois();
    }

    @Override
    public void resetRegistrations() {
        registrationSequence = new ArrayList<>();
    }

    @Override
    public ArrayList<Registration<SourceAndConverter[]>> getRegistrations() {
        return this.registrationSequence;
    }

    @Override
    public ConvertibleRois getTransformedRois() {
        return transformedRois;
    }

    @Override
    public void setImage(SourceAndConverter[] img) {
        imageUsedForRegistration = img;
        //TODO imageUsedForRegistration.setCalibration(new Calibration());
    }

    @Override
    public SourceAndConverter[] getImage() {
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

    /*@Override
    public void putTransformedRoisToObjectService() {
        //computeTransformedRois();
        ctx.getService(ObjectService.class).addObject(transformedRois);
    }*/
}
