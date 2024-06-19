package ch.epfl.biop.abba.pluginexample.py;

import bdv.util.BdvHandle;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.registration.plugin.SimpleRegistrationWrapper;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.scijava.command.spimdata.SourceFromImagePlusCommand;
import ch.epfl.biop.sourceandconverter.processor.SourcesChannelsSelect;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.scijava.Context;
import org.scijava.command.PyCommandBuilder;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.util.HashMap;
import java.util.Map;

public class TestExternalRegistrationPlugin {

    public static void main (String... args) throws Error, Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        Atlas mouseAtlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");

        String identityRegName = "ext.IdentityRegistration";

        // ----------------- DEMO PROGRAMMATICALLY ADDING REGISTRATION PLUGIN (Statically added)
        // Fully flexible registration plugin
        MultiSlicePositioner
                .registerRegistrationPlugin(identityRegName, ExternalIdentityRegistrationPlugin::new);

        addDefaultUI("External - Identity Registration", identityRegName, ij.context());

        String rotationRegName = "ext.RotationRegistration";

        // Simple registration plugin
        MultiSlicePositioner
                .registerRegistrationPlugin(rotationRegName, () -> new SimpleRegistrationWrapper(rotationRegName, new ExternalSimpleRotationRegistrationPlugin()));

        addDefaultUI("External - Rotation Registration", rotationRegName, ij.context());


        // --------------- Starting ABBA
        MultiSlicePositioner mp = (MultiSlicePositioner) ij.command().run(ABBAStartCommand.class, true).get().getOutput("mp");

        BdvHandle bdvh = SourceAndConverterServices.getBdvDisplayService().getNewBdv();

        BdvMultislicePositionerView view = new BdvMultislicePositionerView(mp, bdvh);

        ImagePlus demoSlice = IJ.openImage("src/test/resources/demoSlice.tif");
        demoSlice.show();

        ij.command().run(SourceFromImagePlusCommand.class, true, "imagePlus", demoSlice).get();

        SourceAndConverter[] sac = ij.convert().convert(demoSlice.getTitle(), SourceAndConverter[].class);

        mp.createSlice(sac,4.5);

        mp.waitForTasks();

        SliceSources slice = mp.getSlices().get(0);

        //mp.centerBdvViewOn(slice);
        mp.selectSlice(slice);

        /*mp.register(rotationRegName,
                SourcesProcessorHelper.Identity(),
                SourcesProcessorHelper.Identity(),
                new HashMap<>());*/

    }

    public static void addDefaultUI(String commandName, String registrationTypeName, Context ctx) {
        new PyCommandBuilder().name(commandName)
                .input("fixed_channel", Integer.class)
                .input("moving_channel", Integer.class)
                .input("mp", MultiSlicePositioner.class)
                .menuPath("Plugins>BIOP>Atlas>Multi Image To Atlas>Align>"+commandName)
                .function((inputs) -> {
                    System.out.println("Inside run");
                    Map<String, Object> params = new HashMap<>();
                    ((MultiSlicePositioner) inputs.get("mp")).registerSelectedSlices(registrationTypeName,
                            new SourcesChannelsSelect((Integer) inputs.get("fixed_channel")),
                            new SourcesChannelsSelect((Integer) inputs.get("moving_channel")),
                            params);
                    Map<String, Object> out = new HashMap<>();
                    return out;
                }).create(ctx);

        MultiSlicePositioner
                .registerRegistrationPluginUI(registrationTypeName, commandName);
    }

}
