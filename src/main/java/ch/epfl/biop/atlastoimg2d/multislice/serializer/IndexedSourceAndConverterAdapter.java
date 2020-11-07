package ch.epfl.biop.atlastoimg2d.multislice.serializer;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlastoimg2d.multislice.SliceSources;
import com.google.gson.*;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.List;

public class IndexedSourceAndConverterAdapter implements JsonSerializer<SourceAndConverter>,
        JsonDeserializer<SourceAndConverter> {

    final List<SourceAndConverter> list_sacs;

    public IndexedSourceAndConverterAdapter(List<SourceAndConverter> list_sacs) {
        this.list_sacs = list_sacs;
    }

    @Override
    public SourceAndConverter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        return list_sacs.get(obj.get("source_index").getAsInt());
    }

    @Override
    public JsonElement serialize(SourceAndConverter sac, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        System.out.println("sac.getSpimSource().getName():"+sac.getSpimSource().getName());
        System.out.println("size list = "+list_sacs.size());
        obj.addProperty("source_index", list_sacs.indexOf(sac));
        return obj;
    }
}
