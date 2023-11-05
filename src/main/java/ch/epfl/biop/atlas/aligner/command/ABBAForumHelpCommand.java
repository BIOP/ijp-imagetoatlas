package ch.epfl.biop.atlas.aligner.command;

import bdv.BigDataViewer;
import bdv.util.BdvHandle;
import bigwarp.BigWarp;
import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.bdv.img.bioformats.BioFormatsHelper;
import ch.epfl.biop.sourceandconverter.register.Elastix2DSplineRegister;
import ch.epfl.biop.wrappers.elastix.ElastixTask;
import ij.IJ;
import net.imagej.ImageJ;
import net.imagej.updater.UpdateService;
import net.imagej.updater.UpdateSite;
import net.imagej.updater.util.AvailableSites;
import org.scijava.command.Command;
import org.scijava.platform.PlatformService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.util.VersionUtils;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.net.URL;
import java.util.Map;
@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>ABBA - Ask for help in the forum",
        description = "Open a new post in the image.sc forum with current install information")
public class ABBAForumHelpCommand implements Command {

    @Parameter
    PlatformService ps;

    @Parameter
    UpdateService us;

    @Override
    public void run() {
        try {
            Map<String, UpdateSite> sites = AvailableSites.getAvailableSites();

            StringBuilder sites_string = new StringBuilder();

            sites.values().stream()
                    .filter(site -> us.getUpdateSite(site.getName()).isActive())
                    .forEach(site -> sites_string.append(site.getName()+"+"));

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
            body +="Biop Image Loader "+VersionUtils.getVersion(BioFormatsHelper.class)+nl;
            body +="Biop Wrappers "+VersionUtils.getVersion(ElastixTask.class)+nl;
            //noinspection deprecation
            body +="Updates sites: "+sites_string+nl;
            body +="```";

            String fullUrl = imageScForumUrl+"new-topic?"
                    +"title="+title+"&"
                    +"body="+body+"&"
                    +"category=usage-issues&"
                    +"tags=fiji,abba";

            IJ.log(fullUrl); // Mac OS : if the URL is too long, the next line returns an error

            ps.open(new URL(fullUrl));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
