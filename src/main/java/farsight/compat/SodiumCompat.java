package farsight.compat;

import farsight.FarsightClientChunkManager;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;

public class SodiumCompat
{
    public static void init()
    {
        FarsightClientChunkManager.unloadCallback.add(chunk -> SodiumWorldRenderer.instance().onChunkRemoved(chunk.getPos().x, chunk.getPos().z));
    }
}
