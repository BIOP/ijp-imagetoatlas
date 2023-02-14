package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RasterDeformationAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class RasterDeformationActionAdapter implements JsonSerializer<RasterDeformationAction>,
        JsonDeserializer<RasterDeformationAction> {

    final MultiSlicePositioner mp;
    final Supplier<SliceSources> currentSliceGetter;

    public RasterDeformationActionAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public RasterDeformationAction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        double gridSpacingInMicrometer = obj.get("gridSpacingInMicrometer").getAsDouble();
        return new RasterDeformationAction(mp, currentSliceGetter.get(), gridSpacingInMicrometer);
    }

    @Override
    public JsonElement serialize(RasterDeformationAction rasterDeformation, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", RasterDeformationAction.class.getSimpleName());
        obj.addProperty("gridSpacingInMicrometer", rasterDeformation.getGridSpacingInMicrometer());
        return obj;
    }
}