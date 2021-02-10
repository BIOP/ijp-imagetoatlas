package ch.epfl.biop.atlas.aligner.serializers;

import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessComposer;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessor;
import com.google.gson.*;

import java.lang.reflect.Type;

public class SourcesComposerAdapter implements JsonSerializer<SourcesProcessComposer>,
        JsonDeserializer<SourcesProcessComposer> {

    @Override
    public SourcesProcessComposer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        SourcesProcessor sp1 = context.deserialize(json.getAsJsonObject().get("f1"), SourcesProcessor.class);
        SourcesProcessor sp2 = context.deserialize(json.getAsJsonObject().get("f2"), SourcesProcessor.class);
        return new SourcesProcessComposer(sp2,sp1);
    }

    @Override
    public JsonElement serialize(SourcesProcessComposer spc, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", SourcesProcessComposer.class.getSimpleName());
        obj.add("f1", context.serialize(spc.f1));
        obj.add("f2", context.serialize(spc.f2));
        return obj;
    }
}
