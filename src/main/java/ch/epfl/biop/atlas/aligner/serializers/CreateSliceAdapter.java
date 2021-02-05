package ch.epfl.biop.atlas.aligner.serializers;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.CreateSlice;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Arrays;

public class CreateSliceAdapter implements JsonSerializer<CreateSlice>,
        JsonDeserializer<CreateSlice> {

    final MultiSlicePositioner mp;

    public CreateSliceAdapter(MultiSlicePositioner mp) {
        this.mp = mp;
    }

    @Override
    public CreateSlice deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        SourceAndConverter[] sacs = jsonDeserializationContext.deserialize(obj.get("original_sources"), SourceAndConverter[].class);
        double location = obj.get("original_location").getAsDouble();
        double thicknessCorrection = obj.get("thicknessCorrection").getAsDouble();
        double zShiftCorrection = obj.get("zShiftCorrection").getAsDouble();
        return new CreateSlice(mp, Arrays.asList(sacs), location,thicknessCorrection, zShiftCorrection);
    }

    @Override
    public JsonElement serialize(CreateSlice createSlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", CreateSlice.class.getSimpleName());
        obj.add("original_sources",jsonSerializationContext.serialize(createSlice.getSlice().getOriginalSources()));
        obj.addProperty("original_location", createSlice.slicingAxisPosition);
        obj.addProperty("thicknessCorrection", createSlice.zSliceThicknessCorrection);
        obj.addProperty("zShiftCorrection", createSlice.zSliceShiftCorrection);
        return obj;
    }
}
