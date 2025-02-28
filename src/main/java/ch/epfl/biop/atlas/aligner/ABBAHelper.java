package ch.epfl.biop.atlas.aligner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import java.net.HttpURLConnection;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ABBAHelper {

    protected static final Logger logger = LoggerFactory.getLogger(ABBAHelper.class);

    public static void displayABBALogo(int ms) {
        JFrame frameStart = new JFrame();
        frameStart.setUndecorated(true);
        frameStart.setBackground(new Color(1.0f, 1.0f, 1.0f, 0.5f));
        frameStart.setPreferredSize(new Dimension(960,540));
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        frameStart.setLocation(gd[0].getDefaultConfiguration().getBounds().width/2-960/2, gd[0].getDefaultConfiguration().getBounds().height/2-540/2);
        frameStart.pack();
        URL openImage = MultiSlicePositioner.class.getResource("/graphics/ABBA.png");
        try {
            BufferedImage myPicture = ImageIO.read(openImage);
            frameStart.add(new JLabel(new ImageIcon(myPicture)));
            frameStart.setVisible(true);
            Thread.sleep(ms);
            frameStart.setVisible(false);
            frameStart.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getMapUrl() {
        ABBASettings settings = getFromLocalFiji();
        if (settings==null) {
            return null; //Prefs.get(keyPrefix+"mapUrl","");
        } else {
            return settings.pathToABBAAtlas;
        }
    }

    public static String getOntologyUrl() {
        ABBASettings settings = getFromLocalFiji();
        if (settings==null) {
            return null;// Prefs.get(keyPrefix+"ontologyUrl","");
        } else {
            return settings.pathToABBAOntology;
        }
    }

    public static File getElastixExeFile() {
        ABBASettings settings = getFromLocalFiji();
        if (settings==null) {
            return null;
        } else {
            return new File(settings.pathToElastixExeFile);
        }
    }

    public static File getTransformixExeFile() {
        ABBASettings settings = getFromLocalFiji();
        if (settings==null) {
            return null;
        } else {
            return new File(settings.pathToTransformixExeFile);
        }
    }

    public static class ABBASettings {
        public String pathToABBAAtlas;
        public String pathToABBAOntology;
        public String pathToElastixExeFile;
        public String pathToTransformixExeFile;
    }

    static public ABBASettings getFromLocalFiji() {
        File abbasettings = new File("plugins"+File.separator+"abbasettings.txt");
        if (abbasettings.exists()) {
            Gson gson = new Gson();
            try {
                return gson.fromJson(new FileReader(abbasettings.getAbsoluteFile()), ABBASettings.class);
            } catch (FileNotFoundException e) {
                return null;
            }
        } else return null;
    }

    static public void setToLocalFiji(ABBASettings settings) {
        File abbaSettingsFile = new File("plugins"+File.separator+"abbasettings.txt");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String abbaSettings = gson.toJson(settings);
        try {
            PrintWriter out = new PrintWriter(abbaSettingsFile);
            out.println(abbaSettings);
            out.close();
        } catch (FileNotFoundException e) {
            logger.warn("Could not print abba settings file "+abbaSettingsFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    /**
     *
     * @param zipUrl the path to a zipped qupath project
     * @return the qpproject file contained in the zip
     * @throws IOException
     */
    static public File getTempQPProject(String zipUrl) throws Exception {
        // Create a temporary directory that will be deleted on JVM exit
        Path tempDir = Files.createTempDirectory("zipDownloadTemp");
        //tempDir.toFile().deleteOnExit();

        // Download the ZIP file
        Path zipFilePath = downloadZip(zipUrl, tempDir);

        // Extract the base name of the ZIP file from the URL
        String zipFileName = extractFileNameFromUrl(zipUrl);
        String targetFilePath = zipFileName.replace(".zip", "") + "/project.qpproj";

        // Unzip the file
        unzip(zipFilePath, tempDir);

        // Get the desired file
        File targetFile = tempDir.resolve(targetFilePath).toFile();

        if (targetFile.exists()) {
            return targetFile;
        } else {
            throw new IOException("File not found in the unzipped contents.");
        }
    }


    private static Path downloadZip(String zipUrl, Path tempDir) throws IOException {
        URL url = new URL(zipUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        Path zipFilePath = tempDir.resolve("downloaded.zip");
        try (InputStream in = connection.getInputStream();
             OutputStream out = Files.newOutputStream(zipFilePath)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        return zipFilePath;
    }

    private static String extractFileNameFromUrl(String zipUrl) throws Exception {
        URL url = new URL(zipUrl);
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static void unzip(Path zipFilePath, Path tempDir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path entryPath = tempDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zipInputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        File f = getTempQPProject("https://zenodo.org/records/14918378/files/abba-omero-gerbi-subset.zip");
        System.out.println(f.getAbsolutePath());

    }


}
