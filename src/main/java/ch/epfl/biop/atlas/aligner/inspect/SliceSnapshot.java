package ch.epfl.biop.atlas.aligner.inspect;

import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import net.imglib2.RealRandomAccess;
import net.imglib2.converter.Converter;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import sc.fiji.bdvpg.source.SourceHelper;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ObjIntConsumer;
import java.util.stream.IntStream;

/**
 * Renders slices of a {@link MultiSlicePositioner} to plain RGB images, without any viewer.
 * Each image shows the registered slice at its position along the slicing axis, optionally
 * blended with an atlas channel, with the atlas region borders and acronyms drawn over it.
 * A header describes the slice and rulers give the aligner coordinates in millimeters, so
 * that a position read on the image can be fed back to ABBA.
 * Meant for quick visual checks, by humans or by vision models driving ABBA through scripts.
 */
public class SliceSnapshot {

    public static class Options {
        /** Pixel size of the rendered image, in millimeters */
        public double pixelSizeMm = 0.025;
        /** Rendered region {x, y, width, height} in millimeters (aligner coordinates), null for the positioner ROI */
        public double[] regionMm = null;
        /** Renders the slice channels */
        public boolean showSlice = true;
        /** Indices of the slice channels to render, null for all channels */
        public int[] sliceChannels = null;
        /** 0: current registration, 1: state before the last registration, etc. */
        public int registrationStepBack = 0;
        /**
         * Position (mm) along the slicing axis where the atlas is rendered, in the atlas convention
         * ({@link MultiSlicePositioner#toAtlasZ(double)}), NaN for the position of the slice
         */
        public double atlasZMm = Double.NaN;
        /** Index of the atlas structural channel blended with the slice, -1 for none */
        public int atlasChannel = -1;
        /** Weight of the atlas channel in the additive blending */
        public double atlasOpacity = 0.5;
        /** Displays the atlas channel in gray, from its 1st to its 99.5th percentile, instead of using its display settings */
        public boolean atlasAutoContrast = true;
        /** Atlas regions covering less than this fraction of the image are merged into their parent, 0 keeps the finest regions */
        public double regionMinArea = 0.01;
        public boolean showRegionBorders = true;
        /** Draws the region acronyms, where they do not overlap */
        public boolean showRegionLabels = true;
        /** Draws a header describing the slice */
        public boolean showHeader = true;
        /** Draws rulers with millimeter coordinates */
        public boolean showRulers = true;
        public int timepoint = 0;
    }

    static final int RULER_LEFT = 44, RULER_BOTTOM = 22, HEADER_HEIGHT = 36, MARGIN = 6;
    static final Color BORDER_COLOR = new Color(255, 255, 0, 150), LABEL_COLOR = new Color(0, 255, 255);

    /**
     * @param slice the slice to render, or null to render the atlas alone, at options.atlasZMm
     */
    public static BufferedImage render(MultiSlicePositioner mp, SliceSources slice, Options options) {
        Grid grid = new Grid(options.regionMm != null ? options.regionMm : mp.getROI(), options.pixelSizeMm);
        double atlasZ = atlasPosition(mp, slice, options);
        int[] red = new int[grid.size()], green = new int[grid.size()], blue = new int[grid.size()];
        if (options.showSlice && (slice != null)) accumulateSlice(slice, options, grid, red, green, blue);
        if (options.atlasChannel >= 0) accumulateAtlas(mp, options, grid, atlasZ, red, green, blue);

        int[] pixels = new int[grid.size()];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (Math.min(255, red[i]) << 16) | (Math.min(255, green[i]) << 8) | Math.min(255, blue[i]);
        }

        Regions regions = null;
        if (options.showRegionBorders || options.showRegionLabels) {
            regions = mergeSmallRegions(mp.getAtlas().getOntology(), sampleLabels(mp, options, grid, atlasZ), options.regionMinArea);
            if (options.showRegionBorders) drawBorders(pixels, regions.ids, grid);
        }

