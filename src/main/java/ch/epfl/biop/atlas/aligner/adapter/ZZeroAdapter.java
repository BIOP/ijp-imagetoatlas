package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.sourceandconverter.processor.SourcesAffineTransformer;
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

public class ZZeroAdapter implements JsonSerializer<MultiSlicePositioner.ZZero>,
        JsonDeserializer<MultiSlicePositioner.ZZero> {

    @Override
    public MultiSlicePositioner.ZZero deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        AffineTransform3D at3d = context.deserialize(json.getAsJsonObject().get("affine_transform"), RealTransform.class);
        return new MultiSlicePositioner.ZZero(at3d);
    }

    @Override
    public JsonElement serialize(MultiSlicePositioner.ZZero sat, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", MultiSlicePositioner.ZZero.class.getSimpleName());
        obj.add("affine_transform", context.serialize(sat.at3d, RealTransform.class));
        return obj;
    }
}