package ch.epfl.biop.atlas.aligner;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.img.OpenersImageLoader;
import ch.epfl.biop.bdv.img.opener.OpenerSettings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import sc.fiji.bdvpg.scijava.service.SourceService;
import sc.fiji.bdvpg.service.SourceServices;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Creates a QuPath project (v0.7 format) whose images are the files the slices were opened from with Bio-Formats.
 * Only the simple case is supported: all channels of a slice come directly (no resampling, no transformation)
 * from a single series of a single 2D file. The QuPath image then has the same pixel grid as the slice, so the
 * regions and the transform exported by ABBA, which are in pixel coordinates, can be written in its data folder.
 */
public class QuPathProjectCreator {

    public static final String QUPATH_PROJECT_VERSION = "0.7.0";

    public static final String BIOFORMATS_SERVER_BUILDER = "qupath.lib.images.servers.bioformats.BioFormatsServerBuilder";

    /**
     * A series of a file read with Bio-Formats, which is what a QuPath image entry points to.
     */
    public static class BioFormatsImage {
        public final File file;
        public final int series;

        public BioFormatsImage(File file, int series) {
            this.file = file;
            this.series = series;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BioFormatsImage)) return false;
            BioFormatsImage other = (BioFormatsImage) o;
            return series == other.series && file.getAbsoluteFile().equals(other.file.getAbsoluteFile());
        }

        @Override
        public int hashCode() {
            return Objects.hash(file.getAbsoluteFile(), series);
        }

        @Override
        public String toString() {
            return file.getName()+" (series "+series+")";
        }
    }

    /**
     * @param slice a slice of ABBA
     * @return the file and the series all the channels of this slice come from
     * @throws IllegalArgumentException with a message for the user if the slice is not a single series of a 2D file
     * opened with Bio-Formats
     */
    public static BioFormatsImage getBioFormatsImage(SliceSources slice) throws IllegalArgumentException {
        BioFormatsImage image = null;
        SourceAndConverter<?>[] sources = slice.getOriginalSources();
        for (int iCh = 0; iCh < sources.length; iCh++) {
            BioFormatsImage channelImage = getBioFormatsImage(sources[iCh], iCh);
            if (image == null) {
                image = channelImage;
            } else if (!image.equals(channelImage)) {
                throw new IllegalArgumentException("its channels come from different images ("+image+" and "+channelImage+").");
            }
        }
        if (image == null) throw new IllegalArgumentException("it has no channel.");
        return image;
    }

    private static BioFormatsImage getBioFormatsImage(SourceAndConverter<?> source, int iCh) {
        Object info = SourceServices.getSourceService().getMetadata(source, SourceService.SPIM_DATA_INFO);
        if (info == null) {
            throw new IllegalArgumentException("channel "+iCh+" is not directly read from a file: "
                    + "it was resampled, transformed or computed before being imported in ABBA.");
        }
        SourceService.SpimDataInfo spimDataInfo = (SourceService.SpimDataInfo) info;
        BasicImgLoader imgLoader = spimDataInfo.asd.getSequenceDescription().getImgLoader();
        if (!(imgLoader instanceof OpenersImageLoader)) {
            throw new IllegalArgumentException("channel "+iCh+" is not read with Bio-Formats (image loader "
                    + imgLoader.getClass().getSimpleName()+").");
        }
        OpenersImageLoader openersImageLoader = (OpenersImageLoader) imgLoader;
        int openerIndex = openersImageLoader.getViewSetupToOpenerAndChannelIndex().get(spimDataInfo.setupId).getOpenerIndex();
        OpenerSettings settings = openersImageLoader.getOpenerSettings().get(openerIndex);
        if (settings.getType() == OpenerSettings.OpenerType.QUPATH) {
            throw new IllegalArgumentException("it was imported from a QuPath project: "
                    + "use 'ABBA - Export Registrations To QuPath Project' instead.");
        }
        if (settings.getType() != OpenerSettings.OpenerType.BIOFORMATS) {
            throw new IllegalArgumentException("channel "+iCh+" is not read with Bio-Formats (opener "+settings.getType()+").");
        }
        File file = new File(settings.getLocation());
        if (!file.exists()) {
            throw new IllegalArgumentException("its file "+file.getAbsolutePath()+" does not exist.");
        }
        long sizeZ = source.getSpimSource().getSource(0, 0).dimension(2);
        if (sizeZ > 1) {
            throw new IllegalArgumentException("it has "+sizeZ+" z planes; only 2D images are supported.");
        }
        return new BioFormatsImage(file, settings.getSeries());
    }

    /**
     * Writes a QuPath project with one image per Bio-Formats image, in this order. The image data folders are created,
     * but they contain no QuPath data: QuPath creates it when the image is opened.
     * @param projectFolder an empty or not yet existing folder
     * @param imageNames names of the images in the QuPath project
     * @param images file and series of each image
     * @return the data folder of each image, in the same order
     * @throws IOException if the project can't be written
     */
    public static List<File> createProject(File projectFolder, List<String> imageNames, List<BioFormatsImage> images) throws IOException {
        if (imageNames.size() != images.size()) {
            throw new IllegalArgumentException("There are "+imageNames.size()+" image names for "+images.size()+" images.");
        }
        Files.createDirectories(projectFolder.toPath());
        File projectFile = new File(projectFolder, "project.qpproj");
        File dataFolder = new File(projectFolder, "data");
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        long timestamp = System.currentTimeMillis();
        JsonObject project = new JsonObject();
        project.addProperty("version", QUPATH_PROJECT_VERSION);
        project.addProperty("createTimestamp", timestamp);
        project.addProperty("modifyTimestamp", timestamp);
        project.addProperty("uri", projectFile.getAbsoluteFile().toURI().toString());
        project.addProperty("lastID", images.size());

        JsonArray entries = new JsonArray();
        List<File> dataEntryFolders = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            int entryID = i + 1;
            JsonObject serverBuilder = getServerBuilder(images.get(i));

            JsonObject entry = new JsonObject();
            entry.add("serverBuilder", serverBuilder);
            entry.addProperty("entryID", entryID);
            entry.addProperty("randomizedName", UUID.randomUUID().toString());
            entry.addProperty("imageName", imageNames.get(i));
            entry.add("metadata", new JsonObject());
            entry.add("tags", new JsonArray());
            entries.add(entry);

            File dataEntryFolder = new File(dataFolder, Integer.toString(entryID));
            Files.createDirectories(dataEntryFolder.toPath());
            write(new File(dataEntryFolder, "server.json"), gson.toJson(serverBuilder));
            dataEntryFolders.add(dataEntryFolder);
        }
        project.add("images", entries);

        write(new File(dataFolder, "metadata.json"), "{}");
        write(projectFile, gson.toJson(project));
        return dataEntryFolders;
    }

    private static JsonObject getServerBuilder(BioFormatsImage image) {
        JsonObject serverBuilder = new JsonObject();
        serverBuilder.addProperty("builderType", "uri");
        serverBuilder.addProperty("providerClassName", BIOFORMATS_SERVER_BUILDER);
        serverBuilder.addProperty("uri", image.file.getAbsoluteFile().toURI().toString());
        JsonArray args = new JsonArray();
        args.add("--series");
        args.add(Integer.toString(image.series));
        serverBuilder.add("args", args);
        return serverBuilder;
    }

    /**
     * Writes a script in the project which imports the atlas regions in the current image. Run for all the images of
     * the project, it imports all the regions exported by ABBA. It requires the ABBA extension of QuPath.
     * @param projectFolder folder of the QuPath project
     * @param atlasName name of the atlas, as in the names of the exported files
     * @return the script file
     * @throws IOException if the script can't be written
     */
    public static File writeImportScript(File projectFolder, String atlasName) throws IOException {
        File scriptsFolder = new File(projectFolder, "scripts");
        Files.createDirectories(scriptsFolder.toPath());
        File script = new File(scriptsFolder, "Import_ABBA_atlas_regions.groovy");
        String escapedAtlasName = atlasName.replace("\\", "\\\\").replace("\"", "\\\"");
        write(script,
                "// Imports the atlas regions exported by ABBA into the current image, replacing previously imported ones.\n" +
                "// Requires the ABBA extension of QuPath. To import them in all images: Run > Run for project.\n" +
                "import qupath.ext.biop.abba.AtlasTools\n" +
                "\n" +
                "def atlasName = \""+escapedAtlasName+"\"\n" +
                "def namingProperty = \"acronym\" // property of the atlas ontology used to name the regions\n" +
                "def splitLeftRight = true\n" +
                "def overwrite = true\n" +
                "\n" +
                "def regions = AtlasTools.loadWarpedAtlasAnnotations(getCurrentImageData(), atlasName, namingProperty, splitLeftRight, overwrite)\n" +
                "if (regions == null) {\n" +
                "    println \"No \"+atlasName+\" regions found for \"+getProjectEntry().getImageName()\n" +
                "}\n");
        return script;
    }

    private static void write(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }
}
