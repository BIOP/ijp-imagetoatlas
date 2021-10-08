package ch.epfl.biop.abba.plugin.py;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.commands.SacMultiSacsPositionerCommand;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesChannelsSelect;
import ch.epfl.biop.atlas.aligner.plugin.SimpleRegistrationWrapper;
import ch.epfl.biop.bdv.command.importer.SourceFromImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.scijava.Context;
import org.scijava.command.PyCommandBuilder;

import java.util.HashMap;
import java.util.Map;

public class TestExternalRegistrationPlugin {

    public static void main (String... args) throws Error, Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        String identityRegName = "ext.IdentityRegistration";

        // ----------------- DEMO PROGRAMMATICALLY ADDING REGISTRATION PLUGIN (Statically added)
        // Fully flexible registration plugin
        MultiSlicePositioner
                .registerRegistrationPlugin(identityRegName, () -> new ExternalIdentityRegistrationPlugin());

        addDefaultUI("External - Identity Registration", identityRegName, ij.context());

        String rotationRegName = "ext.RotationRegistration";

        // Simple registration plugin
        MultiSlicePositioner
                .registerRegistrationPlugin(rotationRegName, () -> new SimpleRegistrationWrapper(rotationRegName, new ExternalSimpleRotationRegistrationPlugin()));

        addDefaultUI("External - Rotation Registration", rotationRegName, ij.context());


        // --------------- Starting ABBA
        MultiSlicePositioner mp = (MultiSlicePositioner) ij.command().run(SacMultiSacsPositionerCommand.class, true).get().getOutput("mp");

        ImagePlus demoSlice = IJ.openImage("src/test/resources/demoSlice.tif");
        demoSlice.show();

        ij.command().run(SourceFromImagePlusCommand.class, true, "imagePlus", demoSlice).get();

        SourceAndConverter[] sac = ij.convert().convert(demoSlice.getTitle(), SourceAndConverter[].class);

        mp.createSlice(sac,4.5);

        mp.waitForTasks();

        SliceSources slice = mp.getSortedSlices().get(0);

        mp.centerBdvViewOn(slice);
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
                    ((MultiSlicePositioner) inputs.get("mp")).register(registrationTypeName,
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
