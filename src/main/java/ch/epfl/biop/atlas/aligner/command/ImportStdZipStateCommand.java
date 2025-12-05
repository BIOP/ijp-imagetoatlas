package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvStartCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;
import ch.epfl.biop.atlas.scijava.AtlasChooserCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.java.utilities.TempDirectory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.task.Task;
import org.scijava.task.TaskService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static ch.epfl.biop.atlas.aligner.MultiSlicePositioner.pack;

@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>ABBA - Import Standardized ABBA Project (Zip)",
        description = "Opens a previously created zipped ABBA project.")
public class ImportStdZipStateCommand implements Command {

    @Parameter(style = "open")
    File zip_file;

    @Parameter
    Context ctx;

    @Parameter
    TaskService taskService;

    @Override
    public void run() {
        if (FilenameUtils.getExtension(zip_file.getName()).equals(".zip")) {
            System.err.println("Zip file expected");
            return;
        }
        Task importTask = taskService.createTask("Import of "+zip_file.getName());
        importTask.setProgressMaximum(3);

        try {
            importTask.start();
            System.out.println("Unzippping state file");
            File directory = unzipToTempFolder(zip_file);
            System.out.println(directory.getAbsolutePath());

            System.out.println("Deserializing meta.json");

            ABBAHelper.ABBAExportMeta meta;
            try (FileReader fileReader = new FileReader(new File(directory, "meta.json"))) {
                meta = new Gson().fromJson(fileReader, ABBAHelper.ABBAExportMeta.class);
            }

            if (meta == null) {
                System.err.println("Error during deserialisation of meta.json file");
                return;
            }
            System.out.println("Opening atlas: "+meta.atlas_name);

            Atlas atlas = (Atlas) ctx.getService(ModuleService.class).run(ctx.getService(CommandService.class).getCommand(AtlasChooserCommand.class), true,"choice", meta.atlas_name).get().getOutput("atlas");

            System.out.println("Creating BDV mp instance");

            BdvMultislicePositionerView view = (BdvMultislicePositionerView) ctx.getService(CommandService.class).run(ABBABdvStartCommand.class, true,
                    "x_axis", meta.x_axis,
                    "y_axis", meta.y_axis,
                    "z_axis", meta.z_axis,
                    "ba", atlas
                    ).get().getOutput("view");


            System.out.println("Restoring correct local paths");
            // We need to do some surgery on the abba state file, because the absolute path of the images will be incorrect
            File originalStateFile = new File(directory, "state.abba");
            // Let's unzip it
            File abbaStateFolder = unzipToTempFolder(originalStateFile);
            // Retrieve the xml file
            File xml = new File(abbaStateFolder, "_bdvdataset_0.xml");
            // Get the openers content
            String openers = extractOpenersContent(xml);
            // Update them
            String updatedOpeners = modifyLocations(openers, directory);
            // Resave the xml file
            replaceOpenersContent(xml, updatedOpeners);
            // Created back the abba state file
            pack(abbaStateFolder.getAbsolutePath(), new File(directory,"state_updated.abba").getAbsolutePath());
            // Finally load the project
            System.out.println("Loading project...");
            view.loadState("state_file", new File(directory, "state_updated.abba"));
            importTask.setStatusMessage("Done!");

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            importTask.finish();
        }


    }

    /**
     * Unzips a zip file into a temporary directory that will be deleted on JVM exit.
     *
     * @param zipFile The zip file to extract
     * @return The temporary directory containing the extracted files
     * @throws IOException If an I/O error occurs
     */
    public static File unzipToTempFolder(File zipFile) throws IOException {
        // Create a temporary directory
        // Path tempDir = Files.createTempDirectory("unzip_");
        Path tempDir = new TempDirectory(zipFile.getName()).getPath();
        File tempDirFile = tempDir.toFile();

        // Extract the zip file
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                File entryFile = new File(tempDirFile, entry.getName());

                // Security check: ensure the entry is within the temp directory
                if (!entryFile.toPath().normalize().startsWith(tempDir)) {
                    throw new IOException("Zip entry is outside of the target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    // Create parent directories if needed
                    entryFile.getParentFile().mkdirs();

                    // Extract file
                    try (FileOutputStream fos = new FileOutputStream(entryFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                zis.closeEntry();
            }
        }

        return tempDirFile;
    }

    public static String extractOpenersContent(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        NodeList openers = doc.getElementsByTagName("openers");
        if (openers.getLength() == 0) {
            throw new IllegalArgumentException("No <openers> tag found");
        }

        Node openersNode = openers.item(0);
        return getNodeContent(openersNode);
    }

    private static String getNodeContent(Node node) {
        StringBuilder content = new StringBuilder();
        NodeList children = node.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                content.append(child.getTextContent());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                content.append(nodeToString(child));
            }
        }

        return content.toString().trim();
    }

    private static String nodeToString(Node node) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String modifyLocations(String openersJson, File newDirectory) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Parse the JSON array
        JsonArray array = JsonParser.parseString(openersJson).getAsJsonArray();

        // Iterate through each object and modify the location
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("location")) {
                String oldLocation = obj.get("location").getAsString();
                String fileName = new File(oldLocation).getName();

                // Build new path with new directory
                String newLocation = new File(newDirectory, fileName).getAbsolutePath();
                obj.addProperty("location", newLocation);
            }
        }

        // Convert back to JSON string
        return gson.toJson(array);
    }

    public static void replaceOpenersContent(File xmlFile, String newContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        NodeList openers = doc.getElementsByTagName("openers");
        if (openers.getLength() == 0) {
            throw new IllegalArgumentException("No <openers> tag found");
        }

        Node openersNode = openers.item(0);

        // Clear existing content
        while (openersNode.hasChildNodes()) {
            openersNode.removeChild(openersNode.getFirstChild());
        }

        // Add new text content
        Text textNode = doc.createTextNode(newContent);
        openersNode.appendChild(textNode);

        // Save back to file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(xmlFile);
        transformer.transform(source, result);
    }
}
