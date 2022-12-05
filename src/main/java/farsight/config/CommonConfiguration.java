package farsight.config;

import com.google.gson.JsonObject;
import farsight.FarsightMod;

public class CommonConfiguration
{
    public int maxRenderDistance = 64;

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry2 = new JsonObject();
        entry2.addProperty("desc:", "Maximum allowed render distance, default 64");
        entry2.addProperty("maxRenderDistance", maxRenderDistance);
        root.add("maxRenderDistance", entry2);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        if (data == null)
        {
            FarsightMod.LOGGER.error("Config file was empty!");
            return;
        }

        maxRenderDistance = data.get("maxRenderDistance").getAsJsonObject().get("maxRenderDistance").getAsInt();
    }
}
