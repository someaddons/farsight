package farsight.config;

import com.google.gson.JsonObject;
import farsight.FarsightMod;

public class CommonConfiguration
{
    public int maxchunkdist = 32;

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry = new JsonObject();
        entry.addProperty("desc:", "The range at which chunks are kept loaded on the clients memory, regardless of server chunk view distance."
                                     + " default:32, min 1, max 128");
        entry.addProperty("maxchunkdist", maxchunkdist);
        root.add("maxchunkdist", entry);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        if (data == null)
        {
            FarsightMod.LOGGER.error("Config file was empty!");
            return;
        }

        try
        {
            maxchunkdist = data.get("maxchunkdist").getAsJsonObject().get("maxchunkdist").getAsInt();
        }
        catch (Exception e)
        {
            FarsightMod.LOGGER.error("Could not parse config file", e);
        }
    }
}
