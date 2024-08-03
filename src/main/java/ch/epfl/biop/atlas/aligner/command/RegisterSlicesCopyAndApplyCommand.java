package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.LockAndRunOnceSliceAction;
import ch.epfl.biop.atlas.aligner.MoveSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.adapter.AlignerState;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.scijava.command.source.register.ElastixHelper;
import com.google.gson.Gson;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.epfl.biop.atlas.aligner.adapter.AlignerState.filterSerializedActions;

@SuppressWarnings("CanBeFinal")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Align>ABBA - Copy and Apply Registration",
        description = "Copy the registration sequence of a slice and apply it to selected slices")
public class RegisterSlicesCopyAndApplyCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Index of the slice registrations you'd like to copy")
    int model_slice_index;

    @Parameter(label = "Tick if you want to skip the pre-transform (probably not)")
    boolean skip_pre_transform = false;


    public void run() {

        List<SliceSources> selectedSlices = mp.getSelectedSlices();

        if (selectedSlices.size()==0) {
            mp.warningMessageForUser.accept("No selected slice", "Please select the slice(s) you want to register");
            return;
        }

        if ((model_slice_index<0)||(model_slice_index>=mp.getSlices().size())) {
            mp.warningMessageForUser.accept("Invalid model slice index", "Please enter a valid slice index.");
            return;
        }

        SliceSources modelSlice = mp.getSlices().get(model_slice_index);

        if (selectedSlices.contains(modelSlice)) {
            selectedSlices.remove(modelSlice);
            mp.warningMessageForUser.accept("Model slice selected!", "The registration sequence will not be applied to the model slice.");
        }


        // Now let's get all the actions performed on the slice
        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean result = new AtomicBoolean();
        serializer = mp.getGsonStateSerializer(new ArrayList<>());

        new LockAndRunOnceSliceAction(mp, modelSlice, counter,1, () -> {
            // This block will be executed when the model slice has finished all jobs
            // First: copy all the actions, it's the code from the state serialisation
            AlignerState.SliceSourcesState slice_state = new AlignerState.SliceSourcesState();
            slice_state.actions.addAll(filterSerializedActions(mp.getActionsFromSlice(modelSlice)));
            slice_state.preTransform = modelSlice.getTransformSourceOrigin();

            for (SliceSources slice: selectedSlices) {
                // Apply all these actions on each slice, it's the deserialisation code
                for (CancelableAction action: slice_state.actions) {
                    // That's necessary because the bounding box needs to match the one of the original image
                    // Maybe that's an issue
                    /*if (action instanceof MoveSliceAction) {
                        MoveSliceAction moveSliceAction = new MoveSliceAction(mp, slice, ((MoveSliceAction) action).getSlicingAxisPosition());
                        moveSliceAction.runRequest();
                    }*/
                    if (action instanceof RegisterSliceAction) {
                        RegisterSliceAction regActionModel = copyReg((RegisterSliceAction) action);
                        RegisterSliceAction registerSlice = new RegisterSliceAction(mp, slice, regActionModel.getRegistration(), regActionModel.getFixedSourcesProcessor(), regActionModel.getMovingSourcesProcessor());
                        registerSlice.setRegistration(registerSlice.getRegistration());
                        registerSlice.runRequest();
                    }
                }
                if (!skip_pre_transform) {
                    slice.transformSourceOrigin(modelSlice.getTransformSourceOrigin().copy());
                }
            }
            return true;
        }, result).runRequest();
    }
    Gson serializer;
    private RegisterSliceAction copyReg(RegisterSliceAction action) {
         // Clone through serialisation and deserialisation
        String regString = serializer.toJson(action, RegisterSliceAction.class);
        return serializer.fromJson(regString, RegisterSliceAction.class);
    }

}
