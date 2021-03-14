package ch.epfl.biop.atlas.aligner.serializers;

import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.sourceandconverter.spline.Elastix2DSplineRegistration;
import com.google.gson.*;
import net.imglib2.realtransform.RealTransform;

import java.lang.reflect.Type;
import java.util.Map;

public class RegistrationAdapter implements JsonSerializer<Registration>,
        JsonDeserializer<Registration> {

    @Override
    public Registration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Registration reg = context.deserialize(json, typeOfT);
        reg.setTransform(json.getAsJsonObject().get("transform").getAsString());
        reg.setRegistrationParameters(context.deserialize(json.getAsJsonObject().get("parameters"), Map.class));
        return reg;
    }

    @Override
    public JsonElement serialize(Registration registration, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", registration.getClass().getSimpleName());
        obj.addProperty("transform", registration.getTransform());
        obj.add("parameters", context.serialize(registration.getRegistrationParameters()));
        return obj;
    }
}
