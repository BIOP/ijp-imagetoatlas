package ch.epfl.biop.atlas.aligner.warp;

import bigwarp.transforms.NgffTransformations;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import loci.formats.in.NiftiReader;
import net.imglib2.realtransform.RealTransform;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5URI;
import org.janelia.saalfeldlab.n5.universe.N5Factory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Inflater;

/**
 * A 2D displacement field on a pixel grid, fixed to moving: the node of index (i, j) goes to (i, j) + d(i, j).
 * Displacements are in pixels: the readers convert the physical vectors of the file with its spacing and
 * direction, so that the field can be laid on any grid of the same size.
 */
public class DisplacementField {

    public final int width, height;
    final float[] dx, dy;

    DisplacementField(int width, int height, float[] dx, float[] dy) {
        this.width = width;
        this.height = height;
        this.dx = dx;
        this.dy = dy;
    }

    /** Writes where the point (x, y), in pixels, goes: bilinear interpolation, the border extends the field */
    public void apply(double x, double y, double[] out) {
        double cx = Math.max(0, Math.min(width - 1, x)), cy = Math.max(0, Math.min(height - 1, y));
        int i = (int) Math.min(width - 2, Math.floor(cx)), j = (int) Math.min(height - 2, Math.floor(cy));
        double u = cx - i, v = cy - j;
        out[0] = x + interpolate(dx, i, j, u, v);
        out[1] = y + interpolate(dy, i, j, u, v);
    }

    private double interpolate(float[] d, int i, int j, double u, double v) {
        int k = j * width + i;
        return (1 - u) * (1 - v) * d[k] + u * (1 - v) * d[k + 1] + (1 - u) * v * d[k + width] + u * v * d[k + 1 + width];
    }

    /** Physical vectors to pixels: v = L d, with L the physical size of a pixel step along each axis (columns) */
    static DisplacementField fromPhysical(int width, int height, float[] vx, float[] vy, double[] l) {
        double det = l[0] * l[3] - l[1] * l[2];
        float[] dx = new float[vx.length], dy = new float[vy.length];
        for (int k = 0; k < vx.length; k++) {
            dx[k] = (float) ((l[3] * vx[k] - l[1] * vy[k]) / det);
            dy[k] = (float) ((-l[2] * vx[k] + l[0] * vy[k]) / det);
        }
        return new DisplacementField(width, height, dx, dy);
    }

