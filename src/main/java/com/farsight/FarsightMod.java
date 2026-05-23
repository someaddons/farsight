package com.farsight;

import com.cupboard.config.CupboardConfig;
import com.farsight.compat.SodiumCompat;
import com.farsight.config.CommonConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(FarsightMod.MODID)
public class FarsightMod
{
    public static final String                              MODID  = "farsight_view";
    public static final Logger                              LOGGER = LogManager.getLogger();
    public static       CupboardConfig<CommonConfiguration> config = new CupboardConfig<>("farsight", new CommonConfiguration());
    public static       Random                              rand   = new Random();

    public FarsightMod(IEventBus modEventBus, ModContainer modContainer)
    {
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::clientSetup);
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event)
    {
        // Side safe client event handler
        FarsightClientChunkManager.unloadCallback.add((level, levelChunk) -> {
            NeoForge.EVENT_BUS.post(new ChunkEvent.Unload(levelChunk));
        });

        FarsightClientChunkManager.loadCallback.add((level, levelChunk) -> {
            NeoForge.EVENT_BUS.post(new ChunkEvent.Load(levelChunk, false));
        });

        if ((FMLLoader.getCurrent().getLoadingModList().getModFileById("rubidium") != null) || (
          FMLLoader.getCurrent().getLoadingModList().getModFileById("sodium") != null))
        {
            SodiumCompat.init();
        }
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.info(MODID + " mod initialized");
    }

    public static void logDebug(String message, Object... args)
    {
        if (config.getCommonConfig().debugLogging)
        {
            LOGGER.warn(message, args);
        }
    }
}
