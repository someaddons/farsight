package com.farsight.compat;

import com.farsight.FarsightClientChunkManager;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;

public class SodiumCompat
{
    public static void init()
    {
        FarsightClientChunkManager.loadCallback.add(((clientLevel, levelChunk) -> SodiumWorldRenderer.instance()
          .onChunkAdded(levelChunk.getPos().x, levelChunk.getPos().z)));
        FarsightClientChunkManager.unloadCallback.add(((clientLevel, levelChunk) -> SodiumWorldRenderer.instance()
          .onChunkRemoved(levelChunk.getPos().x, levelChunk.getPos().z)));
    }
}
