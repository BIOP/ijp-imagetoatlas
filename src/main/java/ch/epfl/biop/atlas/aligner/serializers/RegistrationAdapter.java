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
        System.out.println("Deserialize registration of type : "+typeOfT.getTypeName());

        Registration registration = null;
        try {
            registration = (Registration) scijavacontext.getService(PluginService.class)
            .getPlugin(typeOfT.getTypeName()).createInstance();
            registration.setScijavaContext(scijavacontext);
        } catch (InstantiableException e) {
            e.printStackTrace();
            return null;
        }

        System.out.println(json.getAsJsonObject().get("transform").getAsString());
        registration.setTransform(json.getAsJsonObject().get("transform").getAsString());

        registration.setRegistrationParameters(context.deserialize(json.getAsJsonObject().get("parameters"), Map.class));

        System.out.println("Serializing : "+registration.getClass().getSimpleName());
        System.out.println("Transform"+ registration.getTransform());
        System.out.println("Parameters : "+ registration.getRegistrationParameters());

        return registration;
    }

    @Override
    public JsonElement serialize(Registration registration, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", registration.getClass().getSimpleName());
        obj.addProperty("transform", registration.getTransform());
        obj.add("parameters", context.serialize(registration.getRegistrationParameters()));

        System.out.println("Serializing : "+registration.getClass().getSimpleName());
        System.out.println("Transform : "+ registration.getTransform());
        System.out.println("Parameters : "+ registration.getRegistrationParameters());
        return obj;
    }
}
