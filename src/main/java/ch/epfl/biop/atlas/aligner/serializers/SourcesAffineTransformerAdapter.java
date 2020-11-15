package ch.epfl.biop.atlas.aligner.serializers;

import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesAffineTransformer;
import com.google.gson.*;
import net.imglib2.realtransform.AffineTransform3D;

import java.lang.reflect.Type;

public class SourcesAffineTransformerAdapter implements JsonSerializer<SourcesAffineTransformer>,
        JsonDeserializer<SourcesAffineTransformer> {

    @Override
    public SourcesAffineTransformer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        AffineTransform3D at3d = context.deserialize(json.getAsJsonObject().get("affine_transform"), AffineTransform3D.class);
        return new SourcesAffineTransformer(at3d);
    }

    @Override
    public JsonElement serialize(SourcesAffineTransformer sat, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", SourcesAffineTransformer.class.getSimpleName());
        obj.add("affine_transform", context.serialize(sat.at3d));
        return obj;
    }
}
