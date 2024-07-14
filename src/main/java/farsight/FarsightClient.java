package farsight;

import farsight.compat.SodiumCompat;
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
        FarsightMod.LOGGER.info(FarsightMod.MODID + " mod initialized");
        if (FabricLoader.getInstance().isModLoaded("sodium"))
        {
            SodiumCompat.init();
        }

        FarsightClientChunkManager.unloadCallback.add((level, levelChunk) -> {
            ClientChunkEvents.CHUNK_UNLOAD.invoker().onChunkUnload(level, levelChunk);
        });

        FarsightClientChunkManager.loadCallback.add((level, levelChunk) -> {
            ClientChunkEvents.CHUNK_LOAD.invoker().onChunkLoad(level, levelChunk);
        });
    }
}
