package ch.epfl.biop.abba;

import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.InPlaneTransform;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MirrorSliceZAction;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.aligner.command.ImportSlicesFromFilesCommand;
import ch.epfl.biop.atlas.aligner.command.MirrorDoCommand;
import ch.epfl.biop.atlas.aligner.command.RotateSlicesCommand;
import ch.epfl.biop.atlas.aligner.gui.bdv.card.EditPanel;
import ch.epfl.biop.atlas.aligner.inspect.SliceSnapshot;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ij.IJ;
import ij.ImagePlus;
import loci.common.DebugTools;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.out.OMETiffWriter;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Intervals;
import ome.units.UNITS;
import ome.units.quantity.Length;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Headless checks of the undoable in-plane edits (rotations, flips) on the demo mouse atlas and two demo slices.
 * Renders are written to {@link #OUT}. When they exist there, renders and a state made on master with the old
 * pre-transform rotations (master_*.png, master_old_pretransform.abba) are compared with the new ones.
 */
public class InPlaneEditsDemo {

    static { LegacyInjector.preinit(); }

    static final String OUT = "F:/code/github/abba-agent-scratch/in-plane-edits/";

    static int failures = 0;

    public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        DebugTools.enableLogging("OFF");

        Atlas atlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");
        MultiSlicePositioner mp = newInstance(ij, atlas);
        importSlice(ij, mp, 5.0);
        importSlice(ij, mp, 7.0);
        List<SliceSources> slices = mp.getSlices();

        mirrorUndo(ij, mp, slices);
        inPlaneTransformMath();
        inPlaneTransforms(mp, slices);
        flips(ij, mp, slices);
        rotationsSaveReload(ij, mp, slices);
        oldState(ij, atlas);

        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
        System.exit(failures);
    }

    /** Mirroring several slices is a single undo step */
    static void mirrorUndo(ImageJ ij, MultiSlicePositioner mp, List<SliceSources> slices) throws Exception {
        mp.selectSlice(slices);
        int before = mp.userActionsSize();
        ij.command().run(MirrorDoCommand.class, true, "mp", mp, "mirror_side", "Left").get();
        mp.waitForTasks();
        check("mirror: 2 registrations and 2 marks", (mp.userActionsSize() - before == 4) && registrations(slices, 1));
        mp.cancelLastAction();
        mp.waitForTasks();
        check("mirror: one undo", (mp.userActionsSize() == before) && registrations(slices, 0));
        mp.redoAction();
        mp.waitForTasks();
        check("mirror: one redo", (mp.userActionsSize() - before == 4) && registrations(slices, 1));
        mp.cancelLastAction();
        mp.waitForTasks();
        mp.deselectSlice(slices);
    }

    /** InPlaneTransform composition, against imglib2 concatenations */
    static void inPlaneTransformMath() {
        InPlaneTransform t = new InPlaneTransform(0.3, 1.2, 0.8, 0.15, 0.4, -0.2, 1.5, -0.7);
        // T(c + t) · R · Sh · S · T(-c), built from the right
        AffineTransform3D expected = new AffineTransform3D();
        expected.translate(-t.pivotX, -t.pivotY, 0);
        AffineTransform3D scale = new AffineTransform3D();
        scale.set(t.scaleX, 0, 0);
        scale.set(t.scaleY, 1, 1);
        expected.preConcatenate(scale);
        AffineTransform3D shear = new AffineTransform3D();
        shear.set(t.shear, 0, 1);
        expected.preConcatenate(shear);
        expected.rotate(2, t.angle);
        expected.translate(t.pivotX + t.translationX, t.pivotY + t.translationY, 0);
        check("InPlaneTransform: toAffine is T(c+t).R.Sh.S.T(-c)", equal(t.toAffine(), expected));

        double[] p = {1.5, -0.7, 3};
        InPlaneTransform.rotation(1, 1.5, -0.7).toAffine().apply(p, p);
        check("InPlaneTransform: a rotation keeps its pivot and z",
                (Math.abs(p[0] - 1.5) < 1e-12) && (Math.abs(p[1] + 0.7) < 1e-12) && (p[2] == 3));
        p = new double[]{0, 1, 0};
        InPlaneTransform.rotation(Math.PI / 2.0, 0, 0).toAffine().apply(p, p);
        check("InPlaneTransform: +90 degrees sends (0, 1) (below the pivot) to (-1, 0) (left): clockwise on screen",
                (Math.abs(p[0] + 1) < 1e-12) && (Math.abs(p[1]) < 1e-12));
    }

    /** Translation and rotation renders, one undo step for several slices */
    static void inPlaneTransforms(MultiSlicePositioner mp, List<SliceSources> slices) throws Exception {
        SliceSources s0 = slices.get(0);
        AffineTransform3D identity = sourceTransform(s0);
        compareToMaster("no edit", render(mp, s0, "identity"), "master_identity");
        int before = mp.userActionsSize();

        mp.transformSlicesInPlane(Collections.singletonList(s0),
                new InPlaneTransform(0, 1, 1, 0, 1.0, 0.5, 0, 0).toAffine(), "In-plane transform");
        render(mp, s0, "translate_x+1_y+0.5");
        AffineTransform3D translated = identity.copy();
        translated.translate(1.0, 0.5, 0);
        check("translation: slice moved by (1, 0.5) mm", equal(sourceTransform(s0), translated));
        check("translation: registration named 'In-plane transform'", lastRegistrationName(mp, s0).equals("In-plane transform"));
        mp.cancelLastAction();
        mp.waitForTasks();
        check("translation: undone", (mp.userActionsSize() == before) && equal(sourceTransform(s0), identity));

        mp.transformSlicesInPlane(slices, InPlaneTransform.rotation(Math.PI / 2.0, 0, 0).toAffine(), "Rotate");
        BufferedImage rotated = render(mp, s0, "rotate+90");
        check("rotation +90: source transform of the old pre-transform rotation", equal(sourceTransform(s0), rotated(identity, 90)));
        compareToMaster("rotation +90", rotated, "master_rotZ+90");
        mp.cancelLastAction();
        mp.waitForTasks();
        check("rotation +90 of 2 slices: one undo", (mp.userActionsSize() == before) && registrations(slices, 0)
                && equal(sourceTransform(s0), identity));
        mp.redoAction();
        mp.waitForTasks();
        check("rotation +90 of 2 slices: one redo", (mp.userActionsSize() - before == 4) && registrations(slices, 1));
        mp.cancelLastAction();
        mp.waitForTasks();
    }

    /** Flips around X and Y: same result as the old 180 degrees rotations of the pre-transform, undo, redo, save and reload */
    static void flips(ImageJ ij, MultiSlicePositioner mp, List<SliceSources> slices) throws Exception {
        SliceSources s0 = slices.get(0), s1 = slices.get(1);
        AffineTransform3D identity = sourceTransform(s0);
        double thickness = s0.getThicknessInMm();
        double[] zRange = zRange(s0);
        AffineTransform3D mirrorZ = new AffineTransform3D();
        mirrorZ.set(-1, 2, 2);
        JPanel editCard = new EditPanel(mp).getPanel();
        JButton[] flipButtons = {(JButton) editCard.getComponent(2), (JButton) editCard.getComponent(3)};
        AffineTransform3D[] oldResults = new AffineTransform3D[2];
        int before = mp.userActionsSize();

        for (int axis = 0; axis < 2; axis++) {
            String name = (axis == 0) ? "X" : "Y";
            // What the old SliceSources.rotateSourceOrigin(axis, PI) did on a slice without pre-transform
            AffineTransform3D oldPreTransform = new AffineTransform3D();
            oldPreTransform.rotate(axis, Math.PI);
            s0.transformSourceOrigin(oldPreTransform);
            oldResults[axis] = sourceTransform(s0);
            s0.transformSourceOrigin(new AffineTransform3D());

            mp.selectSlice(s0);
            flipButtons[axis].doClick();
            mp.deselectSlice(s0);
            BufferedImage image = render(mp, s0, "button_flip" + name);
            check("flip " + name + " button: source transform of the old rotation of the pre-transform by 180 degrees",
                    equal(sourceTransform(s0), oldResults[axis]));
            compareToMaster("flip " + name + " button", image, "master_rot" + name + "180");
            check("flip " + name + ": 'Flip' registration, z mirror in the pre-transform, same thickness and z range",
                    lastRegistrationName(mp, s0).equals("Flip") && equal(s0.getTransformSourceOrigin(), mirrorZ)
                            && (s0.getThicknessInMm() == thickness) && equalRange(zRange(s0), zRange));
            mp.cancelLastAction();
            mp.waitForTasks();
            check("flip " + name + ": one undo reverts the registration and the z mirror", (mp.userActionsSize() == before)
                    && (s0.getNumberOfRegistrations() == 0) && equal(s0.getTransformSourceOrigin(), new AffineTransform3D())
                    && equal(sourceTransform(s0), identity));
            mp.redoAction();
            mp.waitForTasks();
            check("flip " + name + ": one redo", (s0.getNumberOfRegistrations() == 1) && equal(s0.getTransformSourceOrigin(), mirrorZ)
                    && equal(sourceTransform(s0), oldResults[axis]));
            mp.cancelLastAction();
            mp.waitForTasks();
        }

        mp.flipSlices(slices, 0);
        mp.waitForTasks();
        check("flip X of 2 slices", registrations(slices, 1) && equal(s1.getTransformSourceOrigin(), mirrorZ) && (mp.userActionsSize() - before == 6));
        mp.cancelLastAction();
        mp.waitForTasks();
        check("flip X of 2 slices: one undo", registrations(slices, 0) && (mp.userActionsSize() == before)
                && equal(s0.getTransformSourceOrigin(), new AffineTransform3D()) && equal(s1.getTransformSourceOrigin(), new AffineTransform3D()));

        List<String> errors = new ArrayList<>();
        BiConsumer<String, String> errorListener = (title, message) -> errors.add(title);
        mp.subscribeToErrorMessages(errorListener);
        mp.selectSlice(s0);
        ij.command().run(RotateSlicesCommand.class, true, "mp", mp, "axis_string", "X", "angle_degrees", 90.0).get();
        mp.waitForTasks();
        check("RotateSlicesCommand X 90: error message, nothing done", (errors.size() == 1) && (mp.userActionsSize() == before)
                && equal(sourceTransform(s0), identity));
        ij.command().run(RotateSlicesCommand.class, true, "mp", mp, "axis_string", "Y", "angle_degrees", -180.0).get();
        mp.waitForTasks();
        mp.deselectSlice(s0);
        mp.unSubscribeFromErrorMessages(errorListener);
        check("RotateSlicesCommand Y -180: flip around Y", (errors.size() == 1) && lastRegistrationName(mp, s0).equals("Flip")
                && equal(s0.getTransformSourceOrigin(), mirrorZ) && equal(sourceTransform(s0), oldResults[1]));

        File state = new File(OUT, "flips.abba");
        state.delete(); // saveState fails on an existing file, even when overwriting
        check("flips: state saved", mp.saveState(state, true));
        MultiSlicePositioner reloaded = newInstance(ij, mp.getAtlas());
        check("flips: state reloaded", reloaded.loadState(state));
        reloaded.waitForTasks();
        SliceSources loaded = reloaded.getSlices().get(0);
        check("flips: reload keeps the z mirror once, through the pre-transform, and the 'Flip' registration",
                equal(loaded.getTransformSourceOrigin(), mirrorZ) && equal(sourceTransform(loaded), oldResults[1])
                        && (loaded.getNumberOfRegistrations() == 1) && lastRegistrationName(reloaded, loaded).equals("Flip")
                        && reloaded.getActionsFromSlice(loaded).stream().noneMatch(action -> action instanceof MirrorSliceZAction));
        render(reloaded, loaded, "reloaded_flipY");

        mp.cancelLastAction();
        mp.waitForTasks();
        check("flips: undone", (mp.userActionsSize() == before) && equal(sourceTransform(s0), identity));
    }

    /** Edit card rotation buttons and RotateSlicesCommand (Z): undo, redo, then save and reload in a new instance */
    static void rotationsSaveReload(ImageJ ij, MultiSlicePositioner mp, List<SliceSources> slices) throws Exception {
        SliceSources s0 = slices.get(0), s1 = slices.get(1);
        AffineTransform3D identity0 = sourceTransform(s0), identity1 = sourceTransform(s1);
        JPanel editCard = new EditPanel(mp).getPanel();
        JButton clockwise = (JButton) editCard.getComponent(0), counterClockwise = (JButton) editCard.getComponent(1);
        int before = mp.userActionsSize();

        mp.selectSlice(s0);
        clockwise.doClick();
        BufferedImage image = render(mp, s0, "button_clockwise");
        check("clockwise button: +90 degrees, registration named 'Rotate'",
                equal(sourceTransform(s0), rotated(identity0, 90)) && lastRegistrationName(mp, s0).equals("Rotate"));
        compareToMaster("clockwise button", image, "master_rotZ+90");
        counterClockwise.doClick();
        mp.waitForTasks();
        check("counter clockwise button: back to the initial position, with 2 registrations",
                equal(sourceTransform(s0), identity0) && (s0.getNumberOfRegistrations() == 2));
        mp.cancelLastAction();
        mp.waitForTasks();
        check("undo counter clockwise", equal(sourceTransform(s0), rotated(identity0, 90)) && (s0.getNumberOfRegistrations() == 1));
        mp.cancelLastAction();
        mp.waitForTasks();
        check("undo clockwise", equal(sourceTransform(s0), identity0) && (s0.getNumberOfRegistrations() == 0) && (mp.userActionsSize() == before));
        mp.redoAction();
        mp.waitForTasks();
        check("redo clockwise", equal(sourceTransform(s0), rotated(identity0, 90)) && (s0.getNumberOfRegistrations() == 1));

        mp.selectSlice(s1);
        ij.command().run(RotateSlicesCommand.class, true, "mp", mp, "axis_string", "Z", "angle_degrees", 30.0).get();
        mp.waitForTasks();
        check("RotateSlicesCommand Z +30 on 2 slices, one undo step",
                equal(sourceTransform(s0), rotated(identity0, 120)) && equal(sourceTransform(s1), rotated(identity1, 30))
                        && (mp.userActionsSize() - before == 5));
        render(mp, s0, "saved_s0_rotate+120");
        render(mp, s1, "saved_s1_rotate+30");
        mp.deselectSlice(slices);

        File state = new File(OUT, "rotations.abba");
        state.delete(); // saveState fails on an existing file, even when overwriting
        check("rotations: state saved", mp.saveState(state, true));
        MultiSlicePositioner reloaded = newInstance(ij, mp.getAtlas());
        check("rotations: state reloaded", reloaded.loadState(state));
        reloaded.waitForTasks();
        List<SliceSources> loaded = reloaded.getSlices();
        check("rotations: reloaded edits replayed as registrations, pre-transform identity", (loaded.size() == 2)
                && equal(sourceTransform(loaded.get(0)), sourceTransform(s0)) && equal(sourceTransform(loaded.get(1)), sourceTransform(s1))
                && (loaded.get(0).getNumberOfRegistrations() == 2) && (loaded.get(1).getNumberOfRegistrations() == 1)
                && lastRegistrationName(reloaded, loaded.get(0)).equals("Rotate")
                && equal(loaded.get(0).getTransformSourceOrigin(), new AffineTransform3D()));
        render(reloaded, loaded.get(0), "reloaded_s0");
    }

    /** A state saved on master with non-identity pre-transforms still loads, and new edits apply after them */
    static void oldState(ImageJ ij, Atlas atlas) throws Exception {
        File state = new File(OUT, "master_old_pretransform.abba");
        if (!state.exists()) {
            System.out.println("SKIP old state: no " + state);
            return;
        }
        MultiSlicePositioner mp = newInstance(ij, atlas);
        check("old state: loaded", mp.loadState(state));
        mp.waitForTasks();
        List<SliceSources> slices = mp.getSlices();
        AffineTransform3D pre0 = new AffineTransform3D(), pre1 = new AffineTransform3D();
        pre0.rotate(2, Math.PI / 6.0);
        pre1.rotate(0, Math.PI);
        pre1.rotate(2, -Math.PI / 9.0);
        check("old state: pre-transforms rotate(Z, 30) and rotate(X, 180) then rotate(Z, -20)", (slices.size() == 2)
                && equal(slices.get(0).getTransformSourceOrigin(), pre0) && equal(slices.get(1).getTransformSourceOrigin(), pre1));
        compareToMaster("old state slice 0", render(mp, slices.get(0), "oldstate_s0"), "master_oldstate_s0");
        compareToMaster("old state slice 1", render(mp, slices.get(1), "oldstate_s1"), "master_oldstate_s1");

        SliceSources s1 = slices.get(1);
        AffineTransform3D loaded = sourceTransform(s1);
        mp.transformSlicesInPlane(Collections.singletonList(s1), InPlaneTransform.rotation(Math.PI / 2.0, 0, 0).toAffine(), "Rotate");
        mp.waitForTasks();
        check("old state: a new rotation applies after the pre-transform", equal(sourceTransform(s1), rotated(loaded, 90)));
        render(mp, s1, "oldstate_s1_rotate+90");
    }

    // ------------------------------------ Helpers

    static MultiSlicePositioner newInstance(ImageJ ij, Atlas atlas) throws Exception {
        return (MultiSlicePositioner) ij.command().run(ABBAStartCommand.class, true,
                "x_axis", "RL", "y_axis", "SI", "z_axis", "AP", "ba", atlas).get().getOutput("mp");
    }

    /**
     * Imports the demo slice with Bio-Formats, from an OME-TIFF copy that carries the pixel size: a slice imported
     * from an ImageJ image can't be reloaded from a saved state, its file path is saved as "Untitled"
     */
    static void importSlice(ImageJ ij, MultiSlicePositioner mp, double axisPosition) throws Exception {
        File file = new File(OUT, "demoSlice.ome.tif");
        if (!file.exists()) {
            ImagePlus imp = IJ.openImage("src/test/resources/demoSlice.tif");
            IMetadata meta = MetadataTools.createOMEXMLMetadata();
            MetadataTools.populateMetadata(meta, 0, "demoSlice", false, "XYZCT",
                    FormatTools.getPixelTypeString(FormatTools.UINT8), imp.getWidth(), imp.getHeight(), 1, 1, 1, 1);
            // Pixel size of the demo slice when imported as an ImageJ image
            meta.setPixelsPhysicalSizeX(new Length(0.015, UNITS.MILLIMETER), 0);
            meta.setPixelsPhysicalSizeY(new Length(0.015, UNITS.MILLIMETER), 0);
            try (OMETiffWriter writer = new OMETiffWriter()) {
                writer.setMetadataRetrieve(meta);
                writer.setId(file.getAbsolutePath());
                writer.saveBytes(0, (byte[]) imp.getProcessor().convertToByteProcessor().getPixels());
            }
        }
        ij.command().run(ImportSlicesFromFilesCommand.class, true, "mp", mp, "datasetname", "demo", "files", new File[]{file},
                "split_rgb_channels", false, "first_slice_position_mm", axisPosition, "slice_spacing_mm", 0.0).get();
        mp.waitForTasks();
        mp.deselectSlice(mp.getSlices());
    }

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failures++;
    }

    static boolean registrations(List<SliceSources> slices, int n) {
        return slices.stream().allMatch(slice -> slice.getNumberOfRegistrations() == n);
    }

    static String lastRegistrationName(MultiSlicePositioner mp, SliceSources slice) {
        List<CancelableAction> actions = mp.getActionsFromSlice(slice);
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i) instanceof RegisterSliceAction) {
                return ((RegisterSliceAction) actions.get(i)).getRegistration().getRegistrationName();
            }
        }
        return "";
    }

    /** Source transform of the first channel of the registered slice: pixel to aligner coordinates */
    static AffineTransform3D sourceTransform(SliceSources slice) {
        AffineTransform3D at = new AffineTransform3D();
        slice.getRegisteredSources()[0].getSpimSource().getSourceTransform(0, 0, at);
        return at;
    }

    /** z range {min, max} covered by the voxels of the registered slice, in mm */
    static double[] zRange(SliceSources slice) {
        long[] dims = Intervals.dimensionsAsLongArray(slice.getRegisteredSources()[0].getSpimSource().getSource(0, 0));
        double[] a = {-0.5, -0.5, -0.5}, b = {dims[0] - 0.5, dims[1] - 0.5, dims[2] - 0.5};
        sourceTransform(slice).apply(a, a);
        sourceTransform(slice).apply(b, b);
        return new double[]{Math.min(a[2], b[2]), Math.max(a[2], b[2])};
    }

    static boolean equalRange(double[] a, double[] b) {
        return (Math.abs(a[0] - b[0]) < 1e-9) && (Math.abs(a[1] - b[1]) < 1e-9);
    }

    /** @return the transform followed by an in-plane rotation about the origin, in degrees */
    static AffineTransform3D rotated(AffineTransform3D transform, double degrees) {
        AffineTransform3D rotation = new AffineTransform3D();
        rotation.rotate(2, Math.toRadians(degrees));
        return transform.copy().preConcatenate(rotation);
    }

    static boolean equal(AffineTransform3D a, AffineTransform3D b) {
        double[] ma = a.getRowPackedCopy(), mb = b.getRowPackedCopy();
        for (int i = 0; i < ma.length; i++) {
            if (Math.abs(ma[i] - mb[i]) > 1e-9) return false;
        }
        return true;
    }

    static BufferedImage render(MultiSlicePositioner mp, SliceSources slice, String name) throws Exception {
        mp.waitForTasks();
        SliceSnapshot.Options options = new SliceSnapshot.Options();
        options.showRegionLabels = false;
        options.pixelSizeMm = 0.04;
        BufferedImage image = SliceSnapshot.render(mp, slice, options);
        SliceSnapshot.save(image, OUT + name + ".png");
        return image;
    }

    /** Compares a render with the one made on master, below the header (it shows the number of registrations) */
    static void compareToMaster(String name, BufferedImage image, String masterName) throws Exception {
        File file = new File(OUT, masterName + ".png");
        if (!file.exists()) {
            System.out.println("SKIP " + name + ": no " + file);
            return;
        }
        BufferedImage master = ImageIO.read(file);
        long sum = 0, n = 0;
        int header = 36; // SliceSnapshot.HEADER_HEIGHT
        boolean sameSize = (master.getWidth() == image.getWidth()) && (master.getHeight() == image.getHeight());
        for (int y = header; sameSize && (y < image.getHeight()); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int a = image.getRGB(x, y), b = master.getRGB(x, y);
                sum += Math.abs(((a >> 16) & 0xff) - ((b >> 16) & 0xff)) + Math.abs(((a >> 8) & 0xff) - ((b >> 8) & 0xff))
                        + Math.abs((a & 0xff) - (b & 0xff));
                n += 3;
            }
        }
        double diff = sameSize ? sum / (double) n : Double.NaN;
        System.out.println("  " + name + ": mean absolute difference with " + masterName + ".png = " + diff);
        check(name + ": render identical to " + masterName + ".png", diff < 0.5);
    }

}
