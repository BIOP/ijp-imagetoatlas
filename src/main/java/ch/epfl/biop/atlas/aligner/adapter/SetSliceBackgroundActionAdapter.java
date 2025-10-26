package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SetSliceBackgroundAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class SetSliceBackgroundActionAdapter implements JsonSerializer<SetSliceBackgroundAction>,
        JsonDeserializer<SetSliceBackgroundAction> {

    final MultiSlicePositioner mp;
    final Supplier<SliceSources> currentSliceGetter;

    public SetSliceBackgroundActionAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public SetSliceBackgroundAction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        int bgValue = obj.get("bgValue").getAsInt();
        return new SetSliceBackgroundAction(mp, currentSliceGetter.get(), bgValue);
    }

    @Override
    public JsonElement serialize(SetSliceBackgroundAction action, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", SetSliceBackgroundAction.class.getSimpleName());
        obj.addProperty("bgValue", action.getBgValue());
        return obj;
    }
}