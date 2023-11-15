package com.farsight;

import com.cupboard.config.CupboardConfig;
import com.farsight.compat.SodiumCompat;
import com.farsight.config.CommonConfiguration;
import com.farsight.event.ClientEventHandler;
import com.farsight.event.EventHandler;
import com.farsight.event.ModEventHandler;
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
        Mod.EventBusSubscriber.Bus.MOD.bus().get().register(ModEventHandler.class);
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(EventHandler.class);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event)
    {
        // Side safe client event handler
        Mod.EventBusSubscriber.Bus.FORGE.bus().get().register(ClientEventHandler.class);
        FarsightClientChunkManager.unloadCallback.add((level, levelChunk) -> {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.ChunkEvent.Unload(levelChunk));
        });

        FarsightClientChunkManager.loadCallback.add((level, levelChunk) -> {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.ChunkEvent.Load(levelChunk, false));
        });

        if ((FMLLoader.getLoadingModList().getModFileById("embeddium") != null) || (FMLLoader.getLoadingModList().getModFileById("rubidium") != null) || (
          FMLLoader.getLoadingModList().getModFileById("sodium") != null))
        {
            SodiumCompat.init();
        }
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        LOGGER.info(MODID + " mod initialized");
    }
}
