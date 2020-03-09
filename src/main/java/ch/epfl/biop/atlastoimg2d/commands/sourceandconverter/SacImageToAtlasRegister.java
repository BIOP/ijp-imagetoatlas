package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlastoimg2d.AtlasToSourceAndConverter2D;
import ch.epfl.biop.registration.*;
import ch.epfl.biop.registration.imageplus.BigWarp2DGridRegistration;
import ch.epfl.biop.registration.imageplus.BigWarp2DRegistration;
import ch.epfl.biop.registration.imageplus.CropAndScaleRegistration;
import ch.epfl.biop.registration.imageplus.Elastix2DRegistration;
import ch.epfl.biop.registration.sourceandconverter.SacBigWarp2DRegistration;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.function.Function;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Image To Atlas (BDV)>Add Registration")
public class SacImageToAtlasRegister implements Command {

    @Parameter(type = ItemIO.BOTH)
    AtlasToSourceAndConverter2D aligner;

    @Parameter(choices={"BigWarp"})
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

        Function<SourceAndConverter[], SourceAndConverter[]> preprocessFixed = im -> im;

        Function<SourceAndConverter[], SourceAndConverter[]> preprocessMoving = im -> im;

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
                aligner.addRegistration( new SacBigWarp2DRegistration(), preprocessFixed, preprocessMoving);
                break;
           /* case "BigWarp Grid":
                aligner.addRegistration( new BigWarp2DGridRegistration(), preprocessFixed, preprocessMoving);
                break;
            case "Crop and Scale":
                aligner.addRegistration( new CropAndScaleRegistration(), preprocessFixed, preprocessMoving);
                break;*/
        }
    }

    public static SourceAndConverter[] filterChannels(SourceAndConverter[] img_in, ArrayList<Integer> channels_index) {
        SourceAndConverter[] out = new SourceAndConverter[channels_index.size()];
        for (int i=0;i<channels_index.size();i++) {
            out[i] = img_in[channels_index.get(i)];
        }
        return out;
    }

}
