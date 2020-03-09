package ch.epfl.biop.atlastoimg2d.commands.imageplus;

import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import ch.epfl.biop.fiji.imageplusutils.ImagePlusFunctions;
import ch.epfl.biop.registration.*;
import ch.epfl.biop.registration.imageplus.BigWarp2DGridRegistration;
import ch.epfl.biop.registration.imageplus.BigWarp2DRegistration;
import ch.epfl.biop.registration.imageplus.CropAndScaleRegistration;
import ch.epfl.biop.registration.imageplus.Elastix2DRegistration;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;
import ij.process.LUT;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.function.Function;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Image To Atlas>Add Registration")
public class ImageToAtlasRegister implements Command {

    @Parameter(type = ItemIO.BOTH)
    AtlasToImg2D aligner;

    @Parameter(choices={"Crop and Scale","Elastix","BigWarp", "BigWarp Grid"})
    String registrationType;

    @Parameter
    Context ctx;

    @Parameter(label="Select channels for the registration (Atlas image) ?")
    boolean filterChannelFixedImage=false;

    @Parameter(required = false, label="Comma separated list for atlas image channels ('1,3')")
    String channels_fixed_image;

    @Parameter(label="Select channels for the registration (your image) ?")
    boolean filterChannelMovingImage;

    @Parameter(required = false, label="Comma separated list for your image channels ('1,3')")
    String channels_moving_image;

    @Override
    public void run() {
        Registration newReg;

        Function<ImagePlus, ImagePlus> preprocessFixed = im -> im;

        Function<ImagePlus, ImagePlus> preprocessMoving = im -> im;

        if ((filterChannelFixedImage)&&(channels_fixed_image!=null)) {
            String[] chs_str = channels_fixed_image.split(",");
            ArrayList<Integer> chs_int = new ArrayList<>();
            for (String st:chs_str) {
                chs_int.add(Integer.valueOf(st.trim()));
            }
            if (chs_int.size()!=0) {
                preprocessFixed = (im) -> filterChannels(im, chs_int);
            }
        }

        if ((filterChannelMovingImage)&&(channels_moving_image!=null)) {
            String[] chs_str = channels_moving_image.split(",");
            ArrayList<Integer> chs_int = new ArrayList<>();
            for (String st:chs_str) {
                chs_int.add(Integer.valueOf(st.trim()));
            }
            if (chs_int.size()!=0) {
                preprocessMoving = (im) -> filterChannels(im, chs_int);
            }
        }

        switch (registrationType) {
            case "Elastix":
                newReg = new Elastix2DRegistration();
                ((Elastix2DRegistration) newReg).ctx=ctx; // Needed because it's using a CommandService launching a Command
                aligner.addRegistration( newReg, preprocessFixed, preprocessMoving );
                break;
            case "BigWarp":
                aligner.addRegistration( new BigWarp2DRegistration(), preprocessFixed, preprocessMoving);
                break;
            case "BigWarp Grid":
                aligner.addRegistration( new BigWarp2DGridRegistration(), preprocessFixed, preprocessMoving);
                break;
            case "Crop and Scale":
                aligner.addRegistration( new CropAndScaleRegistration(), preprocessFixed, preprocessMoving);
                break;
        }
    }

    public static ImagePlus filterChannels(ImagePlus img_in, ArrayList<Integer> channels_index) {
        ImagePlus[] channels_imp = ChannelSplitter.split(new Duplicator().run(img_in));
        /*ImagePlus[] kept_channels = new ImagePlus[channels_index.size()];

        for (int i =0;i<channels_index.size();i++) {
            kept_channels[i] = channels_imp[channels_index.get(i)-1];
        }*/

        ImagePlus img_out = new ImagePlusFunctions.ImagePlusBuilder()
                .allAs(img_in).nChannels(channels_index.size())
               /* .with(
                        b -> {
                            b.luts = new LUT[channels_index.size()];
                            for (int i=0;i<channels_index.size();i++) {
                                b.luts[i] = img_in.getLuts()[channels_index.get(i)-1];//, img_out, i+1);
                            }
                        })*/
                .createImagePlus();

        for (int i=0;i<channels_index.size();i++) {
            copyChannel(img_in, channels_index.get(i), img_out, i+1);
        }

        return img_out.duplicate();

    }

    public static void copyChannel(ImagePlus img_src, int ch_src, ImagePlus img_dst, int ch_dst) {
        img_dst.setC(ch_dst);
        img_src.setC(ch_src);
        img_dst.getProcessor().setLut((LUT) img_src.getLuts()[ch_src-1]);
        if (img_dst instanceof CompositeImage) {
            ((CompositeImage) img_dst).setChannelLut(img_src.getLuts()[ch_src-1]);
        }
        for (int f=0;f<img_dst.getNFrames();f++) {
            for (int s=0;s<img_dst.getNSlices();s++) {
                img_dst.setT(f);
                img_dst.setSlice(s);
                img_src.setT(f);
                img_src.setSlice(s);
                img_dst.setProcessor((ImageProcessor)img_src.getProcessor().clone());
            }
        }
    }
}
