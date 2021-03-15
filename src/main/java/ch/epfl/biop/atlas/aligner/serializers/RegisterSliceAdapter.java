package ch.epfl.biop.atlas.aligner.serializers;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSlice;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.sourcepreprocessors.SourcesProcessor;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.sourceandconverter.spline.Elastix2DSplineRegistration;
import com.google.gson.*;

import java.lang.reflect.Type;
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
        Registration<SourceAndConverter<?>[]> reg = jsonDeserializationContext.deserialize(obj.get("registration"), Registration.class); // isDone should be true when deserialized

        if (reg instanceof Elastix2DSplineRegistration) {
            ((Elastix2DSplineRegistration)reg).setZPositioner(currentSliceGetter.get()::getZAxisPosition);
        }

        SourcesProcessor fixed_sources_preprocess = jsonDeserializationContext.deserialize(obj.get("fixed_sources_preprocess"), SourcesProcessor.class);
        SourcesProcessor moving_souces_preprocess = jsonDeserializationContext.deserialize(obj.get("moving_sources_preprocess"), SourcesProcessor.class);

        RegisterSlice registerSlice = new RegisterSlice(mp, currentSliceGetter.get(), reg, fixed_sources_preprocess, moving_souces_preprocess);

        registerSlice.setRegistration(reg);
        return registerSlice;
    }

    @Override
    public JsonElement serialize(RegisterSlice regSlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        System.out.println("Register RegisterSlice action start");
        obj.addProperty("type", RegisterSlice.class.getSimpleName());

        System.out.println("0");
        obj.add("fixed_sources_preprocess", jsonSerializationContext.serialize(regSlice.getFixedSourcesProcessor()));

        System.out.println("1");
        obj.add("moving_sources_preprocess", jsonSerializationContext.serialize(regSlice.getMovingSourcesProcessor()));

        System.out.println("2");
        obj.add("registration", jsonSerializationContext.serialize(regSlice.getRegistration()));
        System.out.println("Register RegisterSlice action end");
        return obj;
    }
}
