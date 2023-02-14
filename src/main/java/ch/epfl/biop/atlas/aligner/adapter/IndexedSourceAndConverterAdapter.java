package ch.epfl.biop.atlas.aligner.adapter;

import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

public class IndexedSourceAndConverterAdapter implements JsonSerializer<SourceAndConverter>,
        JsonDeserializer<SourceAndConverter> {

    protected static final Logger logger = LoggerFactory.getLogger(IndexedSourceAndConverterAdapter.class);

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
        logger.debug("sac.getSpimSource().getName():"+sac.getSpimSource().getName());
        logger.debug("size list = "+list_sacs.size());
        obj.addProperty("source_index", list_sacs.indexOf(sac));
        return obj;
    }
}
