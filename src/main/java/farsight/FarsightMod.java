package farsight;

import com.cupboard.config.CupboardConfig;
import farsight.compat.SodiumCompat;
import farsight.config.CommonConfiguration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FarsightMod implements ModInitializer
{
    public static final String                              MODID  = "farsight";
    public static final Logger                              LOGGER = LogManager.getLogger();
    public static       CupboardConfig<CommonConfiguration> config = new CupboardConfig<>(MODID, new CommonConfiguration());

    @Override
    public void onInitialize()
    {
        LOGGER.info(MODID + " mod initialized");

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
