package ch.epfl.biop.atlastoimg2d.multislice.serializers;

import ch.epfl.biop.atlastoimg2d.multislice.SliceSources;
import com.google.gson.*;

import java.lang.reflect.Type;

public class RegistrationAndSourcesAdapter implements JsonSerializer<SliceSources.RegistrationAndSources>,
        JsonDeserializer<SliceSources.RegistrationAndSources> {

    @Override
    public SliceSources.RegistrationAndSources deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return null;
    }

    @Override
    public JsonElement serialize(SliceSources.RegistrationAndSources registrationAndSources, Type type, JsonSerializationContext jsonSerializationContext) {
        return null;
    }
}
