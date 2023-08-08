package farsight.compat;

import farsight.FarsightClientChunkManager;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;

public class SodiumCompat
{
    public static void init()
    {
        FarsightClientChunkManager.loadCallback.add(((clientLevel, levelChunk) -> ChunkTrackerHolder.get(clientLevel)
          .onChunkStatusAdded(levelChunk.getPos().x, levelChunk.getPos().z, 1)));
        FarsightClientChunkManager.unloadCallback.add(((clientLevel, levelChunk) -> ChunkTrackerHolder.get(clientLevel)
          .onChunkStatusRemoved(levelChunk.getPos().x, levelChunk.getPos().z, 1)));
    }
}
