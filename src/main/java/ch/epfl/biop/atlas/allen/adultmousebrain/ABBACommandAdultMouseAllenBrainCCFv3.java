package ch.epfl.biop.atlas.allen.adultmousebrain;

import ch.epfl.biop.ABBAHelper;
import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlas.aligner.commands.SacMultiSacsPositionerCommand;
import ch.epfl.biop.atlas.aligner.commands.SlicerAdjusterInteractiveCommand;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.allen.adultmousebrain.AllenBrainAdultMouseAtlasCCF2017Command;
import ch.epfl.biop.wrappers.BiopWrappersCheck;
import ch.epfl.biop.wrappers.elastix.Elastix;
import ch.epfl.biop.wrappers.transformix.Transformix;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * Allen Brain BIOP Aligner entry command
 */


@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>ABBA (Allen Adult Mouse Brain)", initializer = "showlogo")
public class ABBACommandAdultMouseAllenBrainCCFv3 implements Command {

    @Parameter
    CommandService cs;

    @Parameter(label = "URL path to allen brain map data, leave empty for automated downloading and caching (3Go)", persist = false)
    String mapUrl = ABBAHelper.getMapUrl(); //Prefs.get(keyPrefix+"mapUrl","");

    //static String defaultOntologyUrl = ;//"http://ec2-18-222-96-84.us-east-2.compute.amazonaws.com/1.json";//file:/home/nico/Dropbox/BIOP/ABA/BrainServerTest/1.json";
    @Parameter(label = "URL path to allen brain ontology data, leave empty for automated downloading and caching", persist = false)
    String ontologyUrl = ABBAHelper.getOntologyUrl(); //Prefs.get(keyPrefix+"ontologyUrl","");

    @Parameter(label = "Select the executable file 'elastix.exe' or 'elastix.sh'", required = false)
    File elastixExeFile = ABBAHelper.getElastixExeFile();

    @Parameter(label = "Select the executable file 'transformix.exe' or 'transformix.sh'",required = false)
    File transformixExeFile = ABBAHelper.getTransformixExeFile();

    @Parameter(type = ItemIO.OUTPUT)
    MultiSlicePositioner mp;

    @Parameter(label = "Store these settings for all users", persist=false)
    boolean storeAsGlobalSettings = false;

    @Parameter(label = "No Graphical User Interface")
    boolean nogui = false;

    @Override
    public void run() {
        try {
            BiopAtlas ba = (BiopAtlas) cs.run(AllenBrainAdultMouseAtlasCCF2017Command.class, true,
                    "mapUrl", mapUrl,//xmlDatasetFile.toURI().toURL().toString(),
                            "ontologyUrl", ontologyUrl //jsonFile.toURI().toURL().toString()
                    ).get().getOutput("ba");

            if (elastixExeFile!=null && elastixExeFile.exists()) {
                Elastix.setExePath(elastixExeFile);
                BiopWrappersCheck.isElastixSet();
            }

            if (transformixExeFile!=null && transformixExeFile.exists()) {
                Transformix.setExePath(transformixExeFile);
                BiopWrappersCheck.isTransformixSet();
            }

            CommandModule cm = cs.run(SacMultiSacsPositionerCommand.class, true,
                    "ba", ba,
                    "nogui", nogui).get();

            mp = (MultiSlicePositioner) (cm.getOutput("mp"));

            cs.run(SlicerAdjusterInteractiveCommand.class, true,
                    //"zSamplingSteps", 200,
                    "rotateX",0,
                    "rotateY",0,
                    "reslicedAtlas", mp.getReslicedAtlas(),
                    "lockAngles", false);

            if (storeAsGlobalSettings) {
                ABBAHelper.ABBASettings settings = new ABBAHelper.ABBASettings();
                settings.pathToABBAAtlas = mapUrl;
                settings.pathToABBAOntology = ontologyUrl;
                settings.pathToElastixExeFile = elastixExeFile.getAbsolutePath();
                settings.pathToTransformixExeFile = transformixExeFile.getAbsolutePath();
                ABBAHelper.setToLocalFiji(settings);
            }
        } /*catch (MalformedURLException e) {
            e.printStackTrace();
        }*/ catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    public void showlogo() {
        ABBAHelper.displayABBALogo(2000);
    } // looks so serious with this.
}
