package ch.epfl.biop.atlas.aligner.serializers;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SetAsKeySlice;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class SetAsKeySliceAdapter implements JsonSerializer<SetAsKeySlice>,
        JsonDeserializer<SetAsKeySlice> {

    final MultiSlicePositioner mp;
    Supplier<SliceSources> currentSliceGetter;

    public SetAsKeySliceAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public SetAsKeySlice deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        double location = obj.get("location").getAsDouble();
        return new SetAsKeySlice(mp, currentSliceGetter.get());
    }

    @Override
    public JsonElement serialize(SetAsKeySlice setAsKeySlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", SetAsKeySlice.class.getSimpleName());
        return obj;
    }
}
