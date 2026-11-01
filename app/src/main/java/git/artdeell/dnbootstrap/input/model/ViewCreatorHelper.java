package git.artdeell.dnbootstrap.input.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class ViewCreatorHelper implements JsonDeserializer<ViewCreator>, JsonSerializer<ViewCreator> {
    @Override
    public ViewCreator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        switch (type) {
            case ControlButtonData.TYPE: return context.deserialize(json, ControlButtonData.class);
            case JoystickData.TYPE: return context.deserialize(json, JoystickData.class);
            default: throw new JsonParseException("Unknown ViewCreator type: "+type);
        }
    }

    @Override
    public JsonElement serialize(ViewCreator src, Type typeOfSrc, JsonSerializationContext context) {
        JsonElement jsonElement = context.serialize(src);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        jsonObject.addProperty("type", src.getType());
        return jsonObject;
    }
}
