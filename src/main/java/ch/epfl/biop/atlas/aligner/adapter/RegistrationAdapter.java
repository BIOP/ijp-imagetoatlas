package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.registration.plugin.ExternalRegistrationPlugin;
import ch.epfl.biop.registration.Registration;
import com.google.gson.*;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.PluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Map;

public class RegistrationAdapter implements JsonSerializer<Registration>,
        JsonDeserializer<Registration> {

    protected static final Logger logger = LoggerFactory.getLogger(RegistrationAdapter.class);

    final Context scijavacontext;

    public RegistrationAdapter(Context context) {
        this.scijavacontext = context;
    }

    @Override
    public Registration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            Registration registration;

            logger.debug("Fetching registration plugin "+typeOfT.getTypeName());
            if (typeOfT.getTypeName().equals(ExternalRegistrationPlugin.class.getName())) {
                String registrationTypeName = json
                        .getAsJsonObject()
                        .get("external_type")
                        .getAsString();
                logger.debug("Generating registration object, type "+registrationTypeName);
                registration = MultiSlicePositioner
                        .getExternalRegistrationPluginSupplier(registrationTypeName)
                        .get();
            } else {
                logger.debug("Looking in scijava plugins");
                registration=(Registration) scijavacontext.getService(PluginService.class).getPlugin(typeOfT.getTypeName()).createInstance();
            }
            registration.setScijavaContext(scijavacontext);
            registration.setTransform(json.getAsJsonObject().get("transform").getAsString());
            registration.setRegistrationParameters(context.deserialize(json.getAsJsonObject().get("parameters"), Map.class));
            return registration;
        } catch (InstantiableException e) {
            logger.error("Unrecognized registration plugin "+typeOfT.getTypeName());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JsonElement serialize(Registration registration, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        logger.debug("Serializing registration of type "+registration.getClass().getSimpleName()+" which type is named "+registration.getRegistrationTypeName());
        logger.debug("With transform "+registration.getTransform());
        logger.debug("And parameters "+registration.getRegistrationParameters());
        if (MultiSlicePositioner.isExternalRegistrationPlugin(registration.getRegistrationTypeName())) {
            obj.addProperty("type", ExternalRegistrationPlugin.class.getSimpleName());
            obj.addProperty("external_type", registration.getRegistrationTypeName());
        } else {
            obj.addProperty("type", registration.getRegistrationTypeName());
        }
        obj.addProperty("transform", registration.getTransform());
        obj.add("parameters", context.serialize(registration.getRegistrationParameters()));

        return obj;
    }
}
