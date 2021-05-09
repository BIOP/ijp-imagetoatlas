package ch.epfl.biop.atlas.aligner.serializers;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.KeySliceOn;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class KeySliceOnAdapter implements JsonSerializer<KeySliceOn>,
        JsonDeserializer<KeySliceOn> {

    final MultiSlicePositioner mp;
    Supplier<SliceSources> currentSliceGetter;

    public KeySliceOnAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public KeySliceOn deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new KeySliceOn(mp, currentSliceGetter.get());
    }

    @Override
    public JsonElement serialize(KeySliceOn setAsKeySlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", KeySliceOn.class.getSimpleName());
        return obj;
    }
}
