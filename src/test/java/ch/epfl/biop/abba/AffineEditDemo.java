package ch.epfl.biop.abba;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.registration.plugin.RegistrationTypeProperties;
import ch.epfl.biop.registration.source.affine.AffineRegistration;
import ch.epfl.biop.source.processor.SourcesProcessorHelper;
import loci.common.DebugTools;
import net.imagej.ImageJ;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.realtransform.AffineTransform3D;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ch.epfl.biop.abba.InPlaneEditsDemo.*;

/**
 * Headless checks of the edition of an affine registration: the edition is undoable and redoable.
 * The edition is scripted (see {@link ScriptedEdit}), the editor window is checked by hand.
 */
public class AffineEditDemo {

    static { LegacyInjector.preinit(); }

    public static void main(String[] args) throws Exception {
        final ImageJ ij = new ImageJ();
        DebugTools.enableLogging("OFF");

        Atlas atlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");
        MultiSlicePositioner mp = newInstance(ij, atlas);
        importSlice(ij, mp, 5.0);

        editUndoRedo(mp, mp.getSlices().get(0));

        System.out.println(failures == 0 ? "ALL CHECKS PASSED" : failures + " CHECK(S) FAILED");
        System.exit(failures);
    }

    /** Edits a translation into a rotation, then undoes and redoes the edition */
    static void editUndoRedo(MultiSlicePositioner mp, SliceSources slice) {
        AffineTransform3D identity = sourceTransform(slice);
        AffineTransform3D translation = new AffineTransform3D();
        translation.translate(1.0, 0.5, 0);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AffineRegistration.TRANSFORM_KEY, AffineRegistration.affineTransform3DToString(translation));
        mp.registerSlices(Collections.singletonList(slice), ScriptedEdit::new,
                SourcesProcessorHelper.Identity(), SourcesProcessorHelper.Identity(), parameters);
        mp.waitForTasks();
        AffineTransform3D translated = identity.copy().preConcatenate(translation);
        check("edit: translation registered", equal(sourceTransform(slice), translated));
        int before = mp.userActionsSize();

        mp.selectSlice(slice);
        mp.editLastRegistrationSelectedSlices(true, SourcesProcessorHelper.Identity(), SourcesProcessorHelper.Identity());
        mp.deselectSlice(slice);
        mp.waitForTasks();
        AffineTransform3D edited = rotated(identity, 90);
        check("edit: translation edited into a rotation, still one registration",
                equal(sourceTransform(slice), edited) && (slice.getNumberOfRegistrations() == 1)
                        && (mp.userActionsSize() == before + 1));

        mp.cancelLastAction();
        mp.waitForTasks();
        check("edit: undo restores the translation", equal(sourceTransform(slice), translated)
                && (slice.getNumberOfRegistrations() == 1) && (mp.userActionsSize() == before));

        ScriptedEdit.edits = 0;
        mp.redoAction();
        mp.waitForTasks();
        check("edit: redo restores the rotation without editing again", equal(sourceTransform(slice), edited)
                && (ScriptedEdit.edits == 0) && (mp.userActionsSize() == before + 1));

        mp.cancelLastAction();
        mp.cancelLastAction();
        mp.waitForTasks();
        check("edit: undo the edition and the registration", equal(sourceTransform(slice), identity)
                && (slice.getNumberOfRegistrations() == 0));
    }

    /** An affine registration whose edition sets a rotation of 90 degrees about the origin, without any window */
    @RegistrationTypeProperties(isManual = false, isEditable = true)
    public static class ScriptedEdit extends AffineRegistration {

        static int edits = 0;

        @Override
        public boolean edit() {
            edits++;
            at3d = new AffineTransform3D();
            at3d.rotate(2, Math.PI / 2.0);
            return true;
        }
    }

}
