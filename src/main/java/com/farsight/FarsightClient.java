package com.farsight;

import com.farsight.compat.SodiumCompat;
import com.farsight.preview.PreviewRegionFileManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class FarsightClient implements ClientModInitializer
{

    @Override
    public void onInitializeClient()
    {
        if (FabricLoader.getInstance().isModLoaded("sodium"))
        {
            SodiumCompat.init();
        }

        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> PreviewRegionFileManager.onChunkUnload(chunk.getPos()));
    }
}
