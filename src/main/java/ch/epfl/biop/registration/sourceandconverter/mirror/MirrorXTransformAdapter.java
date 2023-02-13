package ch.epfl.biop.registration.sourceandconverter.mirror;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import net.imglib2.realtransform.RealTransform;
import org.scijava.plugin.Plugin;
import sc.fiji.persist.IClassRuntimeAdapter;

import java.lang.reflect.Type;
@Plugin(type = IClassRuntimeAdapter.class)
public class MirrorXTransformAdapter implements IClassRuntimeAdapter<RealTransform, MirrorXTransform> {
    @Override
    public Class<? extends RealTransform> getBaseClass() {
        return RealTransform.class;
    }

    @Override
    public Class<? extends MirrorXTransform> getRunTimeClass() {
        return MirrorXTransform.class;
    }

    @Override
    public boolean useCustomAdapter() {
        return true;
    }

    @Override
    public MirrorXTransform deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        double xFactor = obj.getAsJsonPrimitive("xFactor").getAsDouble();
        MirrorXTransform transform = new MirrorXTransform(xFactor);
        return transform;
    }

    @Override
    public JsonElement serialize(MirrorXTransform transform, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        obj.addProperty("xFactor", transform.xFactor);
        return obj;
    }
}
