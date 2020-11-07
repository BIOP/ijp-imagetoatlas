package ch.epfl.biop.atlastoimg2d.multislice.serializer;

import ch.epfl.biop.atlastoimg2d.multislice.MoveSlice;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import ch.epfl.biop.atlastoimg2d.multislice.SliceSources;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class MoveSliceAdapter implements JsonSerializer<MoveSlice>,
        JsonDeserializer<MoveSlice> {

    final MultiSlicePositioner mp;
    Supplier<SliceSources> currentSliceGetter;

    public MoveSliceAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public MoveSlice deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        double location = obj.get("location").getAsDouble();
        return new MoveSlice(mp, currentSliceGetter.get(), location);
    }

    @Override
    public JsonElement serialize(MoveSlice moveSlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", MoveSlice.class.getSimpleName());
        obj.addProperty("location", moveSlice.getSlicingAxisPosition());
        return obj;
    }
}
