package ch.epfl.biop.atlas.aligner.adapter;

import ch.epfl.biop.atlas.aligner.MoveSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.function.Supplier;

public class MoveSliceAdapter implements JsonSerializer<MoveSliceAction>,
        JsonDeserializer<MoveSliceAction> {

    final MultiSlicePositioner mp;
    final Supplier<SliceSources> currentSliceGetter;

    public MoveSliceAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public MoveSliceAction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        // 'z_mm' is the position in the atlas convention, written by scripts; state files store 'location'
        if (obj.has("z_mm")) {
            return new MoveSliceAction(mp, currentSliceGetter.get(), mp.fromAtlasZ(obj.get("z_mm").getAsDouble()));
        }
        if (!obj.has("location")) {
            throw new JsonParseException("MoveSliceAction requires a 'z_mm': the position along the slicing axis, "
                    + "in mm, 0 at the first section of the atlas");
        }
        return new MoveSliceAction(mp, currentSliceGetter.get(), obj.get("location").getAsDouble());
    }

    @Override
    public JsonElement serialize(MoveSliceAction moveSlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", MoveSliceAction.class.getSimpleName());
        obj.addProperty("z_mm", mp.toAtlasZ(moveSlice.getSlicingAxisPosition()));
        obj.addProperty("location", moveSlice.getSlicingAxisPosition());
        return obj;
    }
}
