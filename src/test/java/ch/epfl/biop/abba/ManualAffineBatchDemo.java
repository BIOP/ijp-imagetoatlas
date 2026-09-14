package ch.epfl.biop.abba;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import ch.epfl.biop.atlas.aligner.InPlaneTransform;
import ch.epfl.biop.atlas.aligner.LockAndRunOnceSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import ch.epfl.biop.atlas.aligner.command.RegisterSlicesManualAffineCommand;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.plugin.RegistrationPluginHelper;
import ch.epfl.biop.registration.source.affine.AffineEditor;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static ch.epfl.biop.abba.InPlaneEditsDemo.*;

/**
 * Headless checks of the manual affine registration of several slices, with a scripted editor instead of the
 * {@link AffineEditor} window, on the demo mouse atlas and three demo slices.
 */
public class ManualAffineBatchDemo {

    static { LegacyInjector.preinit(); }

    static final List<String> errors = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        DebugTools.enableLogging("OFF");

        Atlas atlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");
        MultiSlicePositioner mp = newInstance(ij, atlas);
        importSlice(ij, mp, 5.0);
        importSlice(ij, mp, 6.0);
        importSlice(ij, mp, 7.0);
        mp.errorMessageForUser = (title, message) -> {
            System.out.println("Error message: " + title + " - " + message);
            errors.add(title);
        };
        List<SliceSources> slices = mp.getSlices();

