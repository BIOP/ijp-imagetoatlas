package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.*;
import net.imglib2.realtransform.AffineTransform3D;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SliceSourcesStateDeserializer implements JsonDeserializer<AlignerState.SliceSourcesState> {

    final Consumer<SliceSources> sliceSourceConsumer;

    public SliceSourcesStateDeserializer(Consumer<SliceSources> sliceSourceConsumer) {
        this.sliceSourceConsumer = sliceSourceConsumer;
    }

    SliceSources slice;

    @Override
    public AlignerState.SliceSourcesState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        List<CancelableAction> actions = new ArrayList<>();

        JsonObject obj = json.getAsJsonObject();

        obj.get("actions").getAsJsonArray().forEach(jsonElement -> {
            CancelableAction action = context.deserialize(jsonElement, CancelableAction.class);
            action.runRequest();
            sliceSourceConsumer.accept(action.getSliceSources());
            slice = action.getSliceSources();
            actions.add(action);
        });

        AffineTransform3D preTransform = context.deserialize(obj.get("preTransform"), AffineTransform3D.class);

        AlignerState.SliceSourcesState sliceState = new AlignerState.SliceSourcesState();
        sliceState.actions = actions;
        sliceState.slice = slice;
        sliceState.preTransform = preTransform;

        return sliceState;
    }
}
