package ch.epfl.biop.atlas.aligner.serializers;

import ch.epfl.biop.atlas.aligner.KeySliceOff;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class KeySliceOffAdapter implements JsonSerializer<KeySliceOff>,
        JsonDeserializer<KeySliceOff> {

    final MultiSlicePositioner mp;
    Supplier<SliceSources> currentSliceGetter;

    public KeySliceOffAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public KeySliceOff deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new KeySliceOff(mp, currentSliceGetter.get());
    }

    @Override
    public JsonElement serialize(KeySliceOff setAsKeySlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", KeySliceOff.class.getSimpleName());
        return obj;
    }
}
