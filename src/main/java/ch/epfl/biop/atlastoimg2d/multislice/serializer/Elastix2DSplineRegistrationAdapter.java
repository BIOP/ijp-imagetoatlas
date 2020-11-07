package ch.epfl.biop.atlastoimg2d.multislice.serializer;

import ch.epfl.biop.registration.sourceandconverter.spline.Elastix2DSplineRegistration;
import com.google.gson.*;
import net.imglib2.realtransform.RealTransform;

import java.lang.reflect.Type;

public class Elastix2DSplineRegistrationAdapter implements JsonSerializer<Elastix2DSplineRegistration>,
        JsonDeserializer<Elastix2DSplineRegistration> {

    @Override
    public Elastix2DSplineRegistration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Elastix2DSplineRegistration registration = new Elastix2DSplineRegistration();
        registration.setRealTransform(context.deserialize(json.getAsJsonObject().get("spline_transform"), RealTransform.class));
        registration.setDone();
        return registration;
    }

    @Override
    public JsonElement serialize(Elastix2DSplineRegistration registration, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", Elastix2DSplineRegistration.class.getSimpleName());
        obj.add("spline_transform", context.serialize(registration.getRealTransform()));
        return obj;
    }
}