        BufferedImage image = new BufferedImage(grid.w, grid.h, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, grid.w, grid.h, pixels, 0, grid.w);
        return decorate(mp, slice, atlasZ, image, grid, options.showRegionLabels ? regions : null, options);
    }

    /**
     * Renders several slices and tiles them in a grid
     * @param columns number of columns, 0 or less for an automatic choice
     */
    public static BufferedImage overview(MultiSlicePositioner mp, List<SliceSources> slices, Options options, int columns) {
        List<BufferedImage> tiles = new ArrayList<>();
        slices.stream().parallel().map(slice -> render(mp, slice, options)).forEachOrdered(tiles::add);
        return tile(tiles, columns);
    }

    /**
     * Tiles images in a grid, row by row
     * @param columns number of columns, 0 or less for an automatic choice
     */
    public static BufferedImage tile(List<BufferedImage> tiles, int columns) {
        if (tiles.isEmpty()) throw new IllegalArgumentException("No image to tile");
        int nColumns = columns > 0 ? columns : (int) Math.ceil(Math.sqrt(tiles.size()));
        int nRows = (int) Math.ceil(tiles.size() / (double) nColumns);
        int tileW = tiles.stream().mapToInt(BufferedImage::getWidth).max().getAsInt();
        int tileH = tiles.stream().mapToInt(BufferedImage::getHeight).max().getAsInt();
        int gap = 4;
        BufferedImage sheet = new BufferedImage(nColumns * (tileW + gap) - gap, nRows * (tileH + gap) - gap, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int i = 0; i < tiles.size(); i++) {
            g.drawImage(tiles.get(i), (i % nColumns) * (tileW + gap), (i / nColumns) * (tileH + gap), null);
        }
        g.dispose();
        return sheet;
    }

    /** Writes the image as a PNG file and returns its absolute path */
    public static String save(BufferedImage image, String path) throws IOException {
        File file = new File(path);
        if ((file.getParentFile() != null) && (!file.getParentFile().exists())) file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file);
        return file.getAbsolutePath();
    }

    // ------------------------------------ Sampling

    /** Pixel grid of a snapshot, in aligner coordinates (mm) */
    static class Grid {
        final double x, y, pixelSize;
        final int w, h;

        Grid(double[] region, double pixelSize) {
            this.x = region[0];
            this.y = region[1];
            this.pixelSize = pixelSize;
            this.w = Math.max(1, (int) Math.round(region[2] / pixelSize));
            this.h = Math.max(1, (int) Math.round(region[3] / pixelSize));
        }

        int size() { return w * h; }
    }

    /** Samples the source at each pixel center of the grid, at position z along the slicing axis; rows are processed in parallel */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void sample(SourceAndConverter<?> sac, int t, int level, Interpolation interpolation, Grid grid, double z,
                               ObjIntConsumer<Object> consumer) {
        Source src = sac.getSpimSource();
        if (!src.isPresent(t)) return;
        AffineTransform3D toSource = new AffineTransform3D();
        src.getSourceTransform(t, level, toSource);
        AffineTransform3D toPixel = toSource.inverse();
        IntStream.range(0, grid.h).parallel().forEach(j -> {
            RealRandomAccess<?> access = src.getInterpolatedSource(t, level, interpolation).realRandomAccess();
            double[] pos = new double[3];
            for (int i = 0; i < grid.w; i++) {
                pos[0] = grid.x + (i + 0.5) * grid.pixelSize;
                pos[1] = grid.y + (j + 0.5) * grid.pixelSize;
                pos[2] = z;
                toPixel.apply(pos, pos);
                access.setPosition(pos);
                consumer.accept(access.get(), i + j * grid.w);
            }
        });
    }

    /** @return the position of the model, where the atlas is sampled */
    private static double atlasPosition(MultiSlicePositioner mp, SliceSources slice, Options options) {
        if (!Double.isNaN(options.atlasZMm)) return mp.fromAtlasZ(options.atlasZMm);
        if (slice == null) throw new IllegalArgumentException("Rendering the atlas alone requires options.atlasZMm");
        return slice.getSlicingAxisPosition();
    }

    private static void accumulateSlice(SliceSources slice, Options options, Grid grid, int[] red, int[] green, int[] blue) {
        SourceAndConverter<?>[] sources = slice.getRegisteredSources(options.registrationStepBack);
        int[] channels = options.sliceChannels != null ? options.sliceChannels : IntStream.range(0, sources.length).toArray();
        for (int ch : channels) {
            if ((ch < 0) || (ch >= sources.length)) {
                throw new IllegalArgumentException("Slice channel " + ch + " does not exist, the slice has " + sources.length + " channels");
            }
            accumulate(sources[ch], options.timepoint, grid, slice.getSlicingAxisPosition(), 1.0, red, green, blue);
        }
    }

    private static void accumulateAtlas(MultiSlicePositioner mp, Options options, Grid grid, double z, int[] red, int[] green, int[] blue) {
        if (options.atlasChannel >= mp.getReslicedAtlas().getLabelSourceIndex()) {
            throw new IllegalArgumentException("Atlas channel " + options.atlasChannel + " does not exist, the atlas has "
                    + mp.getReslicedAtlas().getLabelSourceIndex() + " structural channels");
        }
        SourceAndConverter<?> sac = mp.getReslicedAtlas().nonExtendedSlicedSources[options.atlasChannel];
        if (!options.atlasAutoContrast || !(sac.getSpimSource().getType() instanceof RealType)) {
            accumulate(sac, options.timepoint, grid, z, options.atlasOpacity, red, green, blue);
            return;
        }
        double[] values = new double[grid.size()];
        sample(sac, options.timepoint, bestLevel(sac, options.timepoint, grid), Interpolation.NLINEAR, grid, z,
                (value, index) -> values[index] = ((RealType<?>) value).getRealDouble());
        double[] sorted = Arrays.stream(values).filter(v -> v > 0).sorted().toArray();
        if (sorted.length == 0) return;
        double min = sorted[(int) (0.01 * (sorted.length - 1))];
        double max = Math.max(min + 1e-9, sorted[(int) (0.995 * (sorted.length - 1))]);
        for (int i = 0; i < values.length; i++) {
            int gray = (int) (options.atlasOpacity * 255 * Math.min(1, Math.max(0, (values[i] - min) / (max - min))));
            red[i] += gray;
            green[i] += gray;
            blue[i] += gray;
        }
    }

    private static int[] sampleLabels(MultiSlicePositioner mp, Options options, Grid grid, double z) {
        int[] labels = new int[grid.size()];
        sample(mp.getReslicedAtlas().nonExtendedSlicedSources[mp.getReslicedAtlas().getLabelSourceIndex()], options.timepoint, 0,
                Interpolation.NEARESTNEIGHBOR, grid, z, (value, index) -> labels[index] = (int) ((RealType<?>) value).getRealDouble());
        return labels;
    }

    private static int bestLevel(SourceAndConverter<?> sac, int t, Grid grid) {
        Source<?> src = sac.getSpimSource();
        return Math.max(0, Math.min(src.getNumMipmapLevels() - 1, SourceHelper.bestLevel(src, t, grid.pixelSize)));
    }

    /** Converts the source with its converter (display settings) and adds its weighted RGB values */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void accumulate(SourceAndConverter<?> sac, int t, Grid grid, double z, double weight, int[] red, int[] green, int[] blue) {
        Converter converter = sac.getConverter();
        sample(sac, t, bestLevel(sac, t, grid), Interpolation.NLINEAR, grid, z, (value, index) -> {
            ARGBType argb = new ARGBType();
            converter.convert(value, argb);
            red[index] += (int) (weight * ARGBType.red(argb.get()));
            green[index] += (int) (weight * ARGBType.green(argb.get()));
            blue[index] += (int) (weight * ARGBType.blue(argb.get()));
        });
    }

    // ------------------------------------ Atlas regions

    /** Atlas region of each pixel, and the acronym of each region (the root and the outside have none) */
    static class Regions {
        final int[] ids;
        final Map<Integer, String> acronyms = new HashMap<>();
        Regions(int[] ids) { this.ids = ids; }
    }

    /** Assigns each pixel to its deepest ontology node, itself included, covering at least minArea of the image */
    private static Regions mergeSmallRegions(AtlasOntology ontology, int[] labels, double minArea) {
        Regions regions = new Regions(labels.clone());
        if (ontology == null) return regions;
        long minPixels = (long) Math.ceil(minArea * labels.length);

        Map<Integer, Long> labelCounts = new HashMap<>();
        for (int label : labels) labelCounts.merge(label, 1L, Long::sum);

        Map<Integer, Long> areas = new HashMap<>(); // per ontology id, descendants included
        labelCounts.forEach((label, count) -> {
            for (AtlasNode n = ontology.getNodeFromId(label); n != null; n = n.parent()) areas.merge(n.getId(), count, Long::sum);
        });

        Map<Integer, Integer> labelToRegion = new HashMap<>();
        labelCounts.keySet().forEach(label -> {
            AtlasNode n = ontology.getNodeFromId(label);
            while ((n != null) && (n.parent() != null) && (areas.get(n.getId()) < minPixels)) n = n.parent();
            labelToRegion.put(label, n == null ? 0 : n.getId());
            if ((n != null) && (n.parent() != null)) regions.acronyms.put(n.getId(), acronym(ontology, n));
        });
        for (int i = 0; i < labels.length; i++) regions.ids[i] = labelToRegion.get(labels[i]);
        return regions;
    }

    private static String acronym(AtlasOntology ontology, AtlasNode node) {
        Map<String, String> data = node.data();
        if (data.containsKey("acronym")) return data.get("acronym");
        String name = data.get(ontology.getNamingProperty());
        return name != null ? name : String.valueOf(node.getId());
    }

    private static void drawBorders(int[] pixels, int[] regionIds, Grid grid) {
        double alpha = BORDER_COLOR.getAlpha() / 255.0;
        for (int j = 0; j < grid.h; j++) {
            for (int i = 0; i < grid.w; i++) {
                int idx = i + j * grid.w;
                boolean border = ((i < grid.w - 1) && (regionIds[idx] != regionIds[idx + 1]))
                        || ((j < grid.h - 1) && (regionIds[idx] != regionIds[idx + grid.w]));
                if (border) {
                    int p = pixels[idx];
                    int r = (int) ((1 - alpha) * ((p >> 16) & 0xff) + alpha * BORDER_COLOR.getRed());
                    int g = (int) ((1 - alpha) * ((p >> 8) & 0xff) + alpha * BORDER_COLOR.getGreen());
                    int b = (int) ((1 - alpha) * (p & 0xff) + alpha * BORDER_COLOR.getBlue());
                    pixels[idx] = (r << 16) | (g << 8) | b;
                }
            }
        }
    }

    /**
     * Draws each acronym at the region pixel closest to the region centroid, largest regions first,
     * skipping the acronyms that would overlap an already drawn one
     */
    private static void drawRegionLabels(Graphics2D g, Regions regions, Grid grid, int left, int top) {
        Map<Integer, double[]> stats = new HashMap<>(); // sum x, sum y, count, closest x, closest y, closest squared distance
        for (int pass = 0; pass < 2; pass++) {
            for (int j = 0; j < grid.h; j++) for (int i = 0; i < grid.w; i++) {
                int id = regions.ids[i + j * grid.w];
                if (!regions.acronyms.containsKey(id)) continue;
                double[] s = stats.computeIfAbsent(id, k -> new double[]{0, 0, 0, 0, 0, Double.MAX_VALUE});
                if (pass == 0) {
                    s[0] += i; s[1] += j; s[2]++;
                } else {
                    double d = Math.pow(i - s[0] / s[2], 2) + Math.pow(j - s[1] / s[2], 2);
                    if (d < s[5]) { s[3] = i; s[4] = j; s[5] = d; }
                }
            }
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        FontMetrics fm = g.getFontMetrics();
        List<Rectangle> drawn = new ArrayList<>();
        stats.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue()[2], a.getValue()[2]))
                .forEach(entry -> {
                    String text = regions.acronyms.get(entry.getKey());
                    int x = left + (int) entry.getValue()[3] - fm.stringWidth(text) / 2;
                    int y = top + (int) entry.getValue()[4] + fm.getAscent() / 2;
                    Rectangle box = new Rectangle(x - 2, y - fm.getAscent(), fm.stringWidth(text) + 4, fm.getHeight());
                    if (drawn.stream().anyMatch(box::intersects)) return;
                    drawn.add(box);
                    g.setColor(Color.BLACK);
                    g.drawString(text, x + 1, y + 1);
                    g.setColor(LABEL_COLOR);
                    g.drawString(text, x, y);
                });
    }

    // ------------------------------------ Decorations

    private static BufferedImage decorate(MultiSlicePositioner mp, SliceSources slice, double atlasZ, BufferedImage image, Grid grid,
                                          Regions regions, Options options) {
        int left = options.showRulers ? RULER_LEFT : 0;
        int bottom = options.showRulers ? RULER_BOTTOM : 0;
        int top = options.showHeader ? HEADER_HEIGHT : 0;
        BufferedImage out = new BufferedImage(left + grid.w + MARGIN, top + grid.h + bottom, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(30, 30, 30));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(image, left, top, null);

        if (regions != null) drawRegionLabels(g, regions, grid, left, top);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        if (options.showHeader) {
            StringBuilder header = new StringBuilder();
            if (slice != null) {
                header.append(String.format(Locale.ROOT, "#%d z=%.3f reg=%d", mp.getSlices().indexOf(slice),
                        mp.toAtlasZ(slice.getSlicingAxisPosition()), slice.getNumberOfRegistrations()));
                if (options.registrationStepBack > 0) header.append("-").append(options.registrationStepBack);
                if (slice.isKeySlice()) header.append(" KEY");
                if (slice.isSelected()) header.append(" SEL");
            }
            if ((slice == null) || (atlasZ != slice.getSlicingAxisPosition())) {
                header.append(String.format(Locale.ROOT, " atlas z=%.3f", mp.toAtlasZ(atlasZ)));
            }
            if (options.atlasChannel >= 0) header.append(" +atlas ch").append(options.atlasChannel);
            g.setColor(Color.WHITE);
            g.drawString(header.toString().trim(), 4, 14);
            g.setColor(Color.LIGHT_GRAY);
            g.drawString(slice != null ? slice.getName() : mp.getAtlas().getName(), 4, 30);
        }

        if (options.showRulers) {
            g.setColor(Color.LIGHT_GRAY);
            FontMetrics fm = g.getFontMetrics();
            double step = niceStep(Math.max(grid.w, grid.h) * grid.pixelSize);
            for (double x = Math.ceil(grid.x / step) * step; x <= grid.x + grid.w * grid.pixelSize; x += step) {
                int px = left + (int) Math.round((x - grid.x) / grid.pixelSize);
                g.drawLine(px, top + grid.h, px, top + grid.h + 4);
                g.drawString(format(x), px - fm.stringWidth(format(x)) / 2, top + grid.h + 16);
            }
            for (double y = Math.ceil(grid.y / step) * step; y <= grid.y + grid.h * grid.pixelSize; y += step) {
                int py = top + (int) Math.round((y - grid.y) / grid.pixelSize);
                g.drawLine(left - 4, py, left, py);
                g.drawString(format(y), left - 6 - fm.stringWidth(format(y)), py + fm.getAscent() / 2 - 1);
            }
            g.drawString("mm", 2, top + grid.h + 16);
        }
        g.dispose();
        return out;
    }

    /** A 1, 2 or 5 power of ten step giving about 8 ticks over the extent */
    private static double niceStep(double extent) {
        double raw = extent / 8.0;
        double magnitude = Math.pow(10, Math.floor(Math.log10(raw)));
        for (double m : new double[]{1, 2, 5}) {
            if (m * magnitude >= raw) return m * magnitude;
        }
        return 10 * magnitude;
    }

    private static String format(double v) {
        return (Math.abs(v - Math.round(v)) < 1e-9) ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
    }

}
