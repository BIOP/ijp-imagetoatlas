package ch.epfl.biop.atlas.aligner.adapter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessor;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.function.Supplier;

/**
 * This adapter only saves the transform, not the preprocessing of the source and the atlas
 * Function.identity is returned instead of the original preprocessing.
 */

public class RegisterSliceAdapter implements JsonSerializer<RegisterSliceAction>,
        JsonDeserializer<RegisterSliceAction> {

    protected static Logger logger = LoggerFactory.getLogger(RegisterSliceAdapter.class);

    final MultiSlicePositioner mp;
    Supplier<SliceSources> currentSliceGetter;

    public RegisterSliceAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public RegisterSliceAction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        Registration<SourceAndConverter<?>[]> reg = jsonDeserializationContext.deserialize(obj.get("registration"), Registration.class); // isDone should be true when deserialized

        SourcesProcessor fixed_sources_preprocess = jsonDeserializationContext.deserialize(obj.get("fixed_sources_preprocess"), SourcesProcessor.class);
        SourcesProcessor moving_sources_preprocess = jsonDeserializationContext.deserialize(obj.get("moving_sources_preprocess"), SourcesProcessor.class);

        RegisterSliceAction registerSlice = new RegisterSliceAction(mp, currentSliceGetter.get(), reg, fixed_sources_preprocess, moving_sources_preprocess);

        registerSlice.setRegistration(reg);
        return registerSlice;
    }

    @Override
    public JsonElement serialize(RegisterSliceAction regSlice, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject obj = new JsonObject();
        logger.debug("RegisterSlice serialization start, class "+ RegisterSliceAction.class.getSimpleName());
        obj.addProperty("type", RegisterSliceAction.class.getSimpleName());

        logger.debug("Serializing moving sources preprocessing");
        obj.add("fixed_sources_preprocess", jsonSerializationContext.serialize(regSlice.getFixedSourcesProcessor()));

        logger.debug("Serializing fixed sources preprocessing");
        obj.add("moving_sources_preprocess", jsonSerializationContext.serialize(regSlice.getMovingSourcesProcessor()));

        logger.debug("Serializing registration");
        obj.add("registration", jsonSerializationContext.serialize(regSlice.getRegistration()));

        logger.debug("RegisterSlice serialization end");
        return obj;
    }
}
