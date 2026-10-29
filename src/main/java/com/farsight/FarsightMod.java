package com.farsight;

import com.cupboard.config.CupboardConfig;
import com.farsight.compat.SodiumCompat;
import com.farsight.config.CommonConfiguration;
import com.farsight.preview.PreviewRegionFileManager;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
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

    public FarsightMod()
    {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "", (c, b) -> true));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().addListener(this::chunkUnload);
    }

    private void chunkUnload(ChunkEvent.Unload chunkEvent)
    {
        if (chunkEvent.getLevel().isClientSide())
        {
            PreviewRegionFileManager.onChunkUnload(chunkEvent.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event)
    {
        // Side safe client event handler
        if ((FMLLoader.getLoadingModList().getModFileById("rubidium") != null) || (
          FMLLoader.getLoadingModList().getModFileById("sodium") != null))
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
