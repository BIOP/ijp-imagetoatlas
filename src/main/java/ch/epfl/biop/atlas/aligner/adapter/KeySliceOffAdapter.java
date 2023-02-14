package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.action.KeySliceOffAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class KeySliceOffAdapter implements JsonSerializer<KeySliceOffAction>,
        JsonDeserializer<KeySliceOffAction> {

    final MultiSlicePositioner mp;
    final Supplier<SliceSources> currentSliceGetter;

    public KeySliceOffAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public KeySliceOffAction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new KeySliceOffAction(mp, currentSliceGetter.get());
    }

    @Override
    public JsonElement serialize(KeySliceOffAction setAsKeySlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", KeySliceOffAction.class.getSimpleName());
        return obj;
    }
}
