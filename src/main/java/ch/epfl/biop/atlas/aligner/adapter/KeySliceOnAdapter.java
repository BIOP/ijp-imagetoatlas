package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.action.KeySliceOnAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class KeySliceOnAdapter implements JsonSerializer<KeySliceOnAction>,
        JsonDeserializer<KeySliceOnAction> {

    final MultiSlicePositioner mp;
    final Supplier<SliceSources> currentSliceGetter;

    public KeySliceOnAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public KeySliceOnAction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new KeySliceOnAction(mp, currentSliceGetter.get());
    }

    @Override
    public JsonElement serialize(KeySliceOnAction setAsKeySlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", KeySliceOnAction.class.getSimpleName());
        return obj;
    }
}
