package ch.epfl.biop.atlas.allen.adultmousebrain;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlastoimg2d.multislice.commands.SacMultiSacsPositionerCommand;
import ch.epfl.biop.atlastoimg2d.multislice.commands.SlicerAdjusterInteractiveCommand;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import ch.epfl.biop.wrappers.BiopWrappersCheck;
import ch.epfl.biop.wrappers.elastix.Elastix;
import ch.epfl.biop.wrappers.transformix.Transformix;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

/**
 * Allen Brain BIOP Aligner entry command
 */


@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>ABBA")
public class ABBA implements Command {

    @Parameter
    CommandService cs;

    @Parameter
    File jsonFile;

    @Parameter
    File xmlDatasetFile;

    @Parameter
    File elastixExeFile;

    @Parameter
    File transformixExeFile;

    @Override
    public void run() {
        try {
            BiopAtlas ba = (BiopAtlas) cs.run(AllenBrainAdultMouseAtlasCCF2017.class, true,
                    "mapUrl", xmlDatasetFile.toURI().toURL().toString(),
                            "ontologyUrl", jsonFile.toURI().toURL().toString()
                    ).get().getOutput("ba");

            Transformix.setExePath(transformixExeFile);
            Elastix.setExePath(elastixExeFile);
            BiopWrappersCheck.isElastixSet();
            BiopWrappersCheck.isTransformixSet();

            CommandModule cm = cs.run(SacMultiSacsPositionerCommand.class, true, "ba", ba).get();

            MultiSlicePositioner mp = (MultiSlicePositioner) (cm.getOutput("mp"));

            cs.run(SlicerAdjusterInteractiveCommand.class, true,
                    "zSamplingSteps", 200,
                    "rotateX",0,
                    "rotateY",0,
                    "reslicedAtlas", mp.getReslicedAtlas());



        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
}