        quietSkip(mp, slices);
        manualAffine(mp, slices);
        cancelledManualAffine(mp, slices);
        waitsForTheEdition(mp, slices);

        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
        Runtime.getRuntime().halt(failures);
    }

    /** A batch whose registrations are never supplied leaves nothing: no registration, no error, no undo step */
    static void quietSkip(MultiSlicePositioner mp, List<SliceSources> slices) throws Exception {
        mp.transformSlicesInPlane(slices, translation(0.5, 0), "Shift");
        mp.waitForTasks();
        int before = mp.userActionsSize();
        String undoMessage = mp.getUndoMessage();

        // One registration, not supplied, out of any batch
        new RegisterSliceAction(mp, slices.get(0), new DeepSliceHelper.Holder<>(),
                SourcesProcessorHelper.Identity(), SourcesProcessorHelper.Identity(), "Nothing").runRequest(true);
        mp.waitForTasks();
        check("skip: a registration not supplied is not in the undo stack", (mp.userActionsSize() == before)
                && registrations(slices, 1) && errors.isEmpty());

        // The same, locked and batched as the manual affine registration does it
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean result = new AtomicBoolean();
        new MarkActionSequenceBatchAction(mp).runRequest();
        for (SliceSources slice : slices) {
            new LockAndRunOnceSliceAction(mp, slice, counter, slices.size(), () -> true, result).runRequest(true);
            new RegisterSliceAction(mp, slice, new DeepSliceHelper.Holder<>(),
                    SourcesProcessorHelper.Identity(), SourcesProcessorHelper.Identity(), "Nothing").runRequest(true);
        }
        new MarkActionSequenceBatchAction(mp).runRequest();
        mp.waitForTasks();
        check("skip: batch leaves no registration and no error message", registrations(slices, 1) && errors.isEmpty());
        check("skip: locks and skipped registrations are neither in the undo stack nor in the timeline",
                (mp.userActionsSize() == before + 2) && noLockNorSkippedRegistration(mp, slices));
        check("skip: the undo message ignores the empty batch", mp.getUndoMessage().equals(undoMessage));
        mp.cancelLastAction();
        mp.waitForTasks();
        check("skip: no extra undo step, the first undo removes the shift", registrations(slices, 0));
        mp.redoAction();
        mp.waitForTasks();
        check("skip: redo the shift", registrations(slices, 1));
        mp.cancelLastAction();
        mp.waitForTasks();
        check("skip: undone again", registrations(slices, 0) && errors.isEmpty());
    }

    /** Three slices get one "Manual Affine" registration each, in one undo step */
    static void manualAffine(MultiSlicePositioner mp, List<SliceSources> slices) throws Exception {
        int before = mp.userActionsSize();
        List<AffineTransform3D> initial = new ArrayList<>();
        for (SliceSources slice : slices) initial.add(sourceTransform(slice));
        List<AffineTransform3D> edited = new ArrayList<>();
        for (int i = 0; i < slices.size(); i++) edited.add(translation(0.1 * (i + 1), -0.2));
        int[] pairsReceived = new int[1];

        RegisterSlicesManualAffineCommand.registerSlices(mp, slices, SourcesProcessorHelper.Identity(), SourcesProcessorHelper.Identity(),
                pairs -> {
                    pairsReceived[0] = pairs.size();
                    return edited;
                });
        mp.waitForTasks();

        check("manual affine: the editor receives one pair per slice", pairsReceived[0] == slices.size());
        boolean transformed = true, named = true, editable = true, roi = true;
        for (int i = 0; i < slices.size(); i++) {
            SliceSources slice = slices.get(i);
            transformed &= equal(sourceTransform(slice), initial.get(i).copy().preConcatenate(edited.get(i)));
            named &= lastRegistrationName(mp, slice).equals(RegisterSlicesManualAffineCommand.REGISTRATION_NAME);
            Registration<SourceAndConverter<?>[]> registration = lastRegistration(mp, slice);
            editable &= RegistrationPluginHelper.isEditable(registration);
            roi &= Double.parseDouble(registration.getRegistrationParameters().get("px")) == mp.getROI()[0]
                    && Double.parseDouble(registration.getRegistrationParameters().get("sy")) == mp.getROI()[3];
        }
        check("manual affine: each slice gets its own transform, in slicing order", transformed && registrations(slices, 1));
        check("manual affine: registrations named 'Manual Affine', editable, with the ROI as parameters", named && editable && roi);
        check("manual affine: one batch, no lock left, no error",
                (mp.userActionsSize() == before + 2 + slices.size()) && noLockNorSkippedRegistration(mp, slices) && errors.isEmpty());

        mp.cancelLastAction();
        mp.waitForTasks();
        boolean restored = true;
        for (int i = 0; i < slices.size(); i++) restored &= equal(sourceTransform(slices.get(i)), initial.get(i));
        check("manual affine: one undo removes all", restored && registrations(slices, 0) && (mp.userActionsSize() == before));
        mp.redoAction();
        mp.waitForTasks();
        boolean redone = true;
        for (int i = 0; i < slices.size(); i++) {
            redone &= equal(sourceTransform(slices.get(i)), initial.get(i).copy().preConcatenate(edited.get(i)));
        }
        check("manual affine: redo restores all, without the editor", redone && registrations(slices, 1));
        mp.cancelLastAction();
        mp.waitForTasks();
    }

    /** Cancelling the edition adds nothing */
    static void cancelledManualAffine(MultiSlicePositioner mp, List<SliceSources> slices) throws Exception {
        mp.transformSlicesInPlane(slices, translation(0.5, 0), "Shift");
        mp.waitForTasks();
        int before = mp.userActionsSize();
        String undoMessage = mp.getUndoMessage();
        RegisterSlicesManualAffineCommand.registerSlices(mp, slices, SourcesProcessorHelper.Identity(), SourcesProcessorHelper.Identity(),
                pairs -> null);
        mp.waitForTasks();
        check("cancelled: no registration, no error message", registrations(slices, 1) && errors.isEmpty()
                && noLockNorSkippedRegistration(mp, slices));
        check("cancelled: only the empty batch marks, ignored by undo", (mp.userActionsSize() == before + 2)
                && mp.getUndoMessage().equals(undoMessage));
        mp.cancelLastAction();
        mp.waitForTasks();
        check("cancelled: the first undo removes the shift", registrations(slices, 0));
    }

    /** A registration requested on a slice while the editor is open waits for the end of the edition */
    static void waitsForTheEdition(MultiSlicePositioner mp, List<SliceSources> slices) throws Exception {
        CountDownLatch editorOpen = new CountDownLatch(1), userDone = new CountDownLatch(1);
        Function<List<AffineEditor.Pair>, List<AffineTransform3D>> editor = pairs -> {
            editorOpen.countDown();
            try {
                userDone.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            List<AffineTransform3D> transforms = new ArrayList<>();
            for (int i = 0; i < pairs.size(); i++) transforms.add(translation(0.3, 0));
            return transforms;
        };
        RegisterSlicesManualAffineCommand.registerSlices(mp, slices, SourcesProcessorHelper.Identity(), SourcesProcessorHelper.Identity(), editor);
        editorOpen.await();
        double[] c = mp.getROICenter();
        mp.transformSlicesInPlane(Collections.singletonList(slices.get(1)), InPlaneTransform.rotation(Math.PI / 2, c[0], c[1]).toAffine(), "Rotate");
        Thread.sleep(2000);
        check("while editing: the rotation of an edited slice waits", slices.get(1).getNumberOfRegistrations() == 0);
        userDone.countDown();
        mp.waitForTasks();
        check("after the edition: manual affine, then the rotation", (slices.get(1).getNumberOfRegistrations() == 2)
                && lastRegistrationName(mp, slices.get(1)).equals("Rotate") && registrations(slices.subList(2, 3), 1)
                && errors.isEmpty());
        mp.cancelLastAction();
        mp.waitForTasks();
        mp.cancelLastAction();
        mp.waitForTasks();
        check("undo the rotation and the manual affine", registrations(slices, 0));
    }

    static boolean noLockNorSkippedRegistration(MultiSlicePositioner mp, List<SliceSources> slices) {
        for (SliceSources slice : slices) {
            for (CancelableAction action : mp.getActionsFromSlice(slice)) {
                if (action instanceof LockAndRunOnceSliceAction) return false;
                if ((action instanceof RegisterSliceAction) && (((RegisterSliceAction) action).getRegistration() == null)) return false;
            }
        }
        return true;
    }

    static Registration<SourceAndConverter<?>[]> lastRegistration(MultiSlicePositioner mp, SliceSources slice) {
        List<CancelableAction> actions = mp.getActionsFromSlice(slice);
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.get(i) instanceof RegisterSliceAction) return ((RegisterSliceAction) actions.get(i)).getRegistration();
        }
        return null;
    }

    static AffineTransform3D translation(double x, double y) {
        AffineTransform3D t = new AffineTransform3D();
        t.translate(x, y, 0);
        return t;
    }
}
