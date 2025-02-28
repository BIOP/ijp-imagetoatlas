package ch.epfl.biop.atlas.aligner.command;

import bdv.BigDataViewer;
import bdv.util.BdvHandle;
import bigwarp.BigWarp;
import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.bdv.img.bioformats.BioFormatsHelper;
import ch.epfl.biop.sourceandconverter.register.Elastix2DSplineRegister;
import ch.epfl.biop.wrappers.deepslice.DeepSlice;
import ch.epfl.biop.wrappers.elastix.Elastix;
import ch.epfl.biop.wrappers.elastix.ElastixTask;
import ch.epfl.biop.wrappers.transformix.Transformix;
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

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public static String pythonInformation = "";

    @Override
    public void run() {
        try {

            StringBuilder sites_string = new StringBuilder();
            if (pythonInformation.isEmpty()) {
                Map<String, UpdateSite> sites = AvailableSites.getAvailableSites();
                try {
                    for (UpdateSite site : sites.values()) {
                        if (us.getUpdateSite(site.getName()).isActive()) {
                            sites_string.append(site.getName()).append("+");
                        }
                    }
                } catch (Exception e) {
                    sites_string.append("Could not get installed update sites.");
                }
            } else {
                sites_string.append("Can't collect update sites with ABBA run from Python");
            }

            String nl = "\n";
            String imageScForumUrl = "https://forum.image.sc/";
            String title = "Help for ABBA in Fiji: [your question here]";
            String body = "[Detail your issue here]"+nl;
            body += nl;
            body += "---"+nl;
            body += nl;
            body += getConfigInfos(nl);

            //noinspection deprecation
            body +="Updates sites: "+sites_string+nl;


            String fullUrl = imageScForumUrl+"new-topic?"
                    +"title="+title+"&"
                    +"body="+formatStringForUrl(body)+"&"
                    +"category=usage-issues&"
                    +"tags=fiji,abba";

            IJ.log(fullUrl); // Mac OS : if the URL is too long, the next line returns an error

            ps.open(new URL(fullUrl));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String formatStringForUrl(String input) {
        try {
            // Encode the input string using UTF-8
            return URLEncoder.encode(input, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getConfigInfos(String nl) {
        String config = "";
        config += "OS and Dependencies Info"+nl;
        config +="```"+nl;
        config +="OS "+ System.getProperty("os.name")+nl;
        if (!pythonInformation.isEmpty()) {
            config += pythonInformation+nl;
        } else {
            config += "Not run from PyImageJ"+nl;
        }
        config +="ImageJ "+ VersionUtils.getVersion(ImageJ.class)+nl;
        config +="IJ "+ VersionUtils.getVersion(IJ.class)+nl;
        config +="ABBA "+ VersionUtils.getVersion(ABBAHelper.class)+nl;
        config +="BigWarp "+VersionUtils.getVersion(BigWarp.class)+nl;
        config +="Bdv "+VersionUtils.getVersion(BigDataViewer.class)+nl;
        config +="Bdv Vistools "+VersionUtils.getVersion(BdvHandle.class)+nl;
        config +="Bdv Biop Tools "+VersionUtils.getVersion(Elastix2DSplineRegister.class)+nl;
        config +="Bdv Playground "+VersionUtils.getVersion(SourceAndConverterServices.class)+nl;
        config +="Biop Image Loader "+VersionUtils.getVersion(BioFormatsHelper.class)+nl;
        config +="Biop Wrappers "+VersionUtils.getVersion(ElastixTask.class)+nl;
        if (Elastix.exePath!=null) {
            config += "Elastix Path: " + Elastix.exePath + " exists ?"+new File(Elastix.exePath).exists()+nl;
        } else {
            config += "Elastix path not set"+nl;
        }
        if (Transformix.exePath!=null) {
            config += "Transformix Path: " + Transformix.exePath + " exists ?"+new File(Transformix.exePath).exists()+nl;
        } else {
            config += "Transformix path not set"+nl;
        }
        if (DeepSlice.envDirPath!=null) {
            config += "Deepslice env dir: " + DeepSlice.envDirPath + " exists ?"+new File(DeepSlice.envDirPath).exists()+nl;
        } else {
            config += "Deepslice env dir not set"+nl;
        }
        config +="```"+nl;
        return config;
    }
}
