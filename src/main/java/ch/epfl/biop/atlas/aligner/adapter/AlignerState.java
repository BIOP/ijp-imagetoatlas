package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.*;
import ch.epfl.biop.atlas.aligner.action.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.imglib2.realtransform.RealTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlignerState {

    protected static final Logger logger = LoggerFactory.getLogger(AlignerState.class);

    public AlignerState(MultiSlicePositioner mp) {

        rotationX = mp.getReslicedAtlas().getRotateX();
        rotationY = mp.getReslicedAtlas().getRotateY();

        mp.getSlices().forEach(sliceSource -> {
            SliceSourcesState slice_state = new SliceSourcesState();
            slice_state.actions.addAll(filterSerializedActions(mp.getActionsFromSlice(sliceSource)));
            slice_state.preTransform = sliceSource.getTransformSourceOrigin();
            slices_state_list.add(slice_state);
        });
    }

    public String version;

    public double rotationX;

    public double rotationY;

    public final List<SliceSourcesState> slices_state_list = new ArrayList<>();

    public static class SliceSourcesState {
        transient public SliceSources slice;
        public RealTransform preTransform; // In reality : AffineTransform3D, but for serialization, it's RealTransform
        public List<CancelableAction> actions = new ArrayList<>();
    }

    /*
    Some actions will not be serialized like the export actions and we
    need to somehow 'compile' actions to get rid of some actions which are there
    for user convenience but that we do not want to keep.
    For instance a series of attempted registration then deleted will not be saved.
     */

    static public List<CancelableAction> filterSerializedActions(List<CancelableAction> ini_actions) {
        Set<Class<? extends CancelableAction>> serializableActions = new HashSet<>();
        serializableActions.add(CreateSliceAction.class);
        serializableActions.add(MoveSliceAction.class);
        serializableActions.add(RegisterSliceAction.class);
        serializableActions.add(KeySliceOnAction.class);
        serializableActions.add(KeySliceOffAction.class);
        serializableActions.add(UnMirrorSliceAction.class);
        serializableActions.add(SetSliceBackgroundAction.class);
        //serializableActions.add(RasterDeformationAction.class);

        Set<Class<? extends CancelableAction>> skipableActions = new HashSet<>();
        skipableActions.add(ExportSliceRegionsToFileAction.class);
        skipableActions.add(ExportSliceRegionsToQuPathProjectAction.class);
        skipableActions.add(ExportSliceRegionsToRoiManagerAction.class);
        skipableActions.add(EditLastRegistrationAction.class); //TODO : make it serializable ? serializable or not ?
        skipableActions.add(ExportAtlasSliceToImagePlusAction.class);
        skipableActions.add(RasterDeformationAction.class);
        skipableActions.add(LockAndRunOnceSliceAction.class);
        skipableActions.add(RasterSliceAction.class);
        skipableActions.add(ExportSliceToImagePlusAction.class);
        skipableActions.add(ExportDeformationFieldToImagePlusAction.class);

        List<CancelableAction> compiledActions = new ArrayList<>();
        if ((ini_actions == null)||(ini_actions.size()==0)) {
            logger.error("Wrong number of actions to be serialized");
            return compiledActions;
        }
        if (!(ini_actions.get(0) instanceof CreateSliceAction)) {
            logger.error("Error : the first action is not a CreateSlice action");
            return compiledActions;
        }
        int idxCompiledActions = 0;
        int idxIniActions = 0;
        while (ini_actions.size()>idxIniActions) {
            CancelableAction nextAction = ini_actions.get(idxIniActions);
            if (serializableActions.contains(nextAction.getClass())) {
                idxCompiledActions++;
                idxIniActions++;
                compiledActions.add(nextAction);
            } else {
                if (skipableActions.contains(nextAction.getClass())) {
                    idxIniActions++;
                } else {
                    if (nextAction instanceof DeleteLastRegistrationAction) {
                        // For now...
                        int idxLastRegistration = idxCompiledActions-1;
                        while ((idxLastRegistration>0)&&!(compiledActions.get(idxLastRegistration) instanceof RegisterSliceAction)) {
                            idxLastRegistration--;
                        }
                        if (idxLastRegistration>=0) {
                            if (compiledActions.get(idxLastRegistration) instanceof RegisterSliceAction) {
                                compiledActions.remove(idxLastRegistration);
                                idxCompiledActions--;
                                idxIniActions++;
                            } else {
                                logger.error("Error : issue with DeleteLastRegistrationAction action!!!!");
                                idxIniActions++;
                            }
                        } else {
                            logger.error("Error : issue with DeleteLastRegistrationAction action!!!!");
                            idxIniActions++;
                        }
                    } else {
                        logger.warn("Error : issue with filtering serializable actions. Action class = "+nextAction.getClass());
                        idxIniActions++;
                    }
                }
            }
        }

        return compiledActions;
    }


    static final Set<String> toReplace = new HashSet<>();

    static {
        toReplace.add("MoveSlice");
        toReplace.add("CreateSlice");
        toReplace.add("RegisterSlice");
        toReplace.add("KeySliceOff");
        toReplace.add("KeySliceOn");
    }

    /**
     * In the previous ABBA version, CreateSliceAction was named CreateSlice, etc.
     * All these need to be changed before being open.
     * Function is there for legacy reasons.
     * @param element a json element to convert
     * @return converted json element
     */
    public static JsonElement convertOldJson(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object  = (JsonObject) element;
            object.entrySet().forEach(e -> {
                if (e.getKey().equals("type")) {
                    String val = e.getValue().getAsString();
                    if (toReplace.contains(val)) {
                        e.setValue(new JsonPrimitive(val+"Action")); // stupid renaming
                    }
                } else {
                    e.setValue(convertOldJson(e.getValue()));
                }
            });
        } else if (element.isJsonArray()) {
            JsonArray array = (JsonArray) element;
            JsonArray converted = new JsonArray();
            for (JsonElement e : array) {
                converted.add(convertOldJson(e));
            }
            element = converted;
        }
        return element;
    }

}
