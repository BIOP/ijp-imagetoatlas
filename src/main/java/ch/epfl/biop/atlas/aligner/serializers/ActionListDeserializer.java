package ch.epfl.biop.atlas.aligner.serializers;

import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.registration.sourceandconverter.affine.Elastix2DAffineRegistration;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ActionListDeserializer implements JsonDeserializer<AlignerState.ActionList> {

    Consumer<SliceSources> sliceSourceConsumer;

    public ActionListDeserializer(Consumer<SliceSources> sliceSourceConsumer) {
        this.sliceSourceConsumer = sliceSourceConsumer;
    }

    @Override
    public AlignerState.ActionList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        List<CancelableAction> actions = new ArrayList<>();
        json.getAsJsonObject().get("actions").getAsJsonArray().forEach(jsonElement -> {
            System.out.println(jsonElement);
            CancelableAction action = context.deserialize(jsonElement, CancelableAction.class);
            action.runRequest();
            sliceSourceConsumer.accept(action.getSliceSources());
            actions.add(action);
        });

        AlignerState.ActionList actionList = new AlignerState.ActionList();
        actionList.actions = actions;
        return actionList;
    }
}
