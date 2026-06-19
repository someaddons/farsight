package com.farsight.config;

import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonObject;

public class CommonConfiguration implements ICommonConfig
{
    public int     maxRenderDistance     = 64;
    public boolean debugLogging          = false;
    public boolean enableChunkPreview    = true;
    public int     previewChunkLoadSpeed = 1;

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry2 = new JsonObject();
        entry2.addProperty("desc:", "Maximum allowed render distance, changes require a game restart. default 64");
        entry2.addProperty("maxRenderDistance", maxRenderDistance);
        root.add("maxRenderDistance", entry2);

        final JsonObject cat1 = new JsonObject();
        cat1.addProperty("descEnable:", "Enables chunk preview feature, which enables saving and loading previously visited chunks to/from disk, default false");
        cat1.addProperty("enabled", enableChunkPreview);
        cat1.addProperty("descSpeed:", "Set the speed at which chunk preview loads chunks from disk, default and minimum 1 Chunk per tick(20 per second)");
        cat1.addProperty("previewChunkLoadSpeed", previewChunkLoadSpeed);
        root.add("chunkPreview", cat1);

        final JsonObject entry4 = new JsonObject();
        entry4.addProperty("desc:", "Enables debug logging to latest.log, default false");
        entry4.addProperty("debugLogging", debugLogging);
        root.add("debugLogging", entry4);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        maxRenderDistance = data.get("maxRenderDistance").getAsJsonObject().get("maxRenderDistance").getAsInt();
        debugLogging = data.get("debugLogging").getAsJsonObject().get("debugLogging").getAsBoolean();
        enableChunkPreview = data.get("chunkPreview").getAsJsonObject().get("enabled").getAsBoolean();
        previewChunkLoadSpeed = data.get("chunkPreview").getAsJsonObject().get("previewChunkLoadSpeed").getAsInt();
    }
}
