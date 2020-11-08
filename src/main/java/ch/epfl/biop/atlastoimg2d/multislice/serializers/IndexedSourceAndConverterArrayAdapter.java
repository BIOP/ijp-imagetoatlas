package ch.epfl.biop.atlastoimg2d.multislice.serializers;

import bdv.viewer.SourceAndConverter;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

public class IndexedSourceAndConverterArrayAdapter implements JsonSerializer<SourceAndConverter[]>,
        JsonDeserializer<SourceAndConverter[]> {

    final List<SourceAndConverter> list_sacs;

    public IndexedSourceAndConverterArrayAdapter(List<SourceAndConverter> list_sacs) {
        this.list_sacs = list_sacs;
    }

    @Override
    public SourceAndConverter[] deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        int[] indexes = jsonDeserializationContext.deserialize(obj.get("source_indexes"), int[].class);
        SourceAndConverter[] sacs = new SourceAndConverter[indexes.length];
        for (int i=0;i<sacs.length;i++) {
            sacs[i] = list_sacs.get(indexes[i]);
        }
        return sacs;
    }

    @Override
    public JsonElement serialize(SourceAndConverter[] sacs, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        int[] indexes = new int[sacs.length];
        for (int i=0;i<sacs.length;i++) {
            indexes[i] = list_sacs.indexOf(sacs[i]);
        }
        obj.add("source_indexes", jsonSerializationContext.serialize(indexes));
        return obj;
    }
}
