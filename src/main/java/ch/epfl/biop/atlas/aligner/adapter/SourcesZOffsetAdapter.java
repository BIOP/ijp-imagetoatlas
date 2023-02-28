package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.SourcesZOffset;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealTransform;

import java.lang.reflect.Type;

public class SourcesZOffsetAdapter implements JsonSerializer<SourcesZOffset>,
        JsonDeserializer<SourcesZOffset> {

    @Override
    public SourcesZOffset deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        AffineTransform3D at3d = context.deserialize(json.getAsJsonObject().get("affine_transform"), RealTransform.class);
        return new SourcesZOffset(at3d);
    }

    @Override
    public JsonElement serialize(SourcesZOffset sat, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", SourcesZOffset.class.getSimpleName());
        obj.add("affine_transform", context.serialize(sat.at3d, RealTransform.class));
        return obj;
    }
}