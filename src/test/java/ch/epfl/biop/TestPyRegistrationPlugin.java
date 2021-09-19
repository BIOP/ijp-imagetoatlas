package ch.epfl.biop;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.ABBACommand;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesChannelsSelect;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessorHelper;
import ch.epfl.biop.atlas.plugin.PyIdentityRegistrationPlugin;
import ch.epfl.biop.bdv.command.importer.SourceFromImagePlusCommand;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImageJ;
import org.scijava.command.PyCommandBuilder;

import java.util.HashMap;
import java.util.Map;

public class TestPyRegistrationPlugin {

    public static void main (String... args) throws Error, Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        MultiSlicePositioner
                .registerRegistrationPlugin(PyIdentityRegistrationPlugin.typeName, () -> new PyIdentityRegistrationPlugin());

        new PyCommandBuilder().name("pyreg")
                .input("fixed_channel", Integer.class)
                .input("moving_channel", Integer.class)
                .input("mp", MultiSlicePositioner.class)
                .menuPath("Plugins>BIOP>Atlas>Multi Image To Atlas>Align>pyreg")
                .function((inputs) -> {
                    System.out.println("Inside run");
                    Map<String, Object> params = new HashMap<>();
                    ((MultiSlicePositioner) inputs.get("mp")).register(PyIdentityRegistrationPlugin.typeName,
                            new SourcesChannelsSelect((Integer) inputs.get("fixed_channel")),
                            new SourcesChannelsSelect((Integer) inputs.get("moving_channel")),
                            params);

                    Map<String, Object> out = new HashMap<>();
                    return out;
                }).create(ij.context());

        MultiSlicePositioner
                .registerRegistrationPluginUI(PyIdentityRegistrationPlugin.typeName, "pyreg");

        MultiSlicePositioner mp = (MultiSlicePositioner) ij.command().run(ABBACommand.class, true).get().getOutput("mp");

        ImagePlus demoSlice = IJ.openImage("src/test/resources/demoSlice.tif");
        demoSlice.show();

        ij.command().run(SourceFromImagePlusCommand.class, true, "imagePlus", demoSlice).get();

        SourceAndConverter[] sac = ij.convert().convert(demoSlice.getTitle(), SourceAndConverter[].class);

        mp.createSlice(sac,4.5);

        mp.waitForTasks();

        SliceSources slice = mp.getSortedSlices().get(0);

        mp.centerBdvViewOn(slice);
        mp.selectSlice(slice);

        mp.register(PyIdentityRegistrationPlugin.typeName,
                SourcesProcessorHelper.Identity(),
                SourcesProcessorHelper.Identity(),
                new HashMap<>());

    }
}