    /**
     * Reads a NIfTI displacement field written by ITK (ANTs, elastix): vectors in ITK's physical space (LPS) while
     * the header is in NIfTI's (RAS). Read by Bio-Formats, which gives the two components as two channels.
     */
    public static DisplacementField readNifti(File file) throws IOException {
        try (NiftiReader reader = new NiftiReader()) {
            reader.setId(file.getAbsolutePath());
            if (reader.getSizeZ() != 1 || reader.getSizeT() != 1 || reader.getSizeC() != 2) {
                throw new IOException("not a 2D displacement field: " + reader.getSizeX() + " x " + reader.getSizeY() + " x "
                        + reader.getSizeZ() + " pixels, " + reader.getSizeC() + " components");
            }
            int w = reader.getSizeX(), h = reader.getSizeY();
            float[][] v = new float[2][];
            for (int c = 0; c < 2; c++) v[c] = toFloats(reader.openBytes(reader.getIndex(0, c, 0)), reader.getPixelType(), reader.isLittleEndian());
            Map<String, Object> meta = reader.getGlobalMetadata();
            boolean sform = meta.containsKey("Affine transform X[0]") && number(meta, "S-form Code") > 0;
            // srow_x and srow_y (or the voxel size alone), in RAS: to LPS, x and y change sign
            double[] l = sform ? new double[]{-number(meta, "Affine transform X[0]"), -number(meta, "Affine transform X[1]"),
                    -number(meta, "Affine transform Y[0]"), -number(meta, "Affine transform Y[1]")}
                    : new double[]{-number(meta, "Voxel width"), 0, 0, -number(meta, "Voxel height")};
            return fromPhysical(w, h, v[0], v[1], l);
        } catch (loci.formats.FormatException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static double number(Map<String, Object> meta, String key) {
        return Double.parseDouble(String.valueOf(meta.get(key)));
    }

    private static float[] toFloats(byte[] bytes, int pixelType, boolean littleEndian) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        boolean isDouble = pixelType == loci.formats.FormatTools.DOUBLE;
        if (!isDouble && pixelType != loci.formats.FormatTools.FLOAT) throw new IOException("the vectors are not floating point numbers");
        float[] out = new float[bytes.length / (isDouble ? 8 : 4)];
        for (int k = 0; k < out.length; k++) out[k] = isDouble ? (float) buffer.getDouble() : buffer.getFloat();
        return out;
    }

    /**
     * Reads a MetaImage displacement field (.mha, or .mhd and its data file) as ITK writes it. Neither Bio-Formats nor
     * SCIFIO reads MetaImage, and Fiji's IO plugin (not a dependency of ABBA) reads scalar images only: this small reader
     * is limited to 2D fields of two channels.
     */
    public static DisplacementField readMetaImage(File file) throws IOException {
        byte[] all = Files.readAllBytes(file.toPath());
        Map<String, String> header = new HashMap<>();
        int pos = 0;
        while (!header.containsKey("ElementDataFile")) {
            int end = pos;
            while (end < all.length && all[end] != '\n') end++;
            if (end == all.length) throw new IOException("no ElementDataFile in the header");
            String line = new String(all, pos, end - pos, StandardCharsets.ISO_8859_1).trim();
            int eq = line.indexOf('=');
            if (eq > 0) header.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            pos = end + 1;
        }
        double[] size = numbers(header.get("DimSize"));
        if (!"2".equals(header.get("NDims")) || !"2".equals(header.get("ElementNumberOfChannels"))) {
            throw new IOException("not a 2D displacement field (NDims " + header.get("NDims") + ", ElementNumberOfChannels "
                    + header.get("ElementNumberOfChannels") + ")");
        }
        String data = header.get("ElementDataFile");
        byte[] bytes = data.equals("LOCAL") ? java.util.Arrays.copyOfRange(all, pos, all.length)
                : Files.readAllBytes(new File(file.getParentFile(), data).toPath());
        if ("True".equalsIgnoreCase(header.get("CompressedData"))) bytes = inflate(bytes);
        int w = (int) size[0], h = (int) size[1];
        boolean isDouble = "MET_DOUBLE".equals(header.get("ElementType"));
        if (!isDouble && !"MET_FLOAT".equals(header.get("ElementType"))) throw new IOException("ElementType must be MET_FLOAT or MET_DOUBLE");
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order("True".equalsIgnoreCase(header.get("BinaryDataByteOrderMSB"))
                || "True".equalsIgnoreCase(header.get("ElementByteOrderMSB")) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        float[] vx = new float[w * h], vy = new float[w * h];
        for (int k = 0; k < w * h; k++) {
            vx[k] = isDouble ? (float) buffer.getDouble() : buffer.getFloat();
            vy[k] = isDouble ? (float) buffer.getDouble() : buffer.getFloat();
        }
        double[] spacing = header.containsKey("ElementSpacing") ? numbers(header.get("ElementSpacing")) : new double[]{1, 1};
        // TransformMatrix lists the direction of each axis in turn
        double[] m = header.containsKey("TransformMatrix") ? numbers(header.get("TransformMatrix")) : new double[]{1, 0, 0, 1};
        return fromPhysical(w, h, vx, vy, new double[]{m[0] * spacing[0], m[2] * spacing[1], m[1] * spacing[0], m[3] * spacing[1]});
    }

    private static double[] numbers(String text) {
        return java.util.Arrays.stream(text.trim().split("\\s+")).mapToDouble(Double::parseDouble).toArray();
    }

    private static byte[] inflate(byte[] bytes) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(bytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[1 << 16];
        try {
            while (!inflater.finished()) {
                int n = inflater.inflate(chunk);
                if (n == 0 && inflater.needsInput()) throw new IOException("truncated compressed data");
                out.write(chunk, 0, n);
            }
        } catch (java.util.zip.DataFormatException e) {
            throw new IOException(e.getMessage(), e);
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }

    /**
     * Reads an OME-NGFF displacement field transformation with BigWarp, which writes them ('displacements'
     * coordinate transformation, RFC-5). The vectors are sampled at the nodes of the field, read from its
     * 'coordinateTransformations' (scale and translation).
     */
    public static DisplacementField readNgff(File folder) throws IOException {
        String url = NgffTransformations.detectTransforms(folder.getAbsolutePath());
        RealTransform transform = url == null ? null : NgffTransformations.open(url);
        if (transform == null) throw new IOException("no displacement field transformation found");
        N5Reader n5 = new N5Factory().gsonBuilder(NgffTransformations.gsonBuilder()).openReader(folder.getAbsolutePath());
        String group;
        try {
            group = new N5URI(url).getGroupPath();
        } catch (java.net.URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }
        JsonArray cts = n5.getAttribute(group, "coordinateTransformations", JsonArray.class);
        String field = cts.get(0).getAsJsonObject().get("path").getAsString();
        // Attributes list the axes as the dimensions of the dataset: the vector axis, of type 'displacement', and x, y
        long[] dims = n5.getDatasetAttributes(field).getDimensions();
        JsonArray axes = n5.getAttribute(field, "coordinateSystems", JsonArray.class).get(0).getAsJsonObject().getAsJsonArray("axes");
        int[] spatial = new int[2];
        int found = 0;
        for (int d = 0; d < axes.size(); d++) {
            if (!axes.get(d).getAsJsonObject().get("type").getAsString().equals("displacement") && found < 2) spatial[found++] = d;
        }
        if (dims.length != 3 || found != 2) throw new IOException("not a 2D displacement field");
        double[] scale = {1, 1}, offset = {0, 0};
        JsonArray toPhysical = n5.getAttribute(field, "coordinateTransformations", JsonArray.class);
        JsonObject first = toPhysical.get(0).getAsJsonObject();
        for (JsonElement e : first.has("transformations") ? first.getAsJsonArray("transformations") : toPhysical) {
            boolean isScale = e.getAsJsonObject().has("scale");
            JsonArray values = e.getAsJsonObject().getAsJsonArray(isScale ? "scale" : "translation");
            for (int k = 0; k < 2; k++) (isScale ? scale : offset)[k] = values.get(spatial[k]).getAsDouble();
        }
        dims = new long[]{dims[spatial[0]], dims[spatial[1]]};
        int w = (int) dims[0], h = (int) dims[1];
        float[] vx = new float[w * h], vy = new float[w * h];
        double[] p = new double[2], q = new double[2];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                p[0] = offset[0] + i * scale[0];
                p[1] = offset[1] + j * scale[1];
                transform.apply(p, q);
                vx[j * w + i] = (float) (q[0] - p[0]);
                vy[j * w + i] = (float) (q[1] - p[1]);
            }
        }
        return fromPhysical(w, h, vx, vy, new double[]{scale[0], 0, 0, scale[1]});
    }
}
