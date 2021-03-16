package ch.epfl.biop.atlas.aligner.serializers;

import ch.epfl.biop.registration.Registration;
import com.google.gson.*;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.PluginService;

import java.lang.reflect.Type;
import java.util.Map;

public class RegistrationAdapter implements JsonSerializer<Registration>,
        JsonDeserializer<Registration> {

    Context scijavacontext;

    public RegistrationAdapter(Context context) {
        this.scijavacontext = context;
    }

    @Override
    public Registration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            Registration registration = (Registration) scijavacontext.getService(PluginService.class)
            .getPlugin(typeOfT.getTypeName()).createInstance();
            registration.setScijavaContext(scijavacontext);
            registration.setTransform(json.getAsJsonObject().get("transform").getAsString());
            registration.setRegistrationParameters(context.deserialize(json.getAsJsonObject().get("parameters"), Map.class));
            return registration;
        } catch (InstantiableException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JsonElement serialize(Registration registration, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        /*System.out.println(registration.getClass().getSimpleName());
        System.out.println(registration.getTransform());
        System.out.println(registration.getRegistrationParameters());
        System.out.println(context.serialize(registration.getRegistrationParameters()));*/

        obj.addProperty("type", registration.getClass().getSimpleName());
        obj.addProperty("transform", registration.getTransform());
        obj.add("parameters", context.serialize(registration.getRegistrationParameters()));

        return obj;
    }
}
