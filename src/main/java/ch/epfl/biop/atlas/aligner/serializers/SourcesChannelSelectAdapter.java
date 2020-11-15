package ch.epfl.biop.atlas.aligner.serializers;

import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesChannelsSelect;
import ch.epfl.biop.registration.sourceandconverter.affine.Elastix2DAffineRegistration;
import ch.epfl.biop.registration.sourceandconverter.spline.Elastix2DSplineRegistration;
import com.google.gson.*;
import net.imglib2.realtransform.AffineTransform3D;

import java.lang.reflect.Type;
import java.util.List;

public class SourcesChannelSelectAdapter implements JsonSerializer<SourcesChannelsSelect>,
        JsonDeserializer<SourcesChannelsSelect> {

    @Override
    public SourcesChannelsSelect deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Integer[] channels_indices = context.deserialize(json.getAsJsonObject().get("channels_indices"), Integer[].class);
        return new SourcesChannelsSelect(channels_indices);
    }

    @Override
    public JsonElement serialize(SourcesChannelsSelect scs, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", SourcesChannelsSelect.class.getSimpleName());
        obj.add("channels_indices", context.serialize(scs.channels_indices.toArray(new Integer[0]), Integer[].class));
        return obj;
    }
}
