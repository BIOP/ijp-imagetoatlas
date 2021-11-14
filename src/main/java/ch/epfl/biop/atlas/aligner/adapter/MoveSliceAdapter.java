package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.action.MoveSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class MoveSliceAdapter implements JsonSerializer<MoveSliceAction>,
        JsonDeserializer<MoveSliceAction> {

    final MultiSlicePositioner mp;
    Supplier<SliceSources> currentSliceGetter;

    public MoveSliceAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public MoveSliceAction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        double location = obj.get("location").getAsDouble();
        return new MoveSliceAction(mp, currentSliceGetter.get(), location);
    }

    @Override
    public JsonElement serialize(MoveSliceAction moveSlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", MoveSliceAction.class.getSimpleName());
        obj.addProperty("location", moveSlice.getSlicingAxisPosition());
        return obj;
    }
}
