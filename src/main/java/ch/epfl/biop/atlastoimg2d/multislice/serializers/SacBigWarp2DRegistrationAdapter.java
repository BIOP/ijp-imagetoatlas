package ch.epfl.biop.atlastoimg2d.multislice.serializers;

import ch.epfl.biop.registration.sourceandconverter.spline.SacBigWarp2DRegistration;
import com.google.gson.*;
import net.imglib2.realtransform.RealTransform;

import java.lang.reflect.Type;

public class SacBigWarp2DRegistrationAdapter implements JsonSerializer<SacBigWarp2DRegistration>,
        JsonDeserializer<SacBigWarp2DRegistration> {

    @Override
    public SacBigWarp2DRegistration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        SacBigWarp2DRegistration registration = new SacBigWarp2DRegistration();
        registration.setRealTransform(context.deserialize(json.getAsJsonObject().get("spline_transform"), RealTransform.class));
        registration.setDone();
        return registration;
    }

    @Override
    public JsonElement serialize(SacBigWarp2DRegistration registration, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", SacBigWarp2DRegistration.class.getSimpleName());
        obj.add("spline_transform", context.serialize(registration.getRealTransform()));
        return obj;
    }
}
