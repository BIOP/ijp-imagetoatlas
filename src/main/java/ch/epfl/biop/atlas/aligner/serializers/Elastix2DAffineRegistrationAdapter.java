package ch.epfl.biop.atlas.aligner.serializers;

import ch.epfl.biop.registration.sourceandconverter.affine.Elastix2DAffineRegistration;
import com.google.gson.*;
import net.imglib2.realtransform.AffineTransform3D;

import java.lang.reflect.Type;

public class Elastix2DAffineRegistrationAdapter implements JsonSerializer<Elastix2DAffineRegistration>,
        JsonDeserializer<Elastix2DAffineRegistration> {

    @Override
    public Elastix2DAffineRegistration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Elastix2DAffineRegistration registration = new Elastix2DAffineRegistration();
        registration.setTransform(context.deserialize(json.getAsJsonObject().get("affine_transform"), AffineTransform3D.class));
        //registration.setDone();
        return registration;
    }

    @Override
    public JsonElement serialize(Elastix2DAffineRegistration registration, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", Elastix2DAffineRegistration.class.getSimpleName());
        //obj.add("affine_transform", context.serialize(registration.getAffineTransform(), AffineTransform3D.class));
        obj.addProperty("transform",
                //        context.serialize(registration.getAffineTransform(), AffineTransform3D.class)
                registration.getTransform()
        );
        return obj;
    }
}
