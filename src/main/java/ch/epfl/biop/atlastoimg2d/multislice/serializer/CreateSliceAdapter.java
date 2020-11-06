package ch.epfl.biop.atlastoimg2d.multislice.serializer;

import ch.epfl.biop.atlastoimg2d.multislice.CreateSlice;
import ch.epfl.biop.atlastoimg2d.multislice.SliceSources;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;

public class CreateSliceAdapter implements JsonSerializer<CreateSlice>,
        JsonDeserializer<CreateSlice> {

    final Map<SliceSources, Integer> map_slice_index;

    public CreateSliceAdapter(Map<SliceSources, Integer> map_slice_index) {
        this.map_slice_index = map_slice_index;
    }

    @Override
    public CreateSlice deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return null;
    }

    @Override
    public JsonElement serialize(CreateSlice createSlice, Type type, JsonSerializationContext jsonSerializationContext) {


        return null;
    }
}
