package ch.epfl.biop.bdv.sourcepreprocessor.adapter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.bdv.sourcepreprocessor.SourcesResampler;
import com.google.gson.*;

import java.lang.reflect.Type;

public class SourcesResamplerAdapter implements JsonSerializer<SourcesResampler>,
        JsonDeserializer<SourcesResampler> {

    @Override
    public SourcesResampler deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        SourceAndConverter model = context.deserialize(json.getAsJsonObject().get("model"), SourceAndConverter.class);
        return new SourcesResampler(model);
    }

    @Override
    public JsonElement serialize(SourcesResampler sr, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", SourcesResampler.class.getSimpleName());
        obj.add("model", context.serialize(sr.model));
        return obj;
    }
}
