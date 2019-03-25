package ch.epfl.biop.registration;

import ch.epfl.biop.java.utilities.roi.ConvertibleRois;
import ch.epfl.biop.java.utilities.roi.types.RealPointList;
import ch.epfl.biop.java.utilities.roi.types.TransformixOutputRoisFile;
import ch.epfl.biop.wrappers.elastix.RegisterHelper;
import ch.epfl.biop.wrappers.elastix.ij2commands.Elastix_Register;
import ch.epfl.biop.wrappers.transformix.TransformHelper;
import ch.epfl.biop.wrappers.transformix.ij2commands.Transformix_TransformImgPlus;
import ij.ImagePlus;
import org.scijava.Context;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
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
            CommandService cs = ctx.getService(CommandService.class);
            Future<CommandModule> cmd = cs.run(Transformix_TransformImgPlus.class,true,
                    "img_in", img, "rh", rh);
            ImagePlus img_out = null;
            try {
                CommandModule cm = cmd.get();
                img_out = (ImagePlus) (cm.getOutput("img_out"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return img_out;
        };
    }

    @Override
    public RealPointList getPtsRegistration(RealPointList pts) {
        ConvertibleRois cr = new ConvertibleRois();
        cr.set(pts);
        TransformHelper th = new TransformHelper();
        th.setTransformFile(rh);
        th.setRois(cr);
        th.transform();
        ConvertibleRois cr_out = th.getTransformedRois();;
        return ConvertibleRois.transformixFileToRealPointList((TransformixOutputRoisFile) cr_out.to(TransformixOutputRoisFile.class));
    }

}
