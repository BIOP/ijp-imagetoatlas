package ch.epfl.biop.registration;

import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.wrappers.elastix.RegisterHelper;
import ch.epfl.biop.wrappers.elastix.ij2commands.Elastix_Register;
import ch.epfl.biop.wrappers.transformix.TransformHelper;
import ij.ImagePlus;
import net.imglib2.RealPoint;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

public class Elastix2DRegistration implements Registration<ImagePlus> {
    ImagePlus fimg, mimg;

    RegisterHelper rh;

    @Override
    public void setFixedImage(ImagePlus fimg) {
        this.fimg = fimg;
    }

    @Override
    public void setMovingImage(ImagePlus mimg) {
        this.mimg = mimg;
    }

    @Parameter
    public Context ctx;

    @Override
    public void register() {
        CommandService cs = ctx.getService(CommandService.class);

        Future<CommandModule> cmd = cs.run(Elastix_Register.class,true,
                "movingImage", mimg,
                "fixedImage", fimg);

        try {
            CommandModule cm = cmd.get();
            rh = (RegisterHelper) (cm.getOutput("rh"));

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Function<ImagePlus, ImagePlus> getImageRegistration() {
        return (img) -> {
            TransformHelper th = new TransformHelper();
            th.setTransformFile(rh);
            th.setImage(img);
            th.transform();
            ImagePlus img_out = (ImagePlus) (th.getTransformedImage().to(ImagePlus.class));
            return img_out;
        };
    }

    /*@Override
    public Function<RealPointList, RealPointList> getPtsRegistration() {
        return (list) -> {
            ConvertibleRois cr = new ConvertibleRois();
            cr.set(list);
            TransformHelper th = new TransformHelper();
            th.setTransformFile(rh);
            th.setRois(cr);
            th.transform();
            ConvertibleRois cr_out = th.getTransformedRois();;
            return ((RealPointList) cr_out.to(RealPointList.class));
        };
    }*/

    @Override
    public RealPointList getPtsRegistration(RealPointList pts) {
        return null;
    }

}
