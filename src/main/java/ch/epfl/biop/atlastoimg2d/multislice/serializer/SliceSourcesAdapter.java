package ch.epfl.biop.atlastoimg2d.multislice.serializer;

import ch.epfl.biop.atlastoimg2d.multislice.*;
import com.google.gson.*;
import sc.fiji.bdvpg.services.SourceAndConverterSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SliceSourcesAdapter implements JsonSerializer<SliceSources>,
        JsonDeserializer<SliceSources> {

    final SourceAndConverterSerializer sacss;
    final MultiSlicePositioner mp;

    public SliceSourcesAdapter(SourceAndConverterSerializer sacss, MultiSlicePositioner mp) {
        this.sacss = sacss;
        this.mp = mp;
    }

    @Override
    public SliceSources deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return null;
    }

    @Override
    public JsonElement serialize(SliceSources sliceSources, Type type, JsonSerializationContext jsonSerializationContext) {



        /*List<CancelableAction> actions = mp.mso.getActionsFromSlice(sliceSources);
        actions = actions.stream().filter(action ->
                        ((action instanceof RegisterSlice)||
                         (action instanceof MoveSlice)||
                         (action instanceof CreateSlice)||
                         (action instanceof DeleteSlice))||
                         (action instanceof DeleteLastRegistration))
                .collect(Collectors.toList());

        List<CancelableAction> actionsToSave = new ArrayList<>();

        CancelableAction lastRegistration;

        for (int i = 0; i<actions.size();i++) {
            CancelableAction a = actions.get(i);
            if (a instanceof RegisterSlice) lastRegistration = a;
            if (a instanceof DeleteLastRegistration) {
                actionsToSave.remove(a);
            } else {
                actionsToSave.add(a);
            }
        }*/



        return null;
    }
}
