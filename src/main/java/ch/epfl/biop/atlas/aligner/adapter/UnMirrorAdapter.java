package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.UnMirrorSliceAction;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class UnMirrorAdapter implements JsonSerializer<UnMirrorSliceAction>,
        JsonDeserializer<UnMirrorSliceAction> {

    final MultiSlicePositioner mp;
    Supplier<SliceSources> currentSliceGetter;

    public UnMirrorAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public UnMirrorSliceAction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new UnMirrorSliceAction(mp, currentSliceGetter.get());
    }

    @Override
    public JsonElement serialize(UnMirrorSliceAction unMirrorSliceAction, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", UnMirrorSliceAction.class.getSimpleName());
        return obj;
    }
}
