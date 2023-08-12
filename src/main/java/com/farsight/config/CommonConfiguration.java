package com.farsight.config;

import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonObject;

public class CommonConfiguration implements ICommonConfig
{
    public int maxchunkdist = 32;

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry2 = new JsonObject();
        entry2.addProperty("desc:", "The distance at which chunks are kept in memory, regardless of whether the server unloads them. default = 32, maximum = 512");
        entry2.addProperty("maxchunkdist", maxchunkdist);
        root.add("maxchunkdist", entry2);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        maxchunkdist = data.get("maxchunkdist").getAsJsonObject().get("maxchunkdist").getAsInt();
    }
}
