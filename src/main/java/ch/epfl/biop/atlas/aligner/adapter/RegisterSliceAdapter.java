package ch.epfl.biop.atlas.aligner.adapter;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.SourcesZOffset;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.source.processor.SourcesIdentity;
import ch.epfl.biop.source.processor.SourcesProcessor;
import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This adapter only saves the transform, not the preprocessing of the source and the atlas
 */

public class RegisterSliceAdapter implements JsonSerializer<RegisterSliceAction>,
        JsonDeserializer<RegisterSliceAction> {

    protected static final Logger logger = LoggerFactory.getLogger(RegisterSliceAdapter.class);

    final MultiSlicePositioner mp;
    final Supplier<SliceSources> currentSliceGetter;

    public RegisterSliceAdapter(MultiSlicePositioner mp, Supplier<SliceSources> sliceGetter) {
        this.mp = mp;
        this.currentSliceGetter = sliceGetter;
    }

    @Override
    public RegisterSliceAction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        Registration<SourceAndConverter<?>[]> reg = jsonDeserializationContext.deserialize(obj.get("registration"), Registration.class); // done if a transform is serialized

        // A missing preprocessing means no preprocessing
        SourcesProcessor fixed_sources_preprocess = obj.has("fixed_sources_preprocess") ?
                jsonDeserializationContext.deserialize(obj.get("fixed_sources_preprocess"), SourcesProcessor.class) : new SourcesIdentity();
        SourcesProcessor moving_sources_preprocess = obj.has("moving_sources_preprocess") ?
                jsonDeserializationContext.deserialize(obj.get("moving_sources_preprocess"), SourcesProcessor.class) : new SourcesIdentity();

        SliceSources slice = currentSliceGetter.get();
        if ((reg != null) && (!reg.isRegistrationDone())) {
            // The registration will be computed: as in MultiSlicePositioner#registerSlices, it runs at the
            // position of the slice it is deserialized on, within the registration ROI unless specified otherwise
            fixed_sources_preprocess = SourcesZOffset.replaceIn(fixed_sources_preprocess, slice);
            moving_sources_preprocess = SourcesZOffset.replaceIn(moving_sources_preprocess, slice);
            Map<String, String> parameters = new HashMap<>(reg.getRegistrationParameters());
            double[] roi = mp.getROI();
            parameters.putIfAbsent("px", String.valueOf(roi[0]));
            parameters.putIfAbsent("py", String.valueOf(roi[1]));
            parameters.putIfAbsent("sx", String.valueOf(roi[2]));
            parameters.putIfAbsent("sy", String.valueOf(roi[3]));
            reg.setRegistrationParameters(parameters);
        }

        RegisterSliceAction registerSlice = new RegisterSliceAction(mp, slice, reg, fixed_sources_preprocess, moving_sources_preprocess);

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
