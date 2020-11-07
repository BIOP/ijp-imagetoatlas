package ch.epfl.biop.atlastoimg2d.multislice.serializer;

import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import ch.epfl.biop.atlastoimg2d.multislice.RegisterSlice;
import ch.epfl.biop.atlastoimg2d.multislice.SliceSources;
import ch.epfl.biop.registration.Registration;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This adapter only saves the transform, not the preprocessing of the source and the atlas
 * Function.identity is returned instead of the original preprocessing.
 */

public class RegisterSliceAdapter implements JsonSerializer<RegisterSlice>,
        JsonDeserializer<RegisterSlice> {

    final MultiSlicePositioner mp;
    Supplier<SliceSources> currentSliceGetter;

    public RegisterSliceAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public RegisterSlice deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        RegisterSlice registerSlice = new RegisterSlice(mp, currentSliceGetter.get(), null, Function.identity(), Function.identity());
        Registration reg = jsonDeserializationContext.deserialize(obj.get("registration"), Registration.class); // isDone should be true when deserialized
        registerSlice.setRegistration(reg);
        return registerSlice;
    }

    @Override
    public JsonElement serialize(RegisterSlice regSlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", RegisterSlice.class.getSimpleName());
        obj.add("registration", jsonSerializationContext.serialize(regSlice.getRegistration()));
        return obj;
    }
}
