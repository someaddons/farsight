package com.farsight.compat;

import com.farsight.FarsightClientChunkManager;
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkStatus;
import net.caffeinemc.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;

public class SodiumCompat
{
    public static void init()
    {
        FarsightClientChunkManager.loadCallback.add(((clientLevel, levelChunk) -> ChunkTrackerHolder.get(clientLevel)
            .onChunkStatusAdded(levelChunk.getPos().x, levelChunk.getPos().z, ChunkStatus.FLAG_ALL)));
        FarsightClientChunkManager.unloadCallback.add(((clientLevel, levelChunk) -> ChunkTrackerHolder.get(clientLevel)
            .onChunkStatusRemoved(levelChunk.getPos().x, levelChunk.getPos().z, ChunkStatus.FLAG_ALL)));
    }
}
