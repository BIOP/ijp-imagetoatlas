package ch.epfl.biop.atlas.aligner.commands;

import bdv.BigDataViewer;
import bdv.util.BdvHandle;
import bigwarp.BigWarp;
import ch.epfl.biop.ABBAHelper;
import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.sourceandconverter.register.Elastix2DSplineRegister;
import ch.epfl.biop.wrappers.elastix.ElastixTask;
import ch.epfl.biop.wrappers.elastix.RemoteElastixTask;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.VersionUtils;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.io.IOException;
import java.net.URL;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>ABBA - Ask for help in the forum")
public class ABBAForumHelpCommand implements Command {

    @Parameter
    PlatformService ps;

    @Override
    public void run() {
        try {
            String nl = "%0D%0A"; // new line in url get
            String imageScForumUrl = "https://forum.image.sc/";
            String title = "Help for ABBA in Fiji: [your question here]";
            String body = "[Detail your issue here]"+nl;
            body += nl;
            body += "---"+nl;
            body += nl;
            body += "OS and Dependencies Info"+nl;
            body +="```"+nl;
            body +="OS "+ System.getProperty("os.name")+nl;
            body +="ImageJ "+ VersionUtils.getVersion(ImageJ.class)+nl;
            body +="IJ "+ VersionUtils.getVersion(IJ.class)+nl;
            body +="ABBA "+ VersionUtils.getVersion(ABBAHelper.class)+nl;
            body +="BigWarp "+VersionUtils.getVersion(BigWarp.class)+nl;
            body +="Bdv "+VersionUtils.getVersion(BigDataViewer.class)+nl;
            body +="Bdv Vistools "+VersionUtils.getVersion(BdvHandle.class)+nl;
            body +="Bdv Biop Tools "+VersionUtils.getVersion(Elastix2DSplineRegister.class)+nl;
            body +="Bdv Playground "+VersionUtils.getVersion(SourceAndConverterServices.class)+nl;
            body +="Bdv BioFormats "+VersionUtils.getVersion(BioFormatsMetaDataHelper.class)+nl;
            body +="Biop Wrappers "+VersionUtils.getVersion(ElastixTask.class)+nl;
            body +="Registration Server "+VersionUtils.getVersion(RemoteElastixTask.class)+nl;
            body +="```";

            String fullUrl = imageScForumUrl+"new-topic?"
                    +"title="+title+"&"
                    +"body="+body+"&"
                    +"category=usage-issues&"
                    +"tags=fiji,abba";

            ps.open(new URL(fullUrl));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
