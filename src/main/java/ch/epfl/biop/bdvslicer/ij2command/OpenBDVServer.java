package ch.epfl.biop.bdvslicer.ij2command;

import bdv.BigDataViewer;
import bdv.ij.util.ProgressWriterIJ;
import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import bdv.viewer.ViewerOptions;

import java.io.IOException;
import java.util.Map;

/**
 * This example illustrates how to create an ImageJ 2 {@link Command} plugin.
 * The pom file of this project is customized for the PTBIOP Organization (biop.epfl.ch)
 * <p>
 * The code here is a simple Gaussian blur using ImageJ Ops.
 * </p>
 */
@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Open Big Dataset from a BigDataServer")
public class OpenBDVServer implements Command {

    @Parameter
    String urlServer;

    @Parameter
    String datasetName;

    @Parameter
    LogService ls;

    @Parameter
    ObjectService os;

    @Override
    public void run() {
        ls.info(this.getClass().toString() + " -");
        ls.info("\t - Server: "+ urlServer);
        ls.info("\t - Dataset" + datasetName);
        try {
            Map<String,String> BDSList = BDVServerUtils.getDatasetList(urlServer);
            final String filename = BDSList.get(datasetName);
            //System.out.println(filename);
            BigDataViewer bdv = BigDataViewer.open( filename, urlServer+" - "+datasetName, new ProgressWriterIJ(), ViewerOptions.options() );
            os.addObject(bdv);
        } catch (SpimDataException e) {
            e.printStackTrace();
            ls.error("Could not create Spim data");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(OpenBDVServer.class, true,
                "urlServer","http://fly.mpi-cbg.de:8081",
                "datasetName", "Drosophila");
    }

}
