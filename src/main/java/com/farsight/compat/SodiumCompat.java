package com.farsight.compat;

import com.farsight.FarsightClientChunkManager;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkStatus;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;

public class SodiumCompat
{
    public static void init()
    {
        FarsightClientChunkManager.loadCallback.add(((clientLevel, levelChunk) -> ChunkTrackerHolder.get(clientLevel)
          .onChunkStatusAdded(levelChunk.getPos().x, levelChunk.getPos().z, ChunkStatus.FLAG_HAS_BLOCK_DATA)));
        FarsightClientChunkManager.unloadCallback.add(((clientLevel, levelChunk) -> ChunkTrackerHolder.get(clientLevel)
          .onChunkStatusRemoved(levelChunk.getPos().x, levelChunk.getPos().z, ChunkStatus.FLAG_HAS_BLOCK_DATA)));
    }
}